package com.brillio.mls.search.service;

import java.util.List;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResponse;
import com.brillio.mls.search.dto.SearchResultItem;
import com.brillio.mls.search.exception.ListingNotFoundException;
import com.brillio.mls.search.repository.ListingRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingSearchServiceTest
{
    private final ListingRepository repository = new ListingRepository();
    private final ListingSearchService service = new ListingSearchService(repository, new RelevanceScorer());

    private static SearchCriteria criteria(Long minPrice, Long maxPrice, Integer minBedrooms,
                                           String city, String keyword, Long targetBudget,
                                           Integer page, Integer pageSize)
    {
        return SearchCriteria.of(minPrice, maxPrice, minBedrooms, city, keyword, targetBudget, page, pageSize);
    }

    @Test
    void returnsAllListingsWhenNoFiltersApplied()
    {
        SearchResponse response = service.search(criteria(null, null, null, null, null, null, null, null));

        assertThat(response.totalResults()).isEqualTo(12);
        assertThat(response.results()).hasSize(10);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void filtersByPriceRange()
    {
        SearchResponse response = service.search(criteria(500000L, 600000L, null, null, null, null, null, null));

        assertThat(response.totalResults()).isEqualTo(4);
        assertThat(response.results())
                .extracting(SearchResultItem::price)
                .allMatch(price -> price >= 500000 && price <= 600000);
    }

    @Test
    void filtersByMinBedrooms()
    {
        SearchResponse response = service.search(criteria(null, null, 4, null, null, null, null, null));

        assertThat(response.totalResults()).isEqualTo(2);
        assertThat(response.results())
                .extracting(SearchResultItem::bedrooms)
                .allMatch(bedrooms -> bedrooms >= 4);
    }

    @Test
    void filtersByCityCaseInsensitively()
    {
        SearchResponse response = service.search(criteria(null, null, null, "springfield", null, null, null, null));

        assertThat(response.totalResults()).isEqualTo(4);
        assertThat(response.results())
                .extracting(SearchResultItem::city)
                .allMatch(city -> city.equalsIgnoreCase("Springfield"));
    }

    @Test
    void filtersByKeywordInDescriptionCaseInsensitively()
    {
        SearchResponse response = service.search(criteria(null, null, null, null, "PETS", null, null, null));

        assertThat(response.totalResults()).isEqualTo(7);
        assertThat(response.results())
                .extracting(SearchResultItem::description)
                .allMatch(description -> description.toLowerCase().contains("pets"));
    }

    @Test
    void combinesMultipleFilters()
    {
        SearchResponse response = service.search(criteria(400000L, 550000L, 3, "Springfield", "kitchen", null, null, null));

        assertThat(response.totalResults()).isEqualTo(2);
        assertThat(response.results())
                .extracting(SearchResultItem::id)
                .containsExactlyInAnyOrder("A2", "B8");
    }

    @Test
    void ranksByRecencyWhenNoTargetBudgetGiven()
    {
        SearchResponse response = service.search(criteria(null, null, null, null, null, null, null, null));

        assertThat(response.results().getFirst().id()).isEqualTo("A5");
        assertThat(response.results().getFirst().relevanceScore()).isEqualTo(100.0);
        assertThat(response.results())
                .extracting(SearchResultItem::relevanceScore)
                .isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void ranksByBudgetFitAndRecencyWhenTargetBudgetGiven()
    {
        SearchResponse response = service.search(criteria(null, null, null, null, null, 450000L, null, null));

        List<SearchResultItem> results = response.results();

        assertThat(results.getFirst().id()).isEqualTo("A5");
        assertThat(results.getFirst().relevanceScore()).isEqualTo(97.3);
        assertThat(results)
                .extracting(SearchResultItem::relevanceScore)
                .isSortedAccordingTo((a, b) -> Double.compare(b, a));
        assertThat(results)
                .extracting(SearchResultItem::relevanceScore)
                .allMatch(score -> score >= 0.0 && score <= 100.0);
    }

    @Test
    void exactBudgetMatchOutranksDistantPrice()
    {
        SearchResponse response = service.search(criteria(null, null, null, "Fairfax", null, 399000L, null, null));

        assertThat(response.results().getFirst().id()).isEqualTo("A3");
    }

    @Test
    void paginatesThroughResults()
    {
        SearchResponse page1 = service.search(criteria(null, null, null, null, null, null, 1, 5));
        SearchResponse page2 = service.search(criteria(null, null, null, null, null, null, 2, 5));
        SearchResponse page3 = service.search(criteria(null, null, null, null, null, null, 3, 5));

        assertThat(page1.results()).hasSize(5);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.hasPrevious()).isFalse();

        assertThat(page2.results()).hasSize(5);
        assertThat(page2.hasNext()).isTrue();
        assertThat(page2.hasPrevious()).isTrue();

        assertThat(page3.results()).hasSize(2);
        assertThat(page3.hasNext()).isFalse();
        assertThat(page3.hasPrevious()).isTrue();

        List<String> allIds = page1.results().stream()
                .map(SearchResultItem::id)
                .toList();
        assertThat(allIds).doesNotContainAnyElementsOf(
                page2.results().stream().map(SearchResultItem::id).toList());
    }

    @Test
    void returnsEmptyPageForValidSearchWithNoMatches()
    {
        SearchResponse response = service.search(criteria(null, null, null, null, "no-such-property-xyz", null, null, null));

        assertThat(response.totalResults()).isZero();
        assertThat(response.results()).isEmpty();
        assertThat(response.totalPages()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void throwsForUnknownCityListingAvailableOptions()
    {
        assertThatThrownBy(() -> service.search(criteria(null, null, null, "Atlantis", null, null, null, null)))
                .isInstanceOf(ListingNotFoundException.class)
                .hasMessageContaining("No listings found for city 'Atlantis'")
                .satisfies(ex -> assertThat(((ListingNotFoundException) ex).getAvailableOptions())
                        .contains("Springfield", "Fairfax", "Reston", "Vienna", "Manassas", "Chantilly"));
    }

    @Test
    void throwsForPageBeyondLastPage()
    {
        assertThatThrownBy(() -> service.search(criteria(null, null, null, null, null, null, 4, 5)))
                .isInstanceOf(ListingNotFoundException.class)
                .hasMessageContaining("Requested page 4 is out of range")
                .hasMessageContaining("12 listings across 3 page(s)");
    }

    @Test
    void throwsForSecondPageWhenSearchHasNoMatches()
    {
        assertThatThrownBy(() -> service.search(criteria(null, null, null, null, "no-such-property-xyz", null, 2, 5)))
                .isInstanceOf(ListingNotFoundException.class)
                .hasMessageContaining("Requested page 2 is out of range");
    }

    @Test
    void exposesDistinctCitiesForClientDropdowns()
    {
        assertThat(service.availableCities())
                .containsExactly("Chantilly", "Fairfax", "Manassas", "Reston", "Springfield", "Vienna");
    }
}
