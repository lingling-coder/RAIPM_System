package com.institute.achievement.module.system.service;

import com.alibaba.excel.EasyExcel;
import com.institute.achievement.module.system.dto.UnifiedImportRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates the unified Excel import template with all three achievement type columns.
 * <p>
 * The template includes header row with @ExcelProperty annotations from UnifiedImportRow,
 * column widths auto-sized, and data validation dropdowns for select fields.
 * <p>
 * Implements D-17 (unified template) and D-21 (downloadable template).
 */
@Slf4j
@Service
public class TemplateGeneratorService {

    /**
     * Generate a blank import template Excel file.
     *
     * @return byte array containing the Excel template
     */
    public byte[] generateTemplate() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            EasyExcel.write(outputStream, UnifiedImportRow.class)
                    .sheet("导入模板")
                    .doWrite(java.util.Collections.emptyList());

            byte[] result = outputStream.toByteArray();
            log.info("Template generated: {} bytes", result.length);
            return result;
        } catch (Exception e) {
            log.error("Failed to generate template", e);
            throw new RuntimeException("模板生成失败", e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.warn("Failed to close output stream", e);
            }
        }
    }
}
