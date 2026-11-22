# productcatalogservice

Run the following command to restore dependencies to `vendor/` directory:

    dep ensure --vendor-only

## Catalog data (`products.json`)

The catalog lives in `products.json` at the root of this service and is copied
into the container image at build time. Each entry has:

```json
{
  "id": "POUROVERSE1",
  "name": "Slow Morning Pour-Over Set",
  "description": "...",
  "picture": "/static/img/products/pour-over-set.jpg",
  "priceUsd": { "currencyCode": "USD", "units": 46, "nanos": 0 },
  "categories": ["kitchen", "home"]
}
```

`picture` points at an asset served by the **frontend** service from
`/static/img/products/`, so any new product needs a matching image added there.
`categories` drive the filters and department nav on the storefront home page.

> Note: `microservices-project-productcatalogservice/products.json` (this file)
> is the copy used by the build; the nested duplicate folder is a stray copy.

## Dynamic catalog reloading / artificial delay

This service has a "dynamic catalog reloading" feature that is purposefully
not well implemented. The goal of this feature is to allow you to modify the
`products.json` file and have the changes be picked up without having to
restart the service.

However, this feature is bugged: the catalog is actually reloaded on each
request, introducing a noticeable delay in the frontend. This delay will also
show up in profiling tools: the `parseCatalog` function will take more than 80%
of the CPU time.

You can trigger this feature (and the delay) by sending a `USR1` signal and
remove it (if needed) by sending a `USR2` signal:

```
# Trigger bug
kubectl exec \
    $(kubectl get pods -l app=productcatalogservice -o jsonpath='{.items[0].metadata.name}') \
    -c server -- kill -USR1 1
# Remove bug
kubectl exec \
    $(kubectl get pods -l app=productcatalogservice -o jsonpath='{.items[0].metadata.name}') \
    -c server -- kill -USR2 1
```

## Latency injection

This service has an `EXTRA_LATENCY` environment variable. This will inject a sleep for the specified [time.Duration](https://golang.org/pkg/time/#ParseDuration) on every call to
to the server.

For example, use `EXTRA_LATENCY="5.5s"` to sleep for 5.5 seconds on every request.
