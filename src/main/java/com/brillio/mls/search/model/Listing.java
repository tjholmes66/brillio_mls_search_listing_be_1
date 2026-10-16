package com.brillio.mls.search.model;

import java.time.LocalDate;

public record Listing(
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
        double latitude,
        double longitude,
        LocalDate listedDate,
        String status,
        String description)
{
}
