@echo off
title Mindcare Desktop - Launcher
echo ==========================================
echo   Mindcare Desktop App - CodeVeins Platform
echo ==========================================
echo.

:: Resolve Maven command in this order:
:: 1) project Maven wrapper
:: 2) local Maven in user profile (current and legacy locations)
:: 3) system PATH
set "MVN="

:: Resolve Java command (prefer local JDK install used by this project)
set "JAVA_CMD="

if exist "%USERPROFILE%\.jdks\temurin-17.0.18\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\temurin-17.0.18"
)

if not defined JAVA_HOME if exist "%USERPROFILE%\.jdks\jdk-17\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-17"
)

if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    set "Path=%JAVA_HOME%\bin;%Path%"
)

if not defined JAVA_CMD (
    for /f "delims=" %%i in ('where java.exe 2^>nul') do (
        set "JAVA_CMD=%%i"
        goto :java_found
    )
)

:java_found

if not exist "%JAVA_CMD%" (
    echo Java not found.
    echo.
    echo Install JDK 17 and ensure java.exe is available, or place it in:
    echo   - %%USERPROFILE%%\.jdks\temurin-17.0.18\bin\java.exe
    echo   - %%USERPROFILE%%\.jdks\jdk-17\bin\java.exe
    pause
    exit /b 1
)

if exist "%~dp0mvnw.cmd" (
    set "MVN=%~dp0mvnw.cmd"
)

if not defined MVN if exist "%USERPROFILE%\apache-maven\bin\mvn.cmd" (
    set "MVN=%USERPROFILE%\apache-maven\bin\mvn.cmd"
)

if not defined MVN if exist "%USERPROFILE%\apache-maven\apache-maven-3.9.6\bin\mvn.cmd" (
    set "MVN=%USERPROFILE%\apache-maven\apache-maven-3.9.6\bin\mvn.cmd"
)

if not defined MVN if exist "%USERPROFILE%\.maven\maven-3.9.14\bin\mvn.cmd" (
    set "MVN=%USERPROFILE%\.maven\maven-3.9.14\bin\mvn.cmd"
)

if not defined MVN (
    for /d %%D in ("%USERPROFILE%\.maven\maven-*") do (
        if exist "%%~fD\bin\mvn.cmd" (
            set "MVN=%%~fD\bin\mvn.cmd"
            goto :mvn_found
        )
    )
)

if not defined MVN (
    for /f "delims=" %%i in ('where mvn.cmd 2^>nul') do (
        set "MVN=%%i"
        goto :mvn_found
    )
)

:mvn_found

if not exist "%MVN%" (
    echo Maven not found.
    echo.
    echo Install Maven and add it to PATH, or place it in one of these locations:
    echo   - %~dp0mvnw.cmd
    echo   - %%USERPROFILE%%\.maven\maven-3.9.14\bin\mvn.cmd
    echo   - %%USERPROFILE%%\apache-maven\bin\mvn.cmd
    echo   - %%USERPROFILE%%\apache-maven\apache-maven-3.9.6\bin\mvn.cmd
    pause
    exit /b 1
)

set "Path=%~dp0;%Path%"

echo Starting Mindcare Desktop...
echo Using Java : %JAVA_CMD%
echo Using Maven: %MVN%
echo.

cd /d "%~dp0"
"%MVN%" javafx:run -f pom.xml

if errorlevel 1 (
    echo.
    echo ==========================================
    echo   Application exited with an error.
    echo ==========================================
    pause
)
