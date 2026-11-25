#!/usr/bin/env bash
set -e
mkdir -p bin
echo "Compiling Hospital Emergency Management System..."
javac -d bin src/model/*.java src/dsa/*.java src/db/*.java src/server/*.java src/Main.java
echo "Starting MedCare Emergency Portal Server..."
java -cp "bin:lib/*" Main
