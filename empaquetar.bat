@echo off
setlocal
title Empaquetar - Administrador de ventas

rem ====================================================================
rem  Crea la aplicacion de escritorio "Administrador de ventas" y su
rem  acceso directo (con el icono del carrito) en el Escritorio.
rem
rem  USO: doble clic sobre este archivo, con la carpeta del proyecto
rem       completa (la que contiene pom.xml). Necesita internet la
rem       primera vez para que Maven descargue las librerias.
rem
rem  OPCIONES (solo si la aplicacion no abre; se escriben despues del
rem  nombre del archivo, desde una ventana de simbolo del sistema):
rem     empaquetar.bat completo   -> incluye TODO Java (pesa mas, pero
rem                                  descarta que falte algun modulo)
rem     empaquetar.bat consola    -> la aplicacion abre con una ventana
rem                                  negra que muestra los errores
rem     empaquetar.bat completo consola   -> las dos a la vez
rem ====================================================================

rem Solo si Maven no se encuentra solo: escribe aqui la ruta completa de mvn.cmd, por ejemplo
rem set "MVN_MANUAL=C:\Program Files\NetBeans-16\netbeans\java\maven\bin\mvn.cmd"
set "MVN_MANUAL="

set "NOMBRE=Administrador de ventas"
set "VERSION=1.0.0"
set "JAR=sgis-app-%VERSION%.jar"
set "INSTALACION=%LOCALAPPDATA%\AdministradorDeVentas"

cd /d "%~dp0"
if not exist pom.xml goto :ERROR_CARPETA

echo.
echo [1/6] Buscando Java (JDK con jpackage)...
set "JPACKAGE="
for /d %%D in ("C:\Program Files\Java\jdk-19*") do if exist "%%D\bin\jpackage.exe" set "JPACKAGE=%%D\bin\jpackage.exe"
if not defined JPACKAGE for /f "delims=" %%P in ('where jpackage 2^>nul') do if not defined JPACKAGE set "JPACKAGE=%%P"
if not defined JPACKAGE goto :ERROR_JDK

for %%I in ("%JPACKAGE%") do set "JDK_BIN=%%~dpI"
for %%I in ("%JDK_BIN%..") do set "JAVA_HOME=%%~fI"
echo      Usando JDK en: %JAVA_HOME%

echo.
echo [2/6] Buscando Maven...
set "MVN="
where mvn >nul 2>nul && set "MVN=mvn"
if not defined MVN for /d %%D in ("C:\Program Files\NetBeans*") do if exist "%%D\netbeans\java\maven\bin\mvn.cmd" set "MVN=%%D\netbeans\java\maven\bin\mvn.cmd"
if not defined MVN if defined MVN_MANUAL set "MVN=%MVN_MANUAL%"
if not defined MVN goto :ERROR_MAVEN
echo      Maven encontrado.

echo.
echo [3/6] Compilando el proyecto (puede tardar unos minutos la primera vez)...
call "%MVN%" clean package -DskipTests
if errorlevel 1 goto :ERROR_COMPILAR
if not exist "target\%JAR%" goto :ERROR_COMPILAR
copy /y "target\%JAR%" "target\lib\" >nul

echo.
echo [4/6] Creando la aplicacion con jpackage...
if exist dist rmdir /s /q dist

set "MODULOS=java.base,java.desktop,java.sql,java.naming,java.logging,java.management,java.xml,java.prefs,java.security.sasl,jdk.unsupported,jdk.localedata,java.scripting,java.security.jgss,java.sql.rowset,java.datatransfer"
"%JDK_BIN%java.exe" --list-modules | findstr /b /c:"jdk.crypto.ec" >nul && set "MODULOS=%MODULOS%,jdk.crypto.ec"

set "COMPLETO="
set "OPCION_CONSOLA="
if /i "%~1"=="completo" set "COMPLETO=1"
if /i "%~2"=="completo" set "COMPLETO=1"
if /i "%~1"=="consola" set "OPCION_CONSOLA=--win-console"
if /i "%~2"=="consola" set "OPCION_CONSOLA=--win-console"

set OPCION_RUNTIME=--add-modules %MODULOS%
if defined COMPLETO set OPCION_RUNTIME=--runtime-image "%JAVA_HOME%"
if defined COMPLETO (echo      Modo: Java completo) else (echo      Modo: Java reducido)
if defined OPCION_CONSOLA echo      Con ventana de consola para ver errores

"%JPACKAGE%" --type app-image --name "%NOMBRE%" --input target\lib --main-jar %JAR% --main-class sgis.Main --icon icono\icono_carrito.ico --dest dist --app-version %VERSION% --vendor "Atlas Corporation" %OPCION_RUNTIME% %OPCION_CONSOLA%
if errorlevel 1 goto :ERROR_JPACKAGE
if not exist "dist\%NOMBRE%\%NOMBRE%.exe" goto :ERROR_JPACKAGE

echo.
echo [5/6] Instalando en: %INSTALACION%
if exist "%INSTALACION%" rmdir /s /q "%INSTALACION%"
xcopy "dist\%NOMBRE%" "%INSTALACION%\" /E /I /Y /Q >nul

echo.
echo [6/6] Creando el acceso directo en el Escritorio...
set "EXE=%INSTALACION%\%NOMBRE%.exe"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$w=New-Object -ComObject WScript.Shell; $s=$w.CreateShortcut([Environment]::GetFolderPath('Desktop')+'\%NOMBRE%.lnk'); $s.TargetPath='%EXE%'; $s.WorkingDirectory='%INSTALACION%'; $s.IconLocation='%EXE%'; $s.Description='%NOMBRE%'; $s.Save()"
if errorlevel 1 goto :ERROR_ACCESO

echo.
echo ====================================================================
echo  LISTO. Ya tienes "%NOMBRE%" en el Escritorio.
echo.
echo  Para usarla cada dia:
echo    1. Abre el Panel de Control de XAMPP y pulsa Start en MySQL
echo       (Apache NO hace falta, solo MySQL).
echo    2. Doble clic en "%NOMBRE%" del Escritorio.
echo.
echo  Si no abre, mira el archivo de registro:
echo    %USERPROFILE%\.sgis\aplicacion.log
echo  o vuelve a ejecutar este archivo con la opcion "completo" o "consola".
echo ====================================================================
pause
exit /b 0

:ERROR_CARPETA
echo.
echo ERROR: ejecuta este archivo desde la carpeta del proyecto (donde esta pom.xml).
pause
exit /b 1

:ERROR_JDK
echo.
echo ERROR: no se encontro jpackage. Instala un JDK 17 o superior (no solo el JRE)
echo        y vuelve a ejecutar este archivo.
pause
exit /b 1

:ERROR_MAVEN
echo.
echo ERROR: no se encontro Maven. Abre este archivo con el Bloc de notas y escribe, en la
echo        linea set "MVN_MANUAL=" del inicio, la ruta completa de mvn.cmd.
pause
exit /b 1

:ERROR_COMPILAR
echo.
echo ERROR: no se pudo compilar el proyecto. Revisa los mensajes de arriba.
pause
exit /b 1

:ERROR_JPACKAGE
echo.
echo ERROR: jpackage no pudo crear la aplicacion. Revisa los mensajes de arriba.
pause
exit /b 1

:ERROR_ACCESO
echo.
echo ERROR: la aplicacion se instalo en %INSTALACION%
echo        pero no se pudo crear el acceso directo. Crealo a mano desde esa carpeta.
pause
exit /b 1
