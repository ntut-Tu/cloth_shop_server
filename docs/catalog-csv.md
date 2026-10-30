# Vendor catalog CSV

Authenticated vendors can download `GET /api/products/csv/template`, export their own
catalog with `GET /api/products/csv/export?offset=0`, and upload a UTF-8 CSV as multipart field
`file` to `POST /api/products/csv/import`. Authentication uses the existing `token`
cookie. All three routes require an active vendor account.

An export returns one page that fits the configured import row and byte limits. If
there are more rows, the response includes `X-Next-Offset`; request the next page
with that value until the header is absent. Each page contains the template header
and can be imported independently. A single row that exceeds the byte limit is
reported with HTTP 413.

The template resource is loaded and parsed at startup. Imports must use its exact
header and column order:

```csv
operation,product_id,variant_id,name,description,image_url,category,is_list,color,size,price,stock
```

| Operation | Identifiers | Fields | Effect |
| --- | --- | --- | --- |
| `ADD` | Leave both IDs empty | Supply name, category, color, size, price, stock. Other product fields are optional. | Create a product and its first variant. |
| `ADD` | Supply `product_id`; leave `variant_id` empty | Supply color, size, price, stock; leave product fields empty. | Add a variant to an owned product. |
| `EDIT` | Supply `product_id`; optionally supply `variant_id` | Include only fields to change. Variant fields require `variant_id`. | Update an owned product and/or variant. Blank cells leave existing values intact. |
| `DELETE` | Supply `product_id`; optionally supply `variant_id`; leave other fields empty | None | Delete one variant or the product and its variants. Existing order references or other constraints can prevent deletion. |

`price` and `stock` must be nonnegative integers; IDs must be positive integers.
`is_list` accepts `true` or `false`. Quoted fields can contain commas, quotes, and
newlines. A leading apostrophe escapes text that begins with `=`, `+`, `-`, or `@`;
exports add this prefix where needed for spreadsheet safety. Text fields retain
their original leading and trailing spaces when exported and imported.

The response reports each data row's operation, resulting IDs, and success or error.
Every row has its own database transaction, so one invalid row does not roll back
successful rows. The default limits are 1 MiB and 1,000 data rows; these and the
template resource are mapped under `catalog.csv` in application properties.
