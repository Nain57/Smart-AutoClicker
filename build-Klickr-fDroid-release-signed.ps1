param(
    [string]$SigningStorePassword,
    [string]$SigningKeyAlias,
    [string]$SigningKeyPassword,
    [string]$ReleaseKeystorePassphrase,
    [string]$ReleaseKeystoreAscPath = "smartautoclicker.jks.asc",
    [switch]$SkipClean
)

$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$localPropertiesPath = Join-Path $rootDir "local.properties"
$keystorePath = Join-Path $rootDir "smartautoclicker/smartautoclicker.jks"
$releaseKeystoreAscFullPath = Join-Path $rootDir $ReleaseKeystoreAscPath
$apkOutputPath = Join-Path $rootDir "smartautoclicker/build/outputs/apk/fDroid/release/smartautoclicker-fDroid-release.apk"
$finalApkPath = Join-Path $rootDir "Klickr-fDroid-release-signed.apk"

function Get-LocalPropertyValue {
    param(
        [string]$Path,
        [string]$Key
    )

    if (-not (Test-Path $Path)) { return $null }

    foreach ($line in Get-Content -Path $Path) {
        if ($line -match "^$([regex]::Escape($Key))=(.*)$") {
            return $Matches[1].Trim()
        }
    }

    return $null
}

if (-not $SigningStorePassword) { $SigningStorePassword = $env:SIGNING_STORE_PASSWORD }
if (-not $SigningKeyAlias) { $SigningKeyAlias = $env:SIGNING_KEY_ALIAS }
if (-not $SigningKeyPassword) { $SigningKeyPassword = $env:SIGNING_KEY_PASSWORD }

if (-not $SigningStorePassword) { $SigningStorePassword = Get-LocalPropertyValue -Path $localPropertiesPath -Key "signingStorePassword" }
if (-not $SigningKeyAlias) { $SigningKeyAlias = Get-LocalPropertyValue -Path $localPropertiesPath -Key "signingKeyAlias" }
if (-not $SigningKeyPassword) { $SigningKeyPassword = Get-LocalPropertyValue -Path $localPropertiesPath -Key "signingKeyPassword" }
if (-not $ReleaseKeystorePassphrase) { $ReleaseKeystorePassphrase = $env:RELEASE_KEYSTORE_PASSPHRASE }
if (-not $ReleaseKeystorePassphrase) { $ReleaseKeystorePassphrase = Get-LocalPropertyValue -Path $localPropertiesPath -Key "releaseKeystorePassphrase" }

if (-not (Test-Path $keystorePath) -and (Test-Path $releaseKeystoreAscFullPath)) {
    if (-not $ReleaseKeystorePassphrase) {
        throw "Keystore is missing and '$ReleaseKeystoreAscPath' exists, but release keystore passphrase is not provided."
    }

    Write-Host "Keystore not found. Restoring from $ReleaseKeystoreAscPath ..."
    & gpg -d --batch --yes --passphrase "$ReleaseKeystorePassphrase" --output "$keystorePath" "$releaseKeystoreAscFullPath"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to restore keystore from $ReleaseKeystoreAscPath"
    }
}

if (-not (Test-Path $keystorePath)) {
    throw "Keystore not found: $keystorePath (or provide '$ReleaseKeystoreAscPath' + passphrase to restore it)."
}
if (-not $SigningStorePassword -or -not $SigningKeyAlias -or -not $SigningKeyPassword) {
    throw "Missing signing values. Provide signingStorePassword, signingKeyAlias, and signingKeyPassword via args, env vars, or local.properties."
}

Write-Host "Building signed fDroid release APK (upstream workflow-compatible)..."
$env:JAVA_TOOL_OPTIONS = "-Djava.net.preferIPv4Stack=true"

# Match release.yml: no random applicationId (see nightly-obfuscation.yml for -PrandomizeAppId=true).
# Prefer :smartautoclicker:clean: root `clean` often fails on Windows (locked jars); full clean also does not undo obfuscation edits under src/.
$gradleArgs = @(
    "--info"
    "-PrandomizeAppId=false"
    "-PsigningStorePassword=$SigningStorePassword"
    "-PsigningKeyAlias=$SigningKeyAlias"
    "-PsigningKeyPassword=$SigningKeyPassword"
)
if (-not $SkipClean) {
    Write-Host "Running :smartautoclicker:clean (use -SkipClean to skip; if a nightly obfuscation build moved sources, git restore first)..."
    & "$rootDir/gradlew.bat" ":smartautoclicker:clean"
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "smartautoclicker:clean failed; continuing with --rerun-tasks to avoid stale outputs."
        $gradleArgs += "--rerun-tasks"
    }
}

& "$rootDir/gradlew.bat" @gradleArgs "assembleFDroidRelease" "bundleFDroidRelease"

if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path $apkOutputPath)) {
    throw "APK output not found: $apkOutputPath"
}

Copy-Item -Path $apkOutputPath -Destination $finalApkPath -Force
(Get-Item -LiteralPath $finalApkPath).LastWriteTime = Get-Date
Write-Host "Done: $finalApkPath"
