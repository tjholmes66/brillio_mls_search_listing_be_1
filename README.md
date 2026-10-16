# MLS Search Listing Backend

Spring Boot (4.1.1) REST API for searching MLS property listings loaded from
`src/main/resources/json/sample_listings.json`. No authentication is required;
the API is open for the separate front-end project to consume.

## Running

```
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`.

## API

### `GET /api/listings/search`

Query parameters (all optional):

| Parameter     | Type  | Default | Constraints                                    |
|---------------|-------|---------|------------------------------------------------|
| `minPrice`    | long  | none    | >= 0                                           |
| `maxPrice`    | long  | none    | >= 0, >= `minPrice`                            |
| `minBedrooms` | int   | none    | >= 0                                           |
| `city`        | string| none    | must exist in dataset (case-insensitive)       |
| `keyword`     | string| none    | <= 200 chars, matched case-insensitively against `description` |
| `targetBudget`| long  | none    | > 0                                            |
| `page`        | int   | 1       | >= 1, <= number of available pages             |
| `pageSize`    | int   | 10      | 1-100                                          |

Blank `city`/`keyword` values are treated as "no filter". Trimming is applied
before matching.

Example:

```
GET /api/listings/search?city=Springfield&minPrice=400000&targetBudget=460000&page=1&pageSize=10
```

Response:

```json
{
  "results": [
    {
      "id": "A1",
      "source": "MLS_A",
      "address": "123 Main St, Apt 4B",
      "city": "Springfield",
      "state": "VA",
      "zip": "22150",
      "price": 450000,
      "bedrooms": 2,
      "bathrooms": 1.5,
      "sqft": 980,
      "listedDate": "2026-08-29",
      "status": "active",
      "description": "Bright top-floor condo near shops and transit. Pet friendly.",
      "relevanceScore": 88.4
    }
  ],
  "page": 1,
  "pageSize": 10,
  "totalResults": 4,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

### `GET /api/listings/cities`

Returns the distinct, sorted list of cities present in the dataset. Useful for
populating a front-end dropdown.

## Relevance scoring

Results are ranked by a 0-100 `relevanceScore` blending two signals
(`RelevanceScorer`):

1. **Budget fit** — `1 - min(1, |price - targetBudget| / targetBudget)`.
   A listing priced exactly at the target scores 1.0; one 100%+ away scores 0.
   When no `targetBudget` is given this signal is neutral so ranking falls back
   to recency alone.
2. **Recency** — exponential decay with a 14-day half-life, measured against the
   newest `listedDate` in the dataset (not the wall clock, so results are
   deterministic): listed today = 1.0, 14 days old = 0.5, 28 days = 0.25.

Combined as `100 * (0.6 * budgetFit + 0.4 * recency)` when `targetBudget` is
supplied, otherwise `100 * recency`. Ties break by newest `listedDate`, then
lowest price, then id.

## Error handling

All errors return a consistent JSON body:

```json
{
  "timestamp": "2026-10-16T20:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "minPrice (600000) must not be greater than maxPrice (500000)",
  "path": "/api/listings/search",
  "details": ["minPrice (600000) must not be greater than maxPrice (500000)"]
}
```

| Situation                                            | Status |
|------------------------------------------------------|--------|
| Invalid parameter values (range, cross-field, length)| 400    |
| Non-numeric value for a numeric parameter            | 400    |
| Unknown `city` (details list available cities)       | 404    |
| `page` beyond the last page (details give the range) | 404    |
| Valid search with no matching listings               | 200 with empty `results` |

## Project layout

```
src/main/java/com/brillio/mls/search
├── controller   ListingSearchController, GlobalExceptionHandler
├── service      ListingSearchService (filter/rank/paginate), RelevanceScorer
├── repository   ListingRepository (loads the classpath JSON once at startup)
├── model        Listing
├── dto          SearchCriteria, SearchResponse, SearchResultItem, ApiError
└── exception    InvalidSearchCriteriaException, ListingNotFoundException
```

## Tests

`./mvnw test` — 47 tests:

- `ListingSearchControllerTest` (`@WebMvcTest` + mocked service): response
  shape, parameter passing, and every 400/404 error path.
- `ListingSearchServiceTest` (real JSON data): each filter, combined filters,
  ranking order, pagination slicing, empty results, unknown city, out-of-range
  page.
- `SearchCriteriaTest`: defaults, normalization, and every validation rule.
- `ListingRepositoryTest`: JSON parsing and dataset metadata.
