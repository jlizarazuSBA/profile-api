# Federal Business Profile API

A Spring Boot API that returns a public profile for a federal contractor,
looked up by its SAM.gov Unique Entity ID (UEI).

- **Registration** comes from the SAM.gov Entity Management API.
- **Award history** comes from USAspending.gov prime contract awards.
- Both sources are **off by default**; the API then returns clearly labeled
  sample data, so it runs with no keys or network access.

## Endpoint
GET /api/v1/business-profiles/{uei}

The UEI must be 12 uppercase letters or digits, must not contain `I` or `O`,
and must not start with `0`. Invalid UEIs return `400` with an RFC 9457
`application/problem+json` body.

## Example response

```json
{
  "uei": "KDH9SC281WL8",
  "sampleData": false,
  "registration": {
    "status": "AVAILABLE",
    "legalBusinessName": "BULLS RUN GROUP, LLC, THE",
    "registrationStatus": "Active"
  },
  "awardHistory": {
    "status": "AVAILABLE",
    "awards": [
      {
        "awardId": "FA238425CB024",
        "description": "DELIVER A COGNITIVE PSYCHOLOGY-BASED IW SUPPORT TOOL.",
        "awardingAgency": "Department of Defense"
      }
    ]
  },
  "notices": [
    "Registration data retrieved from SAM.gov.",
    "Award history retrieved from USAspending.gov prime contract awards."
  ]
}
```

## Source status values

Each section reports where its data came from:

| Status | Meaning |
|---|---|
| `SAMPLE` | Source disabled; fictional demonstration data |
| `AVAILABLE` | Real data retrieved and validated |
| `NO_RECORDS` | Source queried; nothing matched the UEI |
| `UNAVAILABLE` | Source failed, returned invalid data, or could not be queried |

`sampleData` is `true` whenever any section is `SAMPLE`. Upstream error
details are never included in responses.

## How matching works

- **Registration:** `AVAILABLE` only when SAM.gov returns exactly one record
  whose UEI matches and whose name and status are present.
- **Awards:** USAspending is searched by the SAM.gov legal business name
  (its UEI text search did not return results in live testing), then only
  awards whose `Recipient UEI` exactly matches are kept. The award search is
  skipped unless registration is `AVAILABLE`.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SAM_ENABLED` | `false` | Enable SAM.gov registration lookup |
| `SAM_API_KEY` | none | Required when `SAM_ENABLED=true`; startup fails without it |
| `USASPENDING_ENABLED` | `false` | Enable USAspending award history; no key needed |

USAspending settings in `application.properties`: 3s connect timeout,
10s read timeout, 5-year lookback, 5 results.

A personal SAM.gov public API key is limited to 10 requests per day.
Never commit the key; set it only in your shell session.

## Running locally

Requires JDK 21 or later (a JRE is not enough).

```powershell
# Sample mode (no keys, no network)
.\mvnw.cmd spring-boot:run

# Live mode (PowerShell)
$secure = Read-Host "SAM API key" -AsSecureString
$env:SAM_API_KEY = [System.Net.NetworkCredential]::new("", $secure).Password
$env:SAM_ENABLED = "true"
$env:USASPENDING_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

Then:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/business-profiles/KDH9SC281WL8
```

## Tests

```powershell
.\mvnw.cmd clean test
```

44 tests cover UEI validation, each source status path, strict matching,
upstream failures (including 502s), and configuration startup rules.
External APIs are mocked; tests never call SAM.gov or USAspending.

## Known limitations

- Award results are capped and sorted by amount, so similarly named
  recipients with larger awards could crowd out the matching business.
- Only prime contract awards are included; subcontracts and grants are not.