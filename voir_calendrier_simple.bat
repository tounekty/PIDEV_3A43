@echo off
echo ========================================
echo Visualisation du Calendrier Interactif
echo SANS modifications du code existant
echo ========================================
echo.

echo Options disponibles :
echo.
echo 1. Voir le calendrier SEUL (recommandé)
echo    - Juste le calendrier interactif
echo    - Pas de modifications FXML
echo    - Codes couleur et navigation
echo.
echo 2. Voir le calendrier + FXML original
echo    - Calendrier en haut
echo    - FXML original en bas
echo    - Comparaison côte à côte
echo.
echo 3. Application de démonstration complète
echo    - Toutes les fonctionnalités
echo    - Onglets multiples
echo    - Exemples d'utilisation
echo.

set /p choice="Choisissez une option (1, 2 ou 3) : "

if "%choice%"=="1" (
    echo.
    echo Lancement : Calendrier seul...
    call mvn javafx:run -DmainClass=org.example.util.JustCalendarView
) else if "%choice%"=="2" (
    echo.
    echo Lancement : Calendrier + FXML original...
    call mvn javafx:run -DmainClass=org.example.util.ViewCalendarOnly
) else if "%choice%"=="3" (
    echo.
    echo Lancement : Démonstration complète...
    call mvn javafx:run -DmainClass=org.example.util.CalendarExampleApp
) else (
    echo.
    echo Choix invalide. Lancement par défaut du calendrier seul...
    call mvn javafx:run -DmainClass=org.example.util.JustCalendarView
)

echo.
echo ========================================
echo Application terminée.
echo ========================================
echo.
pause