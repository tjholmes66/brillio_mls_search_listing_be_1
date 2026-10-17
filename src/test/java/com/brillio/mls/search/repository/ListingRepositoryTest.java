package com.brillio.mls.search.repository;

import java.time.LocalDate;
import java.util.List;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResultItem;
import com.brillio.mls.search.repository.ListingRepository.ScanResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingRepositoryTest
{
    private final ListingRepository repository = new ListingRepository();

    private static SearchCriteria criteria(Long minPrice, Long maxPrice, Integer minBedrooms,
                                           String city, String keyword)
    {
        return SearchCriteria.of(minPrice, maxPrice, minBedrooms, city, keyword, null, null, null);
    }

    @Test
    void scanReturnsAllListingsWhenNoFiltersApplied()
    {
        ScanResult scan = repository.scan(criteria(null, null, null, null, null));

        assertThat(scan.matches()).hasSize(12);
        assertThat(scan.matches()).allSatisfy(item -> {
            assertThat(item.id()).isNotBlank();
            assertThat(item.address()).isNotBlank();
            assertThat(item.city()).isNotBlank();
            assertThat(item.price()).isPositive();
            assertThat(item.listedDate()).isNotNull();
        });
    }

    @Test
    void scanParsesDatesAndNumbersCorrectly()
    {
        SearchResultItem first = repository.scan(criteria(null, null, null, null, null))
                .matches().getFirst();

        assertThat(first.id()).isEqualTo("A1");
        assertThat(first.price()).isEqualTo(450000L);
        assertThat(first.bedrooms()).isEqualTo(2);
        assertThat(first.bathrooms()).isEqualTo(1.5);
        assertThat(first.listedDate()).isEqualTo(LocalDate.of(2026, 8, 29));
    }

    @Test
    void scanFiltersWhileStreamingRatherThanAfterLoading()
    {
        ScanResult scan = repository.scan(criteria(400000L, 550000L, 3, "Springfield", null));

        assertThat(scan.matches())
                .extracting(SearchResultItem::id)
                .containsExactlyInAnyOrder("A2", "B8");
    }

    @Test
    void scanTracksNewestListedDateAcrossTheWholeFile()
    {
        ScanResult scan = repository.scan(criteria(null, null, null, "Fairfax", null));

        assertThat(scan.matches()).allMatch(item -> item.city().equalsIgnoreCase("Fairfax"));
        assertThat(scan.newestListedDate()).isEqualTo(LocalDate.of(2026, 9, 4));
    }

    @Test
    void scanCollectsDistinctCitiesAcrossTheWholeFile()
    {
        ScanResult scan = repository.scan(criteria(null, null, null, "Fairfax", null));

        assertThat(scan.distinctCities())
                .containsExactly("Chantilly", "Fairfax", "Manassas", "Reston", "Springfield", "Vienna");
    }

    @Test
    void scanReturnsEmptyMatchesForUnmatchedFilters()
    {
        ScanResult scan = repository.scan(criteria(null, null, null, null, "no-such-property-xyz"));

        assertThat(scan.matches()).isEmpty();
        assertThat(scan.newestListedDate()).isEqualTo(LocalDate.of(2026, 9, 4));
    }

    @Test
    void findDistinctCitiesStreamsSortedCaseInsensitively()
    {
        assertThat(repository.findDistinctCities())
                .containsExactly("Chantilly", "Fairfax", "Manassas", "Reston", "Springfield", "Vienna");
    }

    @Test
    void throwsWhenResourceIsMissing()
    {
        ListingRepository missing = new ListingRepository("json/does-not-exist.json");

        assertThatThrownBy(() -> missing.scan(criteria(null, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to read listing resource");
    }
}
