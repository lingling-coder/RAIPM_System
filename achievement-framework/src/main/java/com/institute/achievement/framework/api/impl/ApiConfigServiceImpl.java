package com.institute.achievement.framework.api.impl;

import com.institute.achievement.common.exception.BadRequestException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.framework.api.ApiConfigDTO;
import com.institute.achievement.framework.api.ApiConfigEntity;
import com.institute.achievement.framework.api.ApiConfigMapper;
import com.institute.achievement.framework.api.ApiConfigService;
import com.institute.achievement.framework.api.TestConnectionResultVO;
import com.institute.achievement.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the API configuration service.
 * <p>
 * Provides CRUD operations with Redis caching via @Cacheable,
 * automatic cache invalidation on updates, connection testing
 * with per-config timeouts/auth headers, and runtime config lookup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiConfigServiceImpl extends ServiceImpl<ApiConfigMapper, ApiConfigEntity>
        implements ApiConfigService {

    private final ApiConfigMapper apiConfigMapper;

    // ── CRUD Operations ──────────────────────────────────────────────────

    @Override
    public PageResult<ApiConfigDTO> page(PageQuery query) {
        Page<ApiConfigEntity> mpPage = new Page<>(query.getPage(), query.getPageSize());
        Page<ApiConfigEntity> result = apiConfigMapper.selectPage(mpPage, null);

        List<ApiConfigDTO> dtoList = result.getRecords().stream()
                .map(this::toDTO)
                .peek(ApiConfigDTO::maskSensitiveData)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public ApiConfigDTO getById(Long id) {
        ApiConfigEntity entity = apiConfigMapper.selectById(id);
        if (entity == null) {
            throw new EntityNotFoundException("API配置不存在: " + id);
        }
        ApiConfigDTO dto = toDTO(entity);
        dto.maskSensitiveData();
        return dto;
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiConfig", allEntries = true)
    public void create(ApiConfigDTO dto) {
        // Check unique config code
        ApiConfigEntity existing = apiConfigMapper.selectByCode(dto.getConfigCode());
        if (existing != null) {
            throw new BadRequestException("配置编码已存在: " + dto.getConfigCode());
        }

        ApiConfigEntity entity = toEntity(dto);
        entity.setCreatedBy(SecurityUtils.getCurrentUsername());
        entity.setUpdatedBy(SecurityUtils.getCurrentUsername());
        apiConfigMapper.insert(entity);
        log.info("API configuration created: {} ({})", entity.getConfigName(), entity.getConfigCode());
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiConfig", allEntries = true)
    public void update(Long id, ApiConfigDTO dto) {
        ApiConfigEntity entity = apiConfigMapper.selectById(id);
        if (entity == null) {
            throw new EntityNotFoundException("API配置不存在: " + id);
        }

        // Check configCode uniqueness if changed
        if (dto.getConfigCode() != null && !dto.getConfigCode().equals(entity.getConfigCode())) {
            ApiConfigEntity existing = apiConfigMapper.selectByCode(dto.getConfigCode());
            if (existing != null && !existing.getId().equals(id)) {
                throw new BadRequestException("配置编码已存在: " + dto.getConfigCode());
            }
        }

        updateEntity(entity, dto);
        entity.setUpdatedBy(SecurityUtils.getCurrentUsername());
        apiConfigMapper.updateById(entity);
        log.info("API configuration updated: {} ({})", entity.getConfigName(), entity.getConfigCode());
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiConfig", allEntries = true)
    public void delete(Long id) {
        ApiConfigEntity entity = apiConfigMapper.selectById(id);
        if (entity == null) {
            throw new EntityNotFoundException("API配置不存在: " + id);
        }
        apiConfigMapper.deleteById(id);
        log.info("API configuration deleted: {} ({})", entity.getConfigName(), entity.getConfigCode());
    }

    // ── Connection Testing ───────────────────────────────────────────────

    @Override
    @Transactional
    public TestConnectionResultVO testConnection(Long id) {
        ApiConfigEntity config = apiConfigMapper.selectById(id);
        if (config == null) {
            throw new EntityNotFoundException("API配置不存在: " + id);
        }

        TestConnectionResultVO result;
        long startTime = System.currentTimeMillis();

        try {
            // Create RestTemplate with per-config timeouts
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(config.getConnectTimeout() * 1000);
            factory.setReadTimeout(config.getReadTimeout() * 1000);
            // Note: setBufferRequestBody not available in this Spring version;
            // using defaults which are fine for test-connection requests.
            RestTemplate restTemplate = new RestTemplate(factory);

            // Build headers based on auth type
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Achievement-System/1.0");
            applyAuthHeaders(headers, config);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // Send GET request to endpoint
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getEndpointUrl(),
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            long elapsed = System.currentTimeMillis() - startTime;

            result = TestConnectionResultVO.builder()
                    .success(true)
                    .message("连接成功")
                    .responseTimeMs(elapsed)
                    .statusCode(response.getStatusCode().value())
                    .build();

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("API connection test failed for {} ({}): {}", config.getConfigName(),
                    config.getEndpointUrl(), e.getMessage());

            result = TestConnectionResultVO.builder()
                    .success(false)
                    .message("连接失败: " + e.getMessage())
                    .responseTimeMs(elapsed)
                    .statusCode(null)
                    .build();
        }

        // Update last test time and result
        config.setLastTestTime(LocalDateTime.now());
        config.setLastTestResult(result.getSuccess() ? 1 : 0);
        apiConfigMapper.updateById(config);

        return result;
    }

    // ── Runtime Config Lookup ────────────────────────────────────────────

    @Override
    @Cacheable(value = "apiConfig", key = "#code", unless = "#result == null")
    public ApiConfigEntity getActiveByCode(String code) {
        ApiConfigEntity entity = apiConfigMapper.selectByCode(code);
        if (entity != null && entity.getStatus() == 1) {
            return entity;
        }
        return null;
    }

    @Override
    @CacheEvict(value = "apiConfig", allEntries = true)
    public void refreshCache() {
        log.info("API configuration cache cleared");
    }

    // ── Helper Methods ───────────────────────────────────────────────────

    /**
     * Apply authentication headers to the HTTP request based on auth type.
     */
    private void applyAuthHeaders(HttpHeaders headers, ApiConfigEntity config) {
        String authType = config.getAuthType() != null ? config.getAuthType() : "NONE";

        switch (authType.toUpperCase()) {
            case "API_KEY":
                if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                    headers.set("X-API-Key", config.getApiKey());
                }
                break;

            case "BEARER":
                if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                    headers.setBearerAuth(config.getApiKey());
                }
                break;

            case "BASIC":
                if (config.getApiKey() != null && config.getSecretKey() != null) {
                    String credentials = config.getApiKey() + ":" + config.getSecretKey();
                    String encoded = Base64.getEncoder().encodeToString(
                            credentials.getBytes(StandardCharsets.UTF_8));
                    headers.set("Authorization", "Basic " + encoded);
                }
                break;

            case "NONE":
            default:
                // No authentication headers
                break;
        }
    }

    /**
     * Convert entity to DTO.
     */
    private ApiConfigDTO toDTO(ApiConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        ApiConfigDTO dto = new ApiConfigDTO();
        dto.setId(entity.getId());
        dto.setConfigName(entity.getConfigName());
        dto.setConfigCode(entity.getConfigCode());
        dto.setEndpointUrl(entity.getEndpointUrl());
        dto.setDescription(entity.getDescription());
        dto.setAuthType(entity.getAuthType());
        dto.setApiKey(entity.getApiKey());
        dto.setSecretKey(entity.getSecretKey());
        dto.setTokenUrl(entity.getTokenUrl());
        dto.setConnectTimeout(entity.getConnectTimeout());
        dto.setReadTimeout(entity.getReadTimeout());
        dto.setRetryCount(entity.getRetryCount());
        dto.setRetryInterval(entity.getRetryInterval());
        dto.setBackoffStrategy(entity.getBackoffStrategy());
        dto.setFailureAlert(entity.getFailureAlert());
        dto.setStatus(entity.getStatus());
        dto.setLastTestTime(entity.getLastTestTime());
        dto.setLastTestResult(entity.getLastTestResult());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }

    /**
     * Convert DTO to entity (for creation).
     */
    private ApiConfigEntity toEntity(ApiConfigDTO dto) {
        ApiConfigEntity entity = new ApiConfigEntity();
        updateEntity(entity, dto);
        return entity;
    }

    /**
     * Update entity fields from DTO (for update).
     */
    private void updateEntity(ApiConfigEntity entity, ApiConfigDTO dto) {
        if (dto.getConfigName() != null) entity.setConfigName(dto.getConfigName());
        if (dto.getConfigCode() != null) entity.setConfigCode(dto.getConfigCode());
        if (dto.getEndpointUrl() != null) entity.setEndpointUrl(dto.getEndpointUrl());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getAuthType() != null) entity.setAuthType(dto.getAuthType());
        if (dto.getApiKey() != null) entity.setApiKey(dto.getApiKey());
        if (dto.getSecretKey() != null) entity.setSecretKey(dto.getSecretKey());
        if (dto.getTokenUrl() != null) entity.setTokenUrl(dto.getTokenUrl());
        if (dto.getConnectTimeout() != null) entity.setConnectTimeout(dto.getConnectTimeout());
        if (dto.getReadTimeout() != null) entity.setReadTimeout(dto.getReadTimeout());
        if (dto.getRetryCount() != null) entity.setRetryCount(dto.getRetryCount());
        if (dto.getRetryInterval() != null) entity.setRetryInterval(dto.getRetryInterval());
        if (dto.getBackoffStrategy() != null) entity.setBackoffStrategy(dto.getBackoffStrategy());
        if (dto.getFailureAlert() != null) entity.setFailureAlert(dto.getFailureAlert());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
    }
}
