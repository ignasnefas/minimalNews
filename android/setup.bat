@rem Gradle wrapper bootstrap script for MiniDash Android
@rem Downloads Gradle and generates the wrapper files

@echo off
setlocal

set GRADLE_VERSION=8.4
set GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip
set TEMP_DIR=%TEMP%\gradle-bootstrap
set GRADLE_DIR=%TEMP_DIR%\gradle-%GRADLE_VERSION%

echo [MiniDash] Bootstrapping Gradle wrapper...

if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"

if not exist "%GRADLE_DIR%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -Command "Invoke-WebRequest -Uri '%GRADLE_URL%' -OutFile '%TEMP_DIR%\gradle.zip'"
    echo Extracting...
    powershell -Command "Expand-Archive -Path '%TEMP_DIR%\gradle.zip' -DestinationPath '%TEMP_DIR%' -Force"
)

echo Generating Gradle wrapper...
"%GRADLE_DIR%\bin\gradle.bat" wrapper --gradle-version %GRADLE_VERSION%

echo.
echo Done! You can now open this project in Android Studio or run:
echo   gradlew.bat assembleDebug
echo.

endlocal
