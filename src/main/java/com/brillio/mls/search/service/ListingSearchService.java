package com.brillio.mls.search.service;

import java.time.LocalDate;
import java.util.List;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResponse;
import com.brillio.mls.search.dto.SearchResultItem;
import com.brillio.mls.search.exception.ListingNotFoundException;
import com.brillio.mls.search.model.Listing;
import com.brillio.mls.search.repository.ListingRepository;
import org.springframework.stereotype.Service;

@Service
public class ListingSearchService
{
    private final ListingRepository repository;
    private final RelevanceScorer scorer;

    public ListingSearchService(ListingRepository repository, RelevanceScorer scorer)
    {
        this.repository = repository;
        this.scorer = scorer;
    }

    public SearchResponse search(SearchCriteria criteria)
    {
        validateCityExists(criteria.city());

        LocalDate referenceDate = repository.newestListedDate();

        List<ScoredListing> matches = repository.findAll().stream()
                .filter(listing -> matchesPrice(listing, criteria))
                .filter(listing -> matchesBedrooms(listing, criteria))
                .filter(listing -> matchesCity(listing, criteria))
                .filter(listing -> matchesKeyword(listing, criteria))
                .map(listing -> new ScoredListing(listing, scorer.score(listing, criteria.targetBudget(), referenceDate)))
                .sorted(ListingSearchService::byRelevance)
                .toList();

        return paginate(matches, criteria);
    }

    public List<String> availableCities()
    {
        return repository.findDistinctCities();
    }

    private void validateCityExists(String city)
    {
        if (city == null)
        {
            return;
        }
        List<String> cities = repository.findDistinctCities();
        boolean exists = cities.stream().anyMatch(c -> c.equalsIgnoreCase(city));
        if (!exists)
        {
            throw new ListingNotFoundException(
                    "No listings found for city '" + city + "'", cities);
        }
    }

    private boolean matchesPrice(Listing listing, SearchCriteria criteria)
    {
        if (criteria.minPrice() != null && listing.price() < criteria.minPrice())
        {
            return false;
        }
        return criteria.maxPrice() == null || listing.price() <= criteria.maxPrice();
    }

    private boolean matchesBedrooms(Listing listing, SearchCriteria criteria)
    {
        return criteria.minBedrooms() == null || listing.bedrooms() >= criteria.minBedrooms();
    }

    private boolean matchesCity(Listing listing, SearchCriteria criteria)
    {
        return criteria.city() == null || listing.city().equalsIgnoreCase(criteria.city());
    }

    private boolean matchesKeyword(Listing listing, SearchCriteria criteria)
    {
        return criteria.keyword() == null
                || listing.description().toLowerCase().contains(criteria.keyword().toLowerCase());
    }

    private static int byRelevance(ScoredListing a, ScoredListing b)
    {
        int byScore = Double.compare(b.score(), a.score());
        if (byScore != 0)
        {
            return byScore;
        }
        int byDate = b.listing().listedDate().compareTo(a.listing().listedDate());
        if (byDate != 0)
        {
            return byDate;
        }
        int byPrice = Long.compare(a.listing().price(), b.listing().price());
        if (byPrice != 0)
        {
            return byPrice;
        }
        return a.listing().id().compareTo(b.listing().id());
    }

    private SearchResponse paginate(List<ScoredListing> matches, SearchCriteria criteria)
    {
        long totalResults = matches.size();
        int totalPages = totalResults == 0 ? 0 : (int) Math.ceil((double) totalResults / criteria.pageSize());

        if (criteria.page() > 1 && criteria.page() > totalPages)
        {
            throw new ListingNotFoundException(
                    "Requested page " + criteria.page() + " is out of range; the search returned "
                            + totalResults + " listings across " + totalPages + " page(s)",
                    List.of("1 <= page <= " + totalPages));
        }

        int fromIndex = (criteria.page() - 1) * criteria.pageSize();
        int toIndex = Math.min(fromIndex + criteria.pageSize(), matches.size());

        List<SearchResultItem> pageItems = matches.subList(fromIndex, toIndex).stream()
                .map(ListingSearchService::toResultItem)
                .toList();

        return SearchResponse.of(pageItems, criteria.page(), criteria.pageSize(), totalResults);
    }

    private static SearchResultItem toResultItem(ScoredListing scored)
    {
        Listing listing = scored.listing();
        return new SearchResultItem(
                listing.id(),
                listing.source(),
                listing.address(),
                listing.city(),
                listing.state(),
                listing.zip(),
                listing.price(),
                listing.bedrooms(),
                listing.bathrooms(),
                listing.sqft(),
                listing.listedDate(),
                listing.status(),
                listing.description(),
                scored.score());
    }

    private record ScoredListing(Listing listing, double score)
    {
    }
}
