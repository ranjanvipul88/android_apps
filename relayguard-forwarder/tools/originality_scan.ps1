param(
    [Parameter(Mandatory=$true)]
    [string]$ApkResourceDir,
    [string]$ProjectDir = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectDir)) {
    $ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

if (!(Test-Path $ApkResourceDir)) {
    throw "APK resource directory not found: $ApkResourceDir"
}

$excluded = @("\\build\\", "\\.gradle\\", "\\_analysis_tmp\\")
$gitFiles = & git -c core.excludesfile= -C $ProjectDir ls-files --cached --others --exclude-standard 2>$null
$projectFiles = if ($LASTEXITCODE -eq 0 -and $gitFiles) {
    $gitFiles | ForEach-Object { Get-Item -LiteralPath (Join-Path $ProjectDir $_) -ErrorAction SilentlyContinue }
} else {
    Get-ChildItem -Path $ProjectDir -Recurse -File -ErrorAction SilentlyContinue
}
$projectFiles = $projectFiles | Where-Object {
    $path = $_.FullName
    -not ($excluded | Where-Object { $path.Contains($_) })
}

$projectText = foreach ($file in $projectFiles) {
    try { Get-Content -LiteralPath $file.FullName -Raw -ErrorAction Stop } catch { "" }
}
$joinedProjectText = [string]::Join("`n", $projectText)

$resourceNames = Get-ChildItem -Path $ApkResourceDir -Recurse -File |
    Where-Object { $_.Name.Length -ge 14 } |
    Select-Object -ExpandProperty Name -Unique

$stringCandidates = Select-String -Path (Join-Path $ApkResourceDir "res\values\strings.xml") -Pattern "<string " -ErrorAction SilentlyContinue |
    ForEach-Object {
        if ($_.Line -match ">([^<]{24,})<") { $Matches[1].Trim() }
    } |
    Where-Object {
        $_ -and
        $_ -notmatch "^https?://" -and
        $_ -notmatch "^[A-Z0-9_{} %.,:-]+$"
    } |
    Select-Object -Unique

$matches = @()
foreach ($name in $resourceNames) {
    if ($joinedProjectText.Contains($name)) { $matches += "resource filename: $name" }
}
foreach ($text in $stringCandidates) {
    if ($joinedProjectText.Contains($text)) { $matches += "string: $text" }
}

if ($matches.Count -gt 0) {
    Write-Host "Potential copied APK material found:" -ForegroundColor Red
    $matches | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Originality scan passed. No long APK strings or resource filenames were found in the RelayGuard project." -ForegroundColor Green
