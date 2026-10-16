package com.brillio.mls.search.controller;

import java.time.LocalDate;
import java.util.List;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResponse;
import com.brillio.mls.search.dto.SearchResultItem;
import com.brillio.mls.search.exception.InvalidSearchCriteriaException;
import com.brillio.mls.search.exception.ListingNotFoundException;
import com.brillio.mls.search.service.ListingSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListingSearchController.class)
class ListingSearchControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingSearchService service;

    private static SearchResponse sampleResponse()
    {
        SearchResultItem item = new SearchResultItem("A1", "MLS_A", "123 Main St, Apt 4B",
                "Springfield", "VA", "22150", 450000L, 2, 1.5, 980,
                LocalDate.of(2026, 8, 29), "active", "Bright top-floor condo near shops and transit.",
                87.5);
        return new SearchResponse(List.of(item), 1, 10, 1, 1, false, false);
    }

    @Test
    void searchReturnsResultsWithPaginationMetadata() throws Exception
    {
        when(service.search(any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/listings/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].id").value("A1"))
                .andExpect(jsonPath("$.results[0].address").value("123 Main St, Apt 4B"))
                .andExpect(jsonPath("$.results[0].price").value(450000))
                .andExpect(jsonPath("$.results[0].bedrooms").value(2))
                .andExpect(jsonPath("$.results[0].relevanceScore").value(87.5))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void searchPassesAllQueryParametersToService() throws Exception
    {
        when(service.search(any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/listings/search")
                        .param("minPrice", "100000")
                        .param("maxPrice", "900000")
                        .param("minBedrooms", "2")
                        .param("city", "Springfield")
                        .param("keyword", "condo")
                        .param("targetBudget", "500000")
                        .param("page", "2")
                        .param("pageSize", "25"))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(service).search(captor.capture());
        SearchCriteria captured = captor.getValue();
        assertThat(captured.minPrice()).isEqualTo(100000L);
        assertThat(captured.maxPrice()).isEqualTo(900000L);
        assertThat(captured.minBedrooms()).isEqualTo(2);
        assertThat(captured.city()).isEqualTo("Springfield");
        assertThat(captured.keyword()).isEqualTo("condo");
        assertThat(captured.targetBudget()).isEqualTo(500000L);
        assertThat(captured.page()).isEqualTo(2);
        assertThat(captured.pageSize()).isEqualTo(25);
    }

    @Test
    void searchAppliesDefaultsWhenParamsAbsent() throws Exception
    {
        when(service.search(any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/listings/search"))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(service).search(captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().pageSize()).isEqualTo(10);
    }

    @Test
    void searchRejectsMinPriceGreaterThanMaxPrice() throws Exception
    {
        mockMvc.perform(get("/api/listings/search")
                        .param("minPrice", "600000")
                        .param("maxPrice", "500000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "minPrice (600000) must not be greater than maxPrice (500000)"))
                .andExpect(jsonPath("$.path").value("/api/listings/search"))
                .andExpect(jsonPath("$.details[0]").value(
                        "minPrice (600000) must not be greater than maxPrice (500000)"));

        verifyNoInteractions(service);
    }

    @Test
    void searchRejectsPageSizeOfZero() throws Exception
    {
        mockMvc.perform(get("/api/listings/search").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pageSize must be 1 or greater, got 0"));

        verifyNoInteractions(service);
    }

    @Test
    void searchRejectsNegativePage() throws Exception
    {
        mockMvc.perform(get("/api/listings/search").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page must be 1 or greater, got -1"));

        verifyNoInteractions(service);
    }

    @Test
    void searchRejectsNegativeMinPrice() throws Exception
    {
        mockMvc.perform(get("/api/listings/search").param("minPrice", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("minPrice must be zero or greater, got -1"));

        verifyNoInteractions(service);
    }

    @Test
    void searchRejectsZeroTargetBudget() throws Exception
    {
        mockMvc.perform(get("/api/listings/search").param("targetBudget", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("targetBudget must be greater than zero, got 0"));

        verifyNoInteractions(service);
    }

    @Test
    void searchRejectsNonNumericNumericParam() throws Exception
    {
        mockMvc.perform(get("/api/listings/search").param("minPrice", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'not-a-number' for parameter 'minPrice'; expected type Long"));

        verifyNoInteractions(service);
    }

    @Test
    void searchRejectsOverlongKeyword() throws Exception
    {
        mockMvc.perform(get("/api/listings/search").param("keyword", "x".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("keyword must not exceed 200 characters, got 201"));

        verifyNoInteractions(service);
    }

    @Test
    void searchReturnsNotFoundForUnknownCity() throws Exception
    {
        when(service.search(any())).thenThrow(new ListingNotFoundException(
                "No listings found for city 'Atlantis'", List.of("Springfield", "Fairfax")));

        mockMvc.perform(get("/api/listings/search").param("city", "Atlantis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No listings found for city 'Atlantis'"))
                .andExpect(jsonPath("$.details[0]").value("Springfield"))
                .andExpect(jsonPath("$.details[1]").value("Fairfax"));
    }

    @Test
    void searchReturnsNotFoundForOutOfRangePage() throws Exception
    {
        when(service.search(any())).thenThrow(new ListingNotFoundException(
                "Requested page 9 is out of range; the search returned 13 listings across 3 page(s)",
                List.of("1 <= page <= 3")));

        mockMvc.perform(get("/api/listings/search").param("page", "9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Requested page 9 is out of range; the search returned 13 listings across 3 page(s)"))
                .andExpect(jsonPath("$.details[0]").value("1 <= page <= 3"));
    }

    @Test
    void searchReturnsBadRequestWhenServiceRejectsCriteria() throws Exception
    {
        when(service.search(any())).thenThrow(new InvalidSearchCriteriaException(
                List.of("pageSize must not exceed 100, got 500")));

        mockMvc.perform(get("/api/listings/search").param("pageSize", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pageSize must not exceed 100, got 500"));
    }

    @Test
    void citiesEndpointReturnsDistinctCities() throws Exception
    {
        when(service.availableCities()).thenReturn(List.of("Fairfax", "Reston", "Springfield"));

        mockMvc.perform(get("/api/listings/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Fairfax"))
                .andExpect(jsonPath("$[1]").value("Reston"))
                .andExpect(jsonPath("$[2]").value("Springfield"));
    }

    @Test
    void searchResponseIsServedAsJson() throws Exception
    {
        when(service.search(any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/listings/search").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
