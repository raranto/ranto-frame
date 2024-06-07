@echo off
setlocal EnableDelayedExpansion

rem Chemin du dossier source :
set "sourceDir=D:\Fianarana\Semestre4\Mr Vahatriniaina\Framework\build\classes"

rem Chemin du dossier de destination :
set "destDir=D:\Fianarana\Semestre4\Mr Vahatriniaina\Framework_ITU_Test\lib"

rem Nom du fichier JAR :
set "jarName=Framework.jar"

rem Commande pour créer le fichier JAR :
"C:\Program Files\Java\jdk-19\bin\jar.exe" cvf "!destDir!\!jarName!" -C "!sourceDir!" .

rem Fin du script
echo Création du fichier JAR terminée.
echo Fichier JAR déplacé vers le dossier de destination.

pause
