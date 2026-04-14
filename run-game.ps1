param(
    [string]$JavaFxLib
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

function Resolve-JavaFxLib {
    param([string]$Provided)

    if ($Provided -and (Test-Path $Provided)) {
        return (Resolve-Path $Provided).Path
    }

    if ($env:JAVAFX_LIB -and (Test-Path $env:JAVAFX_LIB)) {
        return (Resolve-Path $env:JAVAFX_LIB).Path
    }

    if ($env:JAVAFX_HOME) {
        $fromHome = Join-Path $env:JAVAFX_HOME 'lib'
        if (Test-Path $fromHome) {
            return (Resolve-Path $fromHome).Path
        }
    }

    $candidates = @(
        'C:\javafx-sdk-21.0.2\lib',
        'C:\javafx-sdk-21.0.8\lib',
        'C:\Program Files\javafx-sdk-21.0.2\lib',
        'C:\Program Files\javafx-sdk-21.0.8\lib'
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    return $null
}

$javaFxLibPath = Resolve-JavaFxLib -Provided $JavaFxLib

if (-not $javaFxLibPath) {
    Write-Error "JavaFX introuvable. Fournis un chemin: .\run-game.ps1 -JavaFxLib 'C:\path\to\javafx-sdk\lib'"
}

$modules = 'javafx.controls,javafx.fxml,javafx.media'

Write-Host "[run-game] JavaFX lib: $javaFxLibPath"
Write-Host '[run-game] Compilation...'
javac --module-path "$javaFxLibPath" --add-modules $modules -d out src\*.java
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host '[run-game] Lancement du jeu...'
java --module-path "$javaFxLibPath" --add-modules $modules -cp out GameApp
exit $LASTEXITCODE
