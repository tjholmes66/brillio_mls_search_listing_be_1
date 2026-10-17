# MLS Search Listing API

REST API for searching MLS property listings. The service streams listings
from the bundled dataset (`src/main/resources/json/sample_listings.json`) and
returns results ranked by relevance.

- **Base URL:** `http://localhost:8080`
- **Content type:** `application/json`
- **Authentication:** none (open/unsecured API)

## Endpoints

| Method | Path                    | Description                                  |
|--------|-------------------------|----------------------------------------------|
| GET    | `/api/listings/search`  | Search listings with filters, ranking, pagination |
| GET    | `/api/listings/cities`  | List distinct cities available for filtering |

---

## GET `/api/listings/search`

Returns a ranked, paginated page of listings matching the supplied filters.

### Query parameters

All parameters are optional. Filters combine with AND logic.

| Parameter     | Type    | Default | Rules                                                             |
|---------------|---------|---------|-------------------------------------------------------------------|
| `minPrice`    | long    | –       | Must be `>= 0`. Must be `<= maxPrice` when both are supplied.     |
| `maxPrice`    | long    | –       | Must be `>= 0`. Must be `>= minPrice` when both are supplied.     |
| `minBedrooms` | int     | –       | Must be `>= 0`.                                                   |
| `city`        | string  | –       | Case-insensitive. Must exist in the dataset, else `404`. Blank/whitespace is treated as "no filter". |
| `keyword`     | string  | –       | Case-insensitive substring match against `description`. Max 200 chars. Blank/whitespace is treated as "no filter". |
| `targetBudget`| long    | –       | Must be `> 0`. Used only for ranking, not filtering.              |
| `page`        | int     | `1`     | Must be `>= 1`. Must not exceed the number of available pages.    |
| `pageSize`    | int     | `10`    | Must be `1`–`100`.                                                |

### Response `200 OK`

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

| Field          | Type             | Description                                        |
|----------------|------------------|----------------------------------------------------|
| `results`      | array of item    | Ranked page of matching listings (see below).      |
| `page`         | int              | Current page (1-based).                            |
| `pageSize`     | int              | Page size actually used.                           |
| `totalResults` | long             | Total matches across all pages.                    |
| `totalPages`   | int              | Total number of pages (`0` when no matches).       |
| `hasNext`      | boolean          | Whether a next page exists.                        |
| `hasPrevious`  | boolean          | Whether a previous page exists.                    |

#### Result item

| Field            | Type    | Description                                              |
|------------------|---------|----------------------------------------------------------|
| `id`             | string  | Listing identifier.                                      |
| `source`         | string  | Source MLS feed.                                         |
| `address`        | string  | Street address.                                          |
| `city`           | string  | City.                                                    |
| `state`          | string  | State code.                                              |
| `zip`            | string  | ZIP code.                                                |
| `price`          | long    | Listing price.                                           |
| `bedrooms`       | int     | Bedroom count.                                           |
| `bathrooms`      | double  | Bathroom count.                                          |
| `sqft`           | int     | Square footage.                                          |
| `listedDate`     | date    | `YYYY-MM-DD` listing date.                               |
| `status`         | string  | Listing status (e.g. `active`, `pending`).               |
| `description`    | string  | Free-text description.                                   |
| `relevanceScore` | double  | Ranking score `0`–`100`, one decimal place (see Scoring).|

### Ranking

Results are ordered by `relevanceScore` descending. Ties break by newest
`listedDate`, then lowest `price`, then `id`.

- **Budget fit** — `1 - min(1, |price - targetBudget| / targetBudget)`. `1.0`
  when priced exactly at target, `0` when 100%+ away. Neutral when no
  `targetBudget` is supplied.
- **Recency** — exponential decay with a 14-day half-life measured against the
  newest `listedDate` in the dataset: today `1.0`, 14 days `0.5`, 28 days
  `0.25`.
- **Combined** — `100 * (0.6 * budgetFit + 0.4 * recency)` when `targetBudget`
  is supplied, otherwise `100 * recency`.

### Example calls

**1. No filters (first page, default size)**

```
GET /api/listings/search
```

**2. Price range**

```
GET /api/listings/search?minPrice=400000&maxPrice=550000
```

**3. Minimum bedrooms**

```
GET /api/listings/search?minBedrooms=3
```

**4. By city**

```
GET /api/listings/search?city=Springfield
```

**5. Free-text keyword in description**

```
GET /api/listings/search?keyword=condo
```

**6. Ranked by budget and recency**

```
GET /api/listings/search?targetBudget=460000
```

**7. Pagination (page 2, 5 per page)**

```
GET /api/listings/search?page=2&pageSize=5
```

**8. Combined filters + ranking + pagination**

```
GET /api/listings/search?minPrice=400000&maxPrice=600000&minBedrooms=3&city=Springfield&keyword=kitchen&targetBudget=500000&page=1&pageSize=10
```

**9. Empty result set (valid search, no matches) — `200` with empty `results`**

