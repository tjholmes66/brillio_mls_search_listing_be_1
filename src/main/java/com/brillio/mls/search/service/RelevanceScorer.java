package com.brillio.mls.search.service;

import java.time.LocalDate;

import com.brillio.mls.search.model.Listing;
import org.springframework.stereotype.Component;

/**
 * Scores how relevant a listing is to a search.
 *
 * The score is a 0-100 blend of two signals:
 *
 * 1. Budget fit: how close the price is to the searcher's targetBudget, as
 *    1 - min(1, |price - targetBudget| / targetBudget). A listing priced exactly
 *    at the target scores 1.0; one priced 100%+ away scores 0. When no
 *    targetBudget is supplied the budget signal is neutral (1.0) so ranking
 *    falls back to recency alone.
 *
 * 2. Recency: exponential decay with a 14-day half-life, measured against the
 *    newest listedDate in the dataset rather than the wall clock, so results
 *    are deterministic. A listing listed today scores 1.0, one from 14 days ago
 *    scores 0.5, one from 28 days ago scores 0.25, and so on.
 *
 * Combined: 100 * (0.6 * budgetFit + 0.4 * recency) when a targetBudget is
 * given, otherwise 100 * recency. Budget is weighted higher because a search
 * with an explicit budget is primarily a affordability signal; recency breaks
 * ties and keeps otherwise-equal listings ordered by freshness.
 */
@Component
public class RelevanceScorer
{
    static final double BUDGET_WEIGHT = 0.6;
    static final double RECENCY_WEIGHT = 0.4;
    static final double RECENCY_HALF_LIFE_DAYS = 14.0;
    private static final double NEUTRAL_BUDGET_FIT = 1.0;

    public double score(Listing listing, Long targetBudget, LocalDate referenceDate)
    {
        double budgetFit = targetBudget == null
                ? NEUTRAL_BUDGET_FIT
                : budgetFit(listing.price(), targetBudget);
        double recency = recency(listing.listedDate(), referenceDate);

        double blended = targetBudget == null
                ? recency
                : BUDGET_WEIGHT * budgetFit + RECENCY_WEIGHT * recency;

        return Math.round(blended * 1000.0) / 10.0;
    }

    private double budgetFit(long price, long targetBudget)
    {
        double relativeDistance = Math.abs(price - targetBudget) / (double) targetBudget;
        return Math.max(0.0, 1.0 - relativeDistance);
    }

    private double recency(LocalDate listedDate, LocalDate referenceDate)
    {
        long ageInDays = Math.max(0, referenceDate.toEpochDay() - listedDate.toEpochDay());
        return Math.pow(0.5, ageInDays / RECENCY_HALF_LIFE_DAYS);
    }
}
