package com.brillio.mls.search.service;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResponse;

import java.util.List;

public interface ListSearchService
{
    SearchResponse search(SearchCriteria criteria);

    List<String> availableCities();
}
