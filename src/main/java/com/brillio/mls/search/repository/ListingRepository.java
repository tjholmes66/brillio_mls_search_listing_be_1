package com.brillio.mls.search.repository;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.brillio.mls.search.dto.SearchCriteria;
import com.brillio.mls.search.dto.SearchResultItem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;

/**
 * Streams listings straight from the classpath JSON file instead of loading
 * them all into memory. Each record is deserialized one at a time, tested
 * against the search criteria, and either projected to a SearchResultItem or
 * discarded immediately. The only state retained across the whole scan is the
 * list of matches plus a bounded set of distinct city names, so memory use
 * scales with the match count, not the file size.
 */
@Repository
public class ListingRepository
{
    private static final String DEFAULT_RESOURCE = "json/sample_listings.json";

    private final String resourcePath;
    private final ObjectMapper mapper = new ObjectMapper();

    public ListingRepository()
    {
        this(DEFAULT_RESOURCE);
    }

    public ListingRepository(String resourcePath)
    {
        this.resourcePath = resourcePath;
    }

    /**
     * Single streaming pass over the file: applies the criteria filters,
     * tracks the newest listedDate seen anywhere in the file (used as the
     * deterministic recency reference), and collects distinct cities.
     */
    public ScanResult scan(SearchCriteria criteria)
    {
        String keyword = criteria.keyword() == null ? null : criteria.keyword().toLowerCase();
        List<SearchResultItem> matches = new ArrayList<>();
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        AtomicReference<LocalDate> newestListedDate = new AtomicReference<>(LocalDate.EPOCH);

        streamHits(hit -> {
            cities.add(hit.city());
            newestListedDate.updateAndGet(current -> hit.listedDate().isAfter(current) ? hit.listedDate() : current);
            if (matches(hit, criteria, keyword))
            {
                matches.add(toItem(hit));
            }
        });

        return new ScanResult(List.copyOf(matches), newestListedDate.get(), List.copyOf(cities));
    }

    public List<String> findDistinctCities()
    {
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        streamHits(hit -> cities.add(hit.city()));
        return List.copyOf(cities);
    }

    private void streamHits(Consumer<ListingHit> onHit)
    {
        try (InputStream in = open();
             MappingIterator<ListingHit> iterator = mapper.readerFor(ListingHit.class).readValues(in))
        {
            while (iterator.hasNextValue())
            {
                onHit.accept(iterator.nextValue());
            }
        }
        catch (JacksonException e)
        {
            throw new IllegalStateException("Failed to parse listings from '" + resourcePath + "'", e);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to read listing resource '" + resourcePath + "'", e);
        }
    }

    private InputStream open() throws IOException
    {
        return new ClassPathResource(resourcePath).getInputStream();
    }

    private static boolean matches(ListingHit hit, SearchCriteria criteria, String keywordLower)
    {
        if (criteria.minPrice() != null && hit.price() < criteria.minPrice())
        {
            return false;
        }
        if (criteria.maxPrice() != null && hit.price() > criteria.maxPrice())
        {
            return false;
        }
        if (criteria.minBedrooms() != null && hit.bedrooms() < criteria.minBedrooms())
        {
            return false;
        }
        if (criteria.city() != null && !hit.city().equalsIgnoreCase(criteria.city()))
        {
            return false;
        }
        return keywordLower == null || hit.description().toLowerCase().contains(keywordLower);
    }

    private static SearchResultItem toItem(ListingHit hit)
    {
        return new SearchResultItem(hit.id(), hit.source(), hit.address(), hit.city(), hit.state(), hit.zip(),
                hit.price(), hit.bedrooms(), hit.bathrooms(), hit.sqft(), hit.listedDate(), hit.status(),
                hit.description(), 0.0);
    }

    record ListingHit(String id, String source, String address, String city, String state, String zip,
                      long price, int bedrooms, double bathrooms, int sqft, LocalDate listedDate,
                      String status, String description)
    {
    }

    public record ScanResult(List<SearchResultItem> matches, LocalDate newestListedDate, List<String> distinctCities)
    {
    }
}
