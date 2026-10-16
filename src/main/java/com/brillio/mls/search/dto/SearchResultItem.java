package com.brillio.mls.search.dto;

import java.time.LocalDate;

public record SearchResultItem(
        String id,
        String source,
        String address,
        String city,
        String state,
        String zip,
        long price,
        int bedrooms,
        double bathrooms,
        int sqft,
        LocalDate listedDate,
        String status,
        String description,
        double relevanceScore)
{
}
