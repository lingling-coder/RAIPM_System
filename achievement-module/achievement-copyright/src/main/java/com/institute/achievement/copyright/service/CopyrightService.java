package com.institute.achievement.copyright.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.copyright.dto.CopyrightDTO;
import com.institute.achievement.copyright.dto.CopyrightVO;
import com.institute.achievement.copyright.entity.Copyright;
import com.institute.achievement.copyright.mapper.CopyrightMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core software copyright achievement service.
 * <p>
 * Stub implementation for TDD RED phase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightService {

    private final CopyrightMapper copyrightMapper;

    @Transactional
    public Long createCopyright(CopyrightDTO dto) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Transactional
    public void updateCopyright(Long id, CopyrightDTO dto) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Transactional
    public void submitCopyright(Long id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Transactional
    public Long saveDraft(CopyrightDTO dto) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Copyright getCopyrightById(Long id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Page<CopyrightVO> pageCopyrights(int page, int size, String status, String keyword) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
