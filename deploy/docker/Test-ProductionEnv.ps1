param(
    [string]$EnvFile = ".\deploy\docker\production.env"
)

$resolvedPath = Resolve-Path -LiteralPath $EnvFile -ErrorAction SilentlyContinue
if (-not $resolvedPath) {
    Write-Error "未找到部署环境文件: $EnvFile"
    exit 1
}

$requiredKeys = @(
    "JWT_SECRET",
    "ADMIN_PASSWORD_HASH",
    "POSTGRES_PASSWORD",
    "REDIS_PASSWORD",
    "WEB_BASE_URL",
    "API_BASE_URL",
    "CORS_ALLOWED_ORIGINS"
)

$values = @{}
Get-Content -LiteralPath $resolvedPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }

    $index = $line.IndexOf("=")
    if ($index -le 0) {
        return
    }

    $key = $line.Substring(0, $index).Trim()
    $value = $line.Substring($index + 1).Trim()
    $values[$key] = $value
}

$errors = New-Object System.Collections.Generic.List[string]
foreach ($key in $requiredKeys) {
    if (-not $values.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($values[$key])) {
        $errors.Add("$key 未配置")
        continue
    }

    if ($values[$key] -like "replace-with*") {
        $errors.Add("$key 仍是模板占位值")
    }
}

if ($values.ContainsKey("JWT_SECRET") -and $values["JWT_SECRET"].Length -lt 32) {
    $errors.Add("JWT_SECRET 长度必须不少于 32 个字符")
}

if ($errors.Count -gt 0) {
    Write-Error ("部署环境预检失败: " + ($errors -join "; "))
    exit 1
}

Write-Host "部署环境预检通过: $resolvedPath"
