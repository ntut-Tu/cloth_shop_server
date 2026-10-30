# Category-aware product filters

The public filter endpoints use one request shape for every product category. Filter definitions in `src/main/resources/application.yml` determine which fields, input controls, operators, and options are available. Unknown fields or unsupported operators return HTTP 400.

## Discover the schema

`GET /api/products/filters/schema?category=cloth`

The response lists common fields plus fields for the requested category. `category=all` exposes only common fields. Supported categories are `all`, `cloth`, `pants`, `shoes`, and `accessories`.

## Search products

`POST /api/products/filters/search`

```json
{
  "category": "cloth",
  "filters": [
    {"key": "price", "operator": "BETWEEN", "values": ["100", "300"]},
    {"key": "color", "operator": "IN", "values": ["Black", "Blue"]},
    {"key": "inStock", "operator": "EQ", "values": ["true"]},
    {"key": "material", "operator": "CONTAINS", "values": ["cotton"]}
  ],
  "sort": "price_asc",
  "page": 1,
  "pageSize": 30
}
```

Pages start at 1. Page size defaults to 30 and is capped at 100. Sort values are `newest`, `sold`, `rating`, `price_asc`, and `price_desc`. Filters are combined with AND. Variant filters such as price, color, size, and positive stock must all match the same variant. A false `inStock` value matches products without any available variant. Price summaries and price sorting use variants that satisfy positive variant filters. Only listed products from active stores are returned.

## Maintain custom fields

`PUT /api/products/filters/{productId}/attributes` requires the existing `token` cookie for a vendor who owns the product. The JSON body is a partial map of category-specific custom fields. A `null` value removes an attribute.

```json
{"material": "Cotton"}
```

Attributes use the `product_filter_attribute` table. The filter schema must define a field with `source: CUSTOM` before it can be written or queried. The database primary key permits one value per product and custom field.