```
GET /api/listings/search?keyword=no-such-property-xyz
```

---

## GET `/api/listings/cities`

Returns the distinct, case-insensitively sorted list of city names present in
the dataset. Useful for populating a filter dropdown.

### Response `200 OK`

```json
["Chantilly", "Fairfax", "Manassas", "Reston", "Springfield", "Vienna"]
```

### Example call

```
GET /api/listings/cities
```

---

## Errors

Every error returns a consistent JSON body:

| Field       | Type            | Description                                  |
|-------------|-----------------|----------------------------------------------|
| `timestamp` | string (ISO)    | Time the error was produced.                 |
| `status`    | int             | HTTP status code.                            |
| `error`     | string          | HTTP reason phrase.                          |
| `message`   | string          | Human-readable summary.                      |
| `path`      | string          | Request path.                                |
| `details`   | array of string | Extra context (all validation errors, or available options). |

### Error catalog

| Status | Trigger                                                              | Example `message` |
|--------|----------------------------------------------------------------------|-------------------|
| `400`  | `minPrice` greater than `maxPrice`                                   | `minPrice (600000) must not be greater than maxPrice (500000)` |
| `400`  | Negative `minPrice` / `maxPrice`                                     | `minPrice must be zero or greater, got -1` |
| `400`  | Negative `minBedrooms`                                               | `minBedrooms must be zero or greater, got -1` |
| `400`  | Non-positive `targetBudget`                                          | `targetBudget must be greater than zero, got 0` |
| `400`  | `pageSize` `< 1` or `> 100`                                          | `pageSize must be 1 or greater, got 0` |
| `400`  | `page` `< 1`                                                         | `page must be 1 or greater, got -1` |
| `400`  | `keyword` longer than 200 chars                                      | `keyword must not exceed 200 characters, got 201` |
| `400`  | Non-numeric value for a numeric parameter                            | `Invalid value 'abc' for parameter 'minPrice'; expected type Long` |
| `404`  | `city` not present in the dataset                                    | `No listings found for city 'Atlantis'` |
| `404`  | `page` beyond the last page                                          | `Requested page 9 is out of range; the search returned 12 listings across 3 page(s)` |
| `200`  | Valid search with zero matches                                       | _(not an error — empty `results`)_ |

### Error examples

**`400` — minPrice greater than maxPrice**

```
GET /api/listings/search?minPrice=600000&maxPrice=500000
```

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

**`400` — pageSize of zero**

```
GET /api/listings/search?pageSize=0
```

```json
{
  "timestamp": "2026-10-16T20:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "pageSize must be 1 or greater, got 0",
  "path": "/api/listings/search",
  "details": ["pageSize must be 1 or greater, got 0"]
}
```

**`400` — multiple invalid values at once** (all reported together)

```
GET /api/listings/search?minPrice=900000&maxPrice=100000&minBedrooms=-2&targetBudget=0&page=0&pageSize=0
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "minPrice (900000) must not be greater than maxPrice (100000); minBedrooms must be zero or greater, got -2; targetBudget must be greater than zero, got 0; page must be 1 or greater, got 0; pageSize must be 1 or greater, got 0",
  "path": "/api/listings/search",
  "details": [
    "minPrice (900000) must not be greater than maxPrice (100000)",
    "minBedrooms must be zero or greater, got -2",
    "targetBudget must be greater than zero, got 0",
    "page must be 1 or greater, got 0",
    "pageSize must be 1 or greater, got 0"
  ]
}
```

**`400` — non-numeric value**

```
GET /api/listings/search?minPrice=abc
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value 'abc' for parameter 'minPrice'; expected type Long",
  "path": "/api/listings/search",
  "details": []
}
```

**`404` — unknown city** (`details` lists available cities)

```
GET /api/listings/search?city=Atlantis
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "No listings found for city 'Atlantis'",
  "path": "/api/listings/search",
  "details": ["Chantilly", "Fairfax", "Manassas", "Reston", "Springfield", "Vienna"]
}
```

**`404` — page out of range** (`details` gives the valid range)

```
GET /api/listings/search?page=9
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Requested page 9 is out of range; the search returned 12 listings across 3 page(s)",
  "path": "/api/listings/search",
  "details": ["1 <= page <= 3"]
}
```

---

## Quick reference (curl)

```bash
# Search with no filters
curl "http://localhost:8080/api/listings/search"

# Filter by price and bedrooms
curl "http://localhost:8080/api/listings/search?minPrice=400000&maxPrice=550000&minBedrooms=3"

# Filter by city and keyword, ranked for a budget
curl "http://localhost:8080/api/listings/search?city=Springfield&keyword=kitchen&targetBudget=500000"

# Paginate
curl "http://localhost:8080/api/listings/search?page=2&pageSize=5"

# Distinct cities
curl "http://localhost:8080/api/listings/cities"

# Error example (400)
curl -i "http://localhost:8080/api/listings/search?minPrice=600000&maxPrice=500000"
```
