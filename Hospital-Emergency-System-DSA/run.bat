@echo off
if not exist bin mkdir bin
echo Compiling Hospital Emergency Management System...
javac -d bin src\model\*.java src\dsa\*.java src\db\*.java src\server\*.java src\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed. Make sure JDK is installed.
    pause
    exit /b %errorlevel%
)
echo Starting MedCare Emergency Portal Server...
java -cp "bin;lib\*" Main
pause
