# G-Commerce API docs

The developer documentation of the G-Commerce API: guides written in Markdown with
[Astro Starlight](https://starlight.astro.build), and an interactive API reference rendered by
[Scalar](https://scalar.com) from the published OpenAPI document.

## Development

Requires Node.js 22.12 or newer.

```bash
npm ci
npm run dev       # http://localhost:4321
npm run build     # static site in dist/; fails on broken links between pages
npm run preview   # serves dist/
```

The "Test Request" button of the API reference calls the production API, which only accepts requests from the
deployed docs site (CORS), so it doesn't work from `localhost`.

## Structure

| Path | Content |
|---|---|
| `src/content/docs/` | The pages: introduction, quickstart, guides and changelog (`.md` / `.mdx`) |
| `public/openapi.json` | The OpenAPI document of the API, rendered at `/api-reference` |
| `astro.config.mjs` | Site, sidebar and Scalar configuration |
| `src/styles/custom.css` | Theme colors |

`public/openapi.json` is generated from the API's code, not edited by hand. After changing the API, regenerate it
from the repository root:

```bash
./mvnw test -Dtest=OpenApiSpecTest -Dopenapi.update=true
```

The guides repeat some values of the API's configuration, such as token lifetimes, rate limits and the maximum page
size. Update them when those settings change.

## Deployment

Vercel builds and deploys the site from this folder (project root directory `docs-site`). The production domain
that Vercel exposes at build time (`VERCEL_PROJECT_PRODUCTION_URL`) becomes the site URL used by the sitemap.
