package com.brillio.mls.search.repository;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import com.brillio.mls.search.model.Listing;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class ListingRepository
{
    private static final String DEFAULT_RESOURCE = "json/sample_listings.json";

    private final List<Listing> listings;

    public ListingRepository()
    {
        this(DEFAULT_RESOURCE);
    }

    public ListingRepository(String resourcePath)
    {
        this.listings = load(resourcePath);
    }

    public List<Listing> findAll()
    {
        return listings;
    }

    public List<String> findDistinctCities()
    {
        return listings.stream()
                .map(Listing::city)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public LocalDate newestListedDate()
    {
        return listings.stream()
                .map(Listing::listedDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.EPOCH);
    }

    private static List<Listing> load(String resourcePath)
    {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = resource.getInputStream())
        {
            List<Listing> loaded = mapper.readValue(in, new TypeReference<List<Listing>>() {});
            if (loaded == null)
            {
                throw new IllegalStateException("Listing resource '" + resourcePath + "' contains no data");
            }
            return List.copyOf(loaded);
        }
        catch (JacksonException e)
        {
            throw new IllegalStateException("Failed to parse listings from '" + resourcePath + "'", e);
        }
        catch (java.io.IOException e)
        {
            throw new IllegalStateException("Failed to read listing resource '" + resourcePath + "'", e);
        }
    }
}
