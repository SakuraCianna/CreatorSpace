param(
  [string]$BaseUrl = "http://localhost:8088",
  [string]$AdminUsername = "",
  [string]$AdminPassword = $env:CREATORSPACE_ADMIN_PASSWORD
)

$ErrorActionPreference = "Stop"

function Read-DotEnv {
  param([string]$Path)

  $values = @{}
  if (-not (Test-Path -LiteralPath $Path)) {
    return $values
  }

  Get-Content -LiteralPath $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#") -or $line -notmatch "=") {
      return
    }
    $parts = $line -split "=", 2
    $key = $parts[0].Trim()
    $value = $parts[1].Trim()
    if (($value.StartsWith("'") -and $value.EndsWith("'")) -or ($value.StartsWith('"') -and $value.EndsWith('"'))) {
      $value = $value.Substring(1, $value.Length - 2)
    }
    $values[$key] = $value
  }

  return $values
}

function Invoke-SmokeJson {
  param(
    [string]$Name,
    [string]$Path,
    [hashtable]$Headers = @{}
  )

  $uri = "$BaseUrl$Path"
  try {
    $response = Invoke-RestMethod -Uri $uri -Method Get -Headers $Headers -TimeoutSec 15
    if ($null -ne $response.success -and -not $response.success) {
      throw "API returned success=false"
    }
    Write-Host "[OK] $Name"
    return $response
  } catch {
    Write-Host "[FAIL] $Name - $($_.Exception.Message)" -ForegroundColor Red
    throw
  }
}

function Invoke-RouteSmoke {
  param([string]$Path)

  try {
    $response = Invoke-WebRequest -Uri "$BaseUrl$Path" -UseBasicParsing -TimeoutSec 15
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
      throw "HTTP $($response.StatusCode)"
    }
    Write-Host "[OK] route $Path"
  } catch {
    Write-Host "[FAIL] route $Path - $($_.Exception.Message)" -ForegroundColor Red
    throw
  }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$envValues = Read-DotEnv (Join-Path $repoRoot ".env")

if (-not $AdminUsername) {
  $AdminUsername = $envValues["ADMIN_USERNAME"]
}
if (-not $AdminUsername) {
  $AdminUsername = "admin"
}
if (-not $AdminPassword) {
  $AdminPassword = "123456"
}

$BaseUrl = $BaseUrl.TrimEnd("/")

Write-Host "[CreatorSpace] Admin B smoke test"
Write-Host "[CreatorSpace] BaseUrl: $BaseUrl"

$loginBody = @{
  username = $AdminUsername
  password = $AdminPassword
} | ConvertTo-Json

try {
  $login = Invoke-RestMethod -Uri "$BaseUrl/api/admin/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -TimeoutSec 15
  $token = $login.data.accessToken
  if (-not $token) {
    throw "No access token returned"
  }
  Write-Host "[OK] admin login"
} catch {
  Write-Host "[FAIL] admin login - $($_.Exception.Message)" -ForegroundColor Red
  Write-Host "[Hint] Set CREATORSPACE_ADMIN_PASSWORD or pass -AdminPassword if the default password is not 123456."
  exit 1
}

$headers = @{ Authorization = "Bearer $token" }

Invoke-RouteSmoke "/admin/articles"
Invoke-RouteSmoke "/admin/projects"
Invoke-RouteSmoke "/admin/categories"
Invoke-RouteSmoke "/admin/tags"
Invoke-RouteSmoke "/admin/files"
Invoke-RouteSmoke "/admin/pages"

$articles = Invoke-SmokeJson "admin articles list" "/api/admin/articles?page=1&pageSize=1" $headers
Invoke-SmokeJson "admin projects list" "/api/admin/projects?page=1&pageSize=1" $headers | Out-Null
Invoke-SmokeJson "admin article categories" "/api/admin/categories?module=ARTICLE" $headers | Out-Null
Invoke-SmokeJson "admin project categories" "/api/admin/categories?module=PROJECT" $headers | Out-Null
Invoke-SmokeJson "tags list" "/api/tags" | Out-Null
Invoke-SmokeJson "admin files list" "/api/admin/files?page=1&pageSize=1" $headers | Out-Null
Invoke-SmokeJson "admin page settings" "/api/admin/site/settings" $headers | Out-Null

$firstArticle = $null
if ($articles.data.records -and $articles.data.records.Count -gt 0) {
  $firstArticle = $articles.data.records[0]
}
if ($firstArticle -and $firstArticle.id) {
  Invoke-SmokeJson "admin article versions" "/api/admin/articles/$($firstArticle.id)/versions" $headers | Out-Null
} else {
  Write-Host "[SKIP] admin article versions - no article record available"
}

Write-Host "[CreatorSpace] Admin B smoke test passed."
