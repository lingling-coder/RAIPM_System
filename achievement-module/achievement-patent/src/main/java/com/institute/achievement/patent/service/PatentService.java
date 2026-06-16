package com.institute.achievement.patent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.patent.dto.PatentDTO;
import com.institute.achievement.patent.dto.PatentVO;
import com.institute.achievement.patent.entity.Patent;
import com.institute.achievement.patent.mapper.PatentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core patent achievement service.
 * <p>
 * Stub implementation for TDD RED phase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatentService {

    private final PatentMapper patentMapper;

    @Transactional
    public Long createPatent(PatentDTO dto) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Transactional
    public void updatePatent(Long id, PatentDTO dto) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Transactional
    public void submitPatent(Long id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Transactional
    public Long saveDraft(PatentDTO dto) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Patent getPatentById(Long id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Page<PatentVO> pagePatents(int page, int size, String status, String keyword) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
