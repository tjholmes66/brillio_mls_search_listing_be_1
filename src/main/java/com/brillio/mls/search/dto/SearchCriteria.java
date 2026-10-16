package com.brillio.mls.search.dto;

import java.util.ArrayList;
import java.util.List;

import com.brillio.mls.search.exception.InvalidSearchCriteriaException;

public record SearchCriteria(
        Long minPrice,
        Long maxPrice,
        Integer minBedrooms,
        String city,
        String keyword,
        Long targetBudget,
        int page,
        int pageSize)
{
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_KEYWORD_LENGTH = 200;

    public static SearchCriteria of(Long minPrice, Long maxPrice, Integer minBedrooms, String city,
                                    String keyword, Long targetBudget, Integer page, Integer pageSize)
    {
        List<String> errors = new ArrayList<>();

        long effectivePage = page == null ? DEFAULT_PAGE : page;
        long effectivePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;

        if (minPrice != null && minPrice < 0)
        {
            errors.add("minPrice must be zero or greater, got " + minPrice);
        }
        if (maxPrice != null && maxPrice < 0)
        {
            errors.add("maxPrice must be zero or greater, got " + maxPrice);
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice)
        {
            errors.add("minPrice (" + minPrice + ") must not be greater than maxPrice (" + maxPrice + ")");
        }
        if (minBedrooms != null && minBedrooms < 0)
        {
            errors.add("minBedrooms must be zero or greater, got " + minBedrooms);
        }
        if (targetBudget != null && targetBudget <= 0)
        {
            errors.add("targetBudget must be greater than zero, got " + targetBudget);
        }

        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.length() > MAX_KEYWORD_LENGTH)
        {
            errors.add("keyword must not exceed " + MAX_KEYWORD_LENGTH + " characters, got " + normalizedKeyword.length());
        }

        String normalizedCity = city == null || city.isBlank() ? null : city.trim();

        if (effectivePage < 1)
        {
            errors.add("page must be 1 or greater, got " + page);
        }
        if (effectivePageSize < 1)
        {
            errors.add("pageSize must be 1 or greater, got " + pageSize);
        }
        else if (effectivePageSize > MAX_PAGE_SIZE)
        {
            errors.add("pageSize must not exceed " + MAX_PAGE_SIZE + ", got " + pageSize);
        }

        if (!errors.isEmpty())
        {
            throw new InvalidSearchCriteriaException(errors);
        }

        return new SearchCriteria(minPrice, maxPrice, minBedrooms, normalizedCity, normalizedKeyword,
                targetBudget, (int) effectivePage, (int) effectivePageSize);
    }
}
