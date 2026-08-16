param(
    [string]$BaseUrl
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = Read-Host "HTTPS model base URL (for example https://model.example.com/)"
}

$uri = $null
if (-not [Uri]::TryCreate($BaseUrl, [UriKind]::Absolute, [ref]$uri)) {
    throw "The model base URL is invalid."
}

$isLocal = $uri.Host -in @("localhost", "127.0.0.1", "::1")
if ($uri.Scheme -ne "https" -and -not $isLocal) {
    throw "A remote model endpoint must use HTTPS so the API key is not sent over plaintext HTTP."
}

$secureKey = Read-Host "New API key (input is hidden)" -AsSecureString
if ($secureKey.Length -eq 0) {
    throw "API key cannot be empty."
}

$keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
try {
    $plainKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)

    [Environment]::SetEnvironmentVariable("AI_MODEL_ENABLED", "true", "User")
    [Environment]::SetEnvironmentVariable("AI_MODEL_BASE_URL", $uri.AbsoluteUri, "User")
    [Environment]::SetEnvironmentVariable("AI_MODEL_API_KEY", $plainKey, "User")
    [Environment]::SetEnvironmentVariable("AI_MODEL_NAME", "deepseek-v4-flash", "User")
    [Environment]::SetEnvironmentVariable("AI_MODEL_REASONING_EFFORT", "medium", "User")
    [Environment]::SetEnvironmentVariable("AI_MODEL_STORE_RESPONSES", "false", "User")
} finally {
    if ($null -ne $keyPointer) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
    }
    $plainKey = $null
    $secureKey.Dispose()
}

Write-Host ""
Write-Host "AI model environment variables were saved for the current Windows user."
Write-Host "Close this window, then restart the AI operations service."
