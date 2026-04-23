@echo off
echo ========================================
echo Lancement du Calendrier Interactif
echo ========================================
echo.

echo 1. Compilation du projet...
call mvn compile -f pom.xml

if %errorlevel% neq 0 (
    echo.
    echo ❌ Erreur de compilation !
    echo Vérifiez les messages d'erreur ci-dessus.
    pause
    exit /b 1
)

echo.
echo ✅ Compilation réussie !
echo.

echo 2. Lancement de l'application de test...
echo    (Dialogue de réservation avec calendrier intégré)
echo.

call mvn javafx:run -DmainClass=org.example.util.TestCalendarInReservation

if %errorlevel% neq 0 (
    echo.
    echo ⚠️  L'application s'est arrêtée.
    echo.
)

echo.
echo ========================================
echo Autres options de lancement :
echo ========================================
echo.
echo Pour lancer l'application de démonstration complète :
echo   mvn javafx:run -DmainClass=org.example.util.CalendarExampleApp
echo.
echo Pour lancer l'exemple d'intégration simple :
echo   mvn javafx:run -DmainClass=org.example.util.CalendarIntegrationExample
echo.
pause