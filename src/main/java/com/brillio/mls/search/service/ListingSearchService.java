package com.brillio.mls.search.service;

import java.time.LocalDate;
import java.util.List;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResponse;
import com.brillio.mls.search.dto.SearchResultItem;
import com.brillio.mls.search.exception.ListingNotFoundException;
import com.brillio.mls.search.repository.ListingRepository;
import com.brillio.mls.search.repository.ListingRepository.ScanResult;
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
        ScanResult scan = repository.scan(criteria);
        validateCityExists(criteria.city(), scan.distinctCities());

        List<ScoredListing> ranked = scan.matches().stream()
                .map(item -> new ScoredListing(item,
                        scorer.score(item.price(), item.listedDate(), criteria.targetBudget(), scan.newestListedDate())))
                .sorted(ListingSearchService::byRelevance)
                .toList();

        return paginate(ranked, criteria);
    }

    public List<String> availableCities()
    {
        return repository.findDistinctCities();
    }

    private void validateCityExists(String city, List<String> distinctCities)
    {
        if (city == null)
        {
            return;
        }
        boolean exists = distinctCities.stream().anyMatch(c -> c.equalsIgnoreCase(city));
        if (!exists)
        {
            throw new ListingNotFoundException(
                    "No listings found for city '" + city + "'", distinctCities);
        }
    }

    private static int byRelevance(ScoredListing a, ScoredListing b)
    {
        int byScore = Double.compare(b.score(), a.score());
        if (byScore != 0)
        {
            return byScore;
        }
        int byDate = b.item().listedDate().compareTo(a.item().listedDate());
        if (byDate != 0)
        {
            return byDate;
        }
        int byPrice = Long.compare(a.item().price(), b.item().price());
        if (byPrice != 0)
        {
            return byPrice;
        }
        return a.item().id().compareTo(b.item().id());
    }

    private SearchResponse paginate(List<ScoredListing> ranked, SearchCriteria criteria)
    {
        long totalResults = ranked.size();
        int totalPages = totalResults == 0 ? 0 : (int) Math.ceil((double) totalResults / criteria.pageSize());

        if (criteria.page() > 1 && criteria.page() > totalPages)
        {
            throw new ListingNotFoundException(
                    "Requested page " + criteria.page() + " is out of range; the search returned "
                            + totalResults + " listings across " + totalPages + " page(s)",
                    List.of("1 <= page <= " + totalPages));
        }

        int fromIndex = (criteria.page() - 1) * criteria.pageSize();
        int toIndex = Math.min(fromIndex + criteria.pageSize(), ranked.size());

        List<SearchResultItem> pageItems = ranked.subList(fromIndex, toIndex).stream()
                .map(ListingSearchService::toResultItem)
                .toList();

        return SearchResponse.of(pageItems, criteria.page(), criteria.pageSize(), totalResults);
    }

    private static SearchResultItem toResultItem(ScoredListing scored)
    {
        SearchResultItem item = scored.item();
        return new SearchResultItem(item.id(), item.source(), item.address(), item.city(), item.state(),
                item.zip(), item.price(), item.bedrooms(), item.bathrooms(), item.sqft(), item.listedDate(),
                item.status(), item.description(), scored.score());
    }

    private record ScoredListing(SearchResultItem item, double score)
    {
    }
}
