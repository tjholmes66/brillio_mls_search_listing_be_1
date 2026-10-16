package com.brillio.mls.search.dto;

import java.util.List;

public record SearchResponse(
        List<SearchResultItem> results,
        int page,
        int pageSize,
        long totalResults,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious)
{
    public static SearchResponse of(List<SearchResultItem> results, int page, int pageSize, long totalResults)
    {
        int totalPages = totalResults == 0 ? 0 : (int) Math.ceil((double) totalResults / pageSize);
        return new SearchResponse(results, page, pageSize, totalResults, totalPages,
                page < totalPages, page > 1 && totalPages > 0);
    }
}
