package com.institute.achievement.framework.audit.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.framework.audit.AuditLogDTO;
import com.institute.achievement.framework.audit.AuditLogEntity;
import com.institute.achievement.framework.audit.AuditLogMapper;
import com.institute.achievement.framework.audit.AuditLogPageDTO;
import com.institute.achievement.framework.audit.AuditLogService;
import com.institute.achievement.framework.audit.AuditLogVO;
import com.institute.achievement.framework.audit.ChainVerificationResult;
import com.institute.achievement.framework.audit.HashChainUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the audit log service.
 * <p>
 * Enforces INSERT-only semantics -- no update or delete operations are
 * exposed. Each new entry is linked to the previous entry via SHA-256
 * hash chain (D-27). The record() method uses REQUIRES_NEW propagation
 * to ensure audit logging succeeds even if the parent transaction rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLogDTO dto) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setOperatorId(dto.getOperatorId());
            entity.setOperatorName(dto.getOperatorName());
            entity.setOperationType(dto.getOperationType());
            entity.setOperationName(dto.getOperationName());
            entity.setTargetType(dto.getTargetType());
            entity.setTargetId(dto.getTargetId());
            entity.setIpAddress(dto.getIpAddress());
            entity.setUserAgent(dto.getUserAgent());
            entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

            // Serialize original and target content to JSON
            try {
                if (dto.getOriginalContent() != null) {
                    entity.setOriginalContent(objectMapper.writeValueAsString(dto.getOriginalContent()));
                }
                if (dto.getTargetContent() != null) {
                    entity.setTargetContent(objectMapper.writeValueAsString(dto.getTargetContent()));
                }
            } catch (Exception e) {
                log.warn("Failed to serialize audit log content: {}", e.getMessage());
                if (dto.getOriginalContent() != null) {
                    entity.setOriginalContent(dto.getOriginalContent().toString());
                }
                if (dto.getTargetContent() != null) {
                    entity.setTargetContent(dto.getTargetContent().toString());
                }
            }

            // Get previous hash from last log entry
            AuditLogEntity lastLog = auditLogMapper.selectLastLog();
            String previousHash = (lastLog != null) ? lastLog.getCurrentHash() : "";
            entity.setPreviousHash(previousHash);

            // Set created_at before computing hash
            entity.setCreatedAt(LocalDateTime.now());

            // Compute current hash
            String currentHash = HashChainUtil.computeHash(
                    null, // id will be assigned by DB, use null or 0
                    previousHash,
                    entity.getTargetContent(),
                    entity.getCreatedAt()
            );
            entity.setCurrentHash(currentHash);

            // Insert the record (INSERT-only)
            auditLogMapper.insert(entity);

            // Update the hash with the actual generated ID by recomputing
            if (entity.getId() != null) {
                String computedHash = HashChainUtil.computeHash(
                        entity.getId(),
                        previousHash,
                        entity.getTargetContent(),
                        entity.getCreatedAt()
                );
                entity.setCurrentHash(computedHash);
                auditLogMapper.updateById(entity);
                log.debug("Audit log recorded: type={}, operator={}, id={}",
                        dto.getOperationType(), dto.getOperatorName(), entity.getId());
            }

        } catch (Exception e) {
            // Audit logging should never break the main operation
            log.error("Failed to record audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    public PageResult<AuditLogVO> page(AuditLogPageDTO dto) {
        // Build pagination
        Page<AuditLogEntity> page = new Page<>(dto.getPage(), dto.getPageSize());

        // Execute filtered query
        Page<AuditLogEntity> result = auditLogMapper.selectPageWithFilters(
                page,
                dto.getOperatorName(),
                dto.getOperationType(),
                dto.getTargetType(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        // Convert to VOs
        List<AuditLogVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), dto.getPage(), dto.getPageSize());
    }

    @Override
    public AuditLogVO getDetail(Long id) {
        AuditLogEntity entity = auditLogMapper.selectById(id);
        if (entity == null) {
            return null;
        }

        AuditLogVO vo = toVO(entity);

        // Compute expected hash and verify integrity
        String expectedHash = HashChainUtil.computeHash(
                entity.getId(),
                entity.getPreviousHash(),
                entity.getTargetContent(),
                entity.getCreatedAt()
        );
        vo.setIntegrityVerified(expectedHash.equals(entity.getCurrentHash()));

        return vo;
    }

    @Override
    public ChainVerificationResult verifyChain(Long fromId, Long toId) {
        List<AuditLogEntity> segment = auditLogMapper.selectChainSegment(fromId, toId);
        return HashChainUtil.verifyChainDetailed(segment);
    }

    @Override
    public Map<String, Long> getOperationTypeStats(LocalDateTime start, LocalDateTime end) {
        List<AuditLogMapper.OperationTypeCount> counts =
                auditLogMapper.countByOperationType(start, end);

        Map<String, Long> stats = new HashMap<>();
        for (AuditLogMapper.OperationTypeCount count : counts) {
            stats.put(count.getOperationType(), count.getCnt());
        }
        return stats;
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private AuditLogVO toVO(AuditLogEntity entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(entity.getId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setOperationType(entity.getOperationType());
        vo.setOperationName(entity.getOperationName());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setIpAddress(entity.getIpAddress());
        vo.setUserAgent(entity.getUserAgent());
        vo.setStatus(entity.getStatus());
        vo.setOriginalContent(entity.getOriginalContent());
        vo.setTargetContent(entity.getTargetContent());
        vo.setPreviousHash(entity.getPreviousHash());
        vo.setCurrentHash(entity.getCurrentHash());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
