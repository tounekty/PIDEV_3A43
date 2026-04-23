@echo off
REM Lancer l'application de calendrier à deux mois
REM Ce script compile et exécute l'application de démonstration

echo ========================================
echo 📅 Calendrier à Deux Mois - Démonstration
echo ========================================
echo.

REM Vérifier si Maven est installé
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur: Maven n'est pas installé ou n'est pas dans le PATH
    echo Veuillez installer Maven ou ajouter son répertoire au PATH
    pause
    exit /b 1
)

echo ✓ Maven trouvé
echo.

REM Compiler le projet
echo 🔨 Compilation du projet...
call mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation
    pause
    exit /b 1
)
echo ✓ Compilation réussie
echo.

REM Créer le classpath avec les dépendances
echo 📦 Préparation des dépendances...
call mvn dependency:copy-dependencies -DoutputDirectory=target/dependencies -q

REM Construire le classpath
setlocal enabledelayedexpansion
set "CLASSPATH=target/classes"
for /r "target/dependencies" %%f in (*.jar) do (
    set "CLASSPATH=!CLASSPATH!;%%f"
)

echo ✓ Dépendances prêtes
echo.

REM Exécuter l'application
echo 🚀 Lancement de l'application...
echo.
java -cp "%CLASSPATH%" org.example.util.DualMonthCalendarApp

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Erreur lors de l'exécution de l'application
    pause
    exit /b 1
)

echo.
echo ✓ Application terminée
pause
