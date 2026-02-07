@echo off
REM MongoDB Setup and Verification Script for Windows
REM This script helps verify and fix MongoDB authentication issues

echo.
echo ========================================
echo MongoDB Authentication Setup
echo ========================================
echo.

REM Check if mongosh is available
echo [Step 1] Checking MongoDB installation...
mongosh --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ERROR: mongosh (MongoDB Shell) not found in PATH
    echo Please install MongoDB and add it to your system PATH
    echo Download from: https://www.mongodb.com/try/download/shell
    pause
    exit /b 1
)
echo ✓ MongoDB Shell found

REM Display MongoDB service status
echo.
echo [Step 2] Checking MongoDB Service Status...
sc query MongoDB >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=3" %%A in ('sc query MongoDB ^| findstr "STATE"') do (
        echo Service Status: %%A
    )
) else (
    echo Note: MongoDB service not found as Windows service
)

REM Get MongoDB running status
echo.
echo [Step 3] Testing MongoDB Connection (without auth)...
mongosh --eval "print('Connected to MongoDB successfully')" 2>nul
if %errorlevel% neq 0 (
    echo.
    echo WARNING: Cannot connect to MongoDB at localhost:27017
    echo Make sure MongoDB is running
    echo You can start it with: mongod
    pause
)

REM Main setup script
echo.
echo [Step 4] Setting up authentication...
echo.

REM Create a JavaScript file for MongoDB setup
(
    echo // Create admin user if not exists
    echo try {
    echo   db.createUser({
    echo     user: "admin",
    echo     pwd: "adminpassword",
    echo     roles: ["root"]
    echo   });
    echo   print("✓ Admin user created successfully");
    echo } catch(e) {
    echo   if (e.code === 51003 ^| ^| e.code === 11000) {
    echo     print("✓ Admin user already exists");
    echo   } else {
    echo     print("Error creating admin user: " + e);
    echo   }
    echo }
    echo.
    echo // Switch to admin database and verify
    echo db = db.getSiblingDB("admin");
    echo print("Switched to admin database");
    echo.
    echo // Create application user
    echo try {
    echo   db.createUser({
    echo     user: "carlevuser",
    echo     pwd: "carlevpassword",
    echo     roles: [
    echo       { role: "readWrite", db: "carlevdb" }
    echo     ]
    echo   });
    echo   print("✓ Application user 'carlevuser' created successfully");
    echo } catch(e) {
    echo   if (e.code === 51003 ^| ^| e.code === 11000) {
    echo     print("✓ Application user 'carlevuser' already exists");
    echo   } else {
    echo     print("Error creating application user: " + e);
    echo   }
    echo }
    echo.
    echo // Verify the user
    echo var users = db.getUsers();
    echo print("Current users in admin database: " + users.length);
    echo for(var i = 0; i ^< users.length; i++) {
    echo   print("  - " + users[i].user);
    echo }
    echo.
    echo print("Setup completed!");
) > "%TEMP%\mongodb-setup.js"

REM Run the setup script
echo Running MongoDB setup script...
mongosh --file "%TEMP%\mongodb-setup.js"

echo.
echo ========================================
echo [Step 5] Verifying Authentication
echo ========================================
echo.

REM Verify connection with credentials
(
    echo db = db.getSiblingDB("admin");
    echo try {
    echo   db.auth("carlevuser", "carlevpassword");
    echo   print("✓ Successfully authenticated as carlevuser");
    echo } catch(e) {
    echo   print("✗ Authentication failed: " + e);
    echo }
) > "%TEMP%\mongodb-verify.js"

echo Testing authentication with carlevuser...
mongosh --file "%TEMP%\mongodb-verify.js"

echo.
echo ========================================
echo Cleanup temporary files...
del "%TEMP%\mongodb-setup.js" 2>nul
del "%TEMP%\mongodb-verify.js" 2>nul

echo.
echo ========================================
echo MongoDB Setup Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Verify application.properties has:
echo    spring.data.mongodb.uri=mongodb://carlevuser:carlevpassword@localhost:27017/carlevdb?authSource=admin
echo.
echo 2. Restart your Spring Boot application
echo.
echo 3. Test the API again
echo.

pause
