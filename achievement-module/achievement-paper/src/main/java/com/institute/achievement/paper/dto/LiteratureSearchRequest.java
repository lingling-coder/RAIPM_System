package com.institute.achievement.paper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for intelligent literature matching search.
 * User provides paper title (required) and optional authors
 * to search CrossRef for candidate publications.
 */
@Data
public class LiteratureSearchRequest {

    /** Paper title (required, primary search query) */
    @NotBlank(message = "论文标题不能为空")
    private String title;

    /** Optional author names to refine the search */
    private String authors;
}
