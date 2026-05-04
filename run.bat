@echo off
REM ==========================================
REM Mindcare Project Runner - Setup & Execute
REM ==========================================
setlocal enabledelayedexpansion

echo.
echo ==========================================
echo   PIDEV_3A43 - Mindcare Project
echo ==========================================
echo.

REM Check for Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found. Please install JDK 17 or higher.
    pause
    exit /b 1
)

echo [OK] Java found
java -version 2>&1

REM Check for Maven
mvn -version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Maven found
    call mvn -version 2>&1 | find "Apache Maven" | findstr /R ".*"
    goto :maven_found
)

echo.
echo [INFO] Maven not found. Checking for Maven in user profile...
if exist "%USERPROFILE%\.m2\apache-maven\bin\mvn.cmd" (
    set "PATH=%USERPROFILE%\.m2\apache-maven\bin;%PATH%"
    echo [OK] Maven found in user profile
    goto :maven_found
)

echo.
echo [INFO] Downloading Maven 3.9.6...

REM Create .m2 directory if it doesn't exist
if not exist "%USERPROFILE%\.m2" mkdir "%USERPROFILE%\.m2"

REM Download Maven
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip', '%USERPROFILE%\.m2\maven.zip')}"

if errorlevel 1 (
    echo ERROR: Failed to download Maven
    pause
    exit /b 1
)

echo [OK] Maven downloaded

REM Extract Maven
echo [INFO] Extracting Maven...
powershell -Command "Expand-Archive -Path '%USERPROFILE%\.m2\maven.zip' -DestinationPath '%USERPROFILE%\.m2'"

if errorlevel 1 (
    echo ERROR: Failed to extract Maven
    pause
    exit /b 1
)

REM Rename extracted directory
for /d %%d in ("%USERPROFILE%\.m2\apache-maven*") do (
    if not "%%~nxd"=="apache-maven" (
        ren "%%d" "apache-maven"
    )
)

REM Delete zip file
del "%USERPROFILE%\.m2\maven.zip"

set "PATH=%USERPROFILE%\.m2\apache-maven\bin;%PATH%"
echo [OK] Maven extracted and ready

:maven_found
echo.
echo [INFO] Building project...
echo.

call mvn clean compile 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo [OK] Build successful!
echo.
echo [INFO] Starting application...
echo.

call mvn javafx:run

if errorlevel 1 (
    echo.
    echo ERROR: Application failed to start
    pause
    exit /b 1
)

echo.
echo [OK] Application closed successfully
pause
exit /b 0
