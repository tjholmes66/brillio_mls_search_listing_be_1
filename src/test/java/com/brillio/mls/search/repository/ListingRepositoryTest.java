package com.brillio.mls.search.repository;

import java.time.LocalDate;
import java.util.List;

import com.brillio.mls.search.model.Listing;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListingRepositoryTest
{
    private final ListingRepository repository = new ListingRepository();

    @Test
    void loadsAllListingsFromClasspathJson()
    {
        List<Listing> listings = repository.findAll();

        assertThat(listings).hasSize(12);
        assertThat(listings).allSatisfy(listing -> {
            assertThat(listing.id()).isNotBlank();
            assertThat(listing.address()).isNotBlank();
            assertThat(listing.city()).isNotBlank();
            assertThat(listing.price()).isPositive();
            assertThat(listing.bedrooms()).isNotNegative();
            assertThat(listing.listedDate()).isNotNull();
            assertThat(listing.description()).isNotBlank();
        });
    }

    @Test
    void parsesDatesAndNumbersCorrectly()
    {
        Listing first = repository.findAll().getFirst();

        assertThat(first.id()).isEqualTo("A1");
        assertThat(first.price()).isEqualTo(450000L);
        assertThat(first.bedrooms()).isEqualTo(2);
        assertThat(first.bathrooms()).isEqualTo(1.5);
        assertThat(first.listedDate()).isEqualTo(LocalDate.of(2026, 8, 29));
    }

    @Test
    void findsDistinctCitiesSortedCaseInsensitively()
    {
        assertThat(repository.findDistinctCities())
                .containsExactly("Chantilly", "Fairfax", "Manassas", "Reston", "Springfield", "Vienna");
    }

    @Test
    void newestListedDateIsDatasetMaximum()
    {
        assertThat(repository.newestListedDate()).isEqualTo(LocalDate.of(2026, 9, 4));
    }
}
