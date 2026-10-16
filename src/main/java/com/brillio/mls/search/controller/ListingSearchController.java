package com.brillio.mls.search.controller;

import java.util.List;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResponse;
import com.brillio.mls.search.service.ListingSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
public class ListingSearchController
{
    private final ListingSearchService service;

    public ListingSearchController(ListingSearchService service)
    {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long targetBudget,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize)
    {
        SearchCriteria criteria = SearchCriteria.of(
                minPrice, maxPrice, minBedrooms, city, keyword, targetBudget, page, pageSize);
        return ResponseEntity.ok(service.search(criteria));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> cities()
    {
        return ResponseEntity.ok(service.availableCities());
    }
}
