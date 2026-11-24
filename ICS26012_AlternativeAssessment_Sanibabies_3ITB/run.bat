@echo off
rem Compiles the simulator and starts it. Double-click this file or run it from a terminal.
cd /d "%~dp0"

javac -d out src\*.java src\model\*.java src\scheduler\*.java src\gui\*.java src\prioritynp\*.java src\multilevelqueue\*.java src\clook\*.java src\sstf\*.java
if errorlevel 1 (
    echo.
    echo Compile failed. See the errors above.
    pause
    exit /b 1
)

rem javaw starts the app without keeping this console window open.
start "" javaw -cp out Main