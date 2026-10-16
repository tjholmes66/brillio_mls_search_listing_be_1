package com.brillio.mls.search.dto;

import com.brillio.mls.search.exception.InvalidSearchCriteriaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchCriteriaTest
{
    @Test
    void appliesDefaultsWhenOptionalParamsAbsent()
    {
        SearchCriteria criteria = SearchCriteria.of(null, null, null, null, null, null, null, null);

        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.pageSize()).isEqualTo(10);
        assertThat(criteria.city()).isNull();
        assertThat(criteria.keyword()).isNull();
    }

    @Test
    void trimsAndNormalizesBlankValuesToNull()
    {
        SearchCriteria criteria = SearchCriteria.of(null, null, null, "  Springfield  ", "   ", null, null, null);

        assertThat(criteria.city()).isEqualTo("Springfield");
        assertThat(criteria.keyword()).isNull();
    }

    @Test
    void acceptsValidFullCriteria()
    {
        SearchCriteria criteria = SearchCriteria.of(100000L, 900000L, 2, "Reston", "pool", 500000L, 2, 25);

        assertThat(criteria.minPrice()).isEqualTo(100000L);
        assertThat(criteria.maxPrice()).isEqualTo(900000L);
        assertThat(criteria.minBedrooms()).isEqualTo(2);
        assertThat(criteria.city()).isEqualTo("Reston");
        assertThat(criteria.keyword()).isEqualTo("pool");
        assertThat(criteria.targetBudget()).isEqualTo(500000L);
        assertThat(criteria.page()).isEqualTo(2);
        assertThat(criteria.pageSize()).isEqualTo(25);
    }

    @Test
    void rejectsMinPriceGreaterThanMaxPrice()
    {
        assertThatThrownBy(() -> SearchCriteria.of(600000L, 500000L, null, null, null, null, null, null))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("minPrice (600000) must not be greater than maxPrice (500000)");
    }

    @Test
    void rejectsNegativePrices()
    {
        assertThatThrownBy(() -> SearchCriteria.of(-1L, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("minPrice must be zero or greater");

        assertThatThrownBy(() -> SearchCriteria.of(null, -100L, null, null, null, null, null, null))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("maxPrice must be zero or greater");
    }

    @Test
    void rejectsNegativeMinBedrooms()
    {
        assertThatThrownBy(() -> SearchCriteria.of(null, null, -1, null, null, null, null, null))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("minBedrooms must be zero or greater");
    }

    @Test
    void rejectsNonPositiveTargetBudget()
    {
        assertThatThrownBy(() -> SearchCriteria.of(null, null, null, null, null, 0L, null, null))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("targetBudget must be greater than zero");
    }

    @Test
    void rejectsPageSizeOfZeroOrLess()
    {
        assertThatThrownBy(() -> SearchCriteria.of(null, null, null, null, null, null, null, 0))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("pageSize must be 1 or greater");

        assertThatThrownBy(() -> SearchCriteria.of(null, null, null, null, null, null, null, -5))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("pageSize must be 1 or greater");
    }

    @Test
    void rejectsPageSizeAboveMaximum()
    {
        assertThatThrownBy(() -> SearchCriteria.of(null, null, null, null, null, null, null, 101))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("pageSize must not exceed 100");
    }

    @Test
    void rejectsPageLessThanOne()
    {
        assertThatThrownBy(() -> SearchCriteria.of(null, null, null, null, null, null, 0, null))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("page must be 1 or greater");
    }

    @Test
    void rejectsOverlongKeyword()
    {
        String keyword = "a".repeat(SearchCriteria.MAX_KEYWORD_LENGTH + 1);

        assertThatThrownBy(() -> SearchCriteria.of(null, null, null, null, keyword, null, null, null))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .hasMessageContaining("keyword must not exceed 200 characters");
    }

    @Test
    void collectsAllErrorsInOneException()
    {
        assertThatThrownBy(() -> SearchCriteria.of(900000L, 100000L, -2, null, null, 0L, 0, 0))
                .isInstanceOf(InvalidSearchCriteriaException.class)
                .satisfies(ex -> assertThat(((InvalidSearchCriteriaException) ex).getErrors())
                        .hasSize(5));
    }
}
