package com.gplanet.commerce_api.dtos.api;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Generic paginated response DTO that wraps any type of content with pagination information.
 *
 * @param <T> The type of content being paginated
 * 
 * @author Gustavo
 * @version 1.0
 */
public record PaginatedResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isLastPage
) {
    /**
     * Creates a PaginatedResponse from a Spring Page object.
     *
     * @param <T> The type of content being paginated
     * @param page The Spring Page object to convert
     * @return A new PaginatedResponse containing the page information
     */
    public static <T> PaginatedResponse<T> fromPage(Page<T> page) {
        return new PaginatedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}