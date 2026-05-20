@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set GRADLE_VERSION=9.2.0
set GRADLE_DIR=%USERPROFILE%\.supsvisuals\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_DIR%\bin\gradle.bat

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Java not found. Install JDK 21 first.
  echo IntelliJ: File ^> Project Structure ^> SDK ^> Add SDK ^> Download JDK ^> 21
  pause
  exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%v

echo Using Java: %JAVA_VERSION%

if exist "%GRADLE_BIN%" (
  echo Using local Gradle %GRADLE_VERSION%...
  if "%~1"=="" (
    "%GRADLE_BIN%" build
  ) else (
    "%GRADLE_BIN%" %*
  )
  pause
  exit /b %errorlevel%
)

echo Gradle not found. Downloading Gradle %GRADLE_VERSION%...
mkdir "%USERPROFILE%\.vibevisuals" 2>nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%USERPROFILE%\.supsvisuals\gradle-%GRADLE_VERSION%-bin.zip'"
if errorlevel 1 (
  echo [ERROR] Could not download Gradle. Check internet connection.
  pause
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%USERPROFILE%\.supsvisuals\gradle-%GRADLE_VERSION%-bin.zip' '%USERPROFILE%\.supsvisuals'"
if errorlevel 1 (
  echo [ERROR] Could not unpack Gradle.
  pause
  exit /b 1
)

if "%~1"=="" (
  "%GRADLE_BIN%" build
) else (
  "%GRADLE_BIN%" %*
)
pause
exit /b %errorlevel%
