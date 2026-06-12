@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo === MasyaLink clean force push ===
echo Current folder: %CD%
echo.

if not exist "app\" goto not_project
if not exist ".github\" goto not_project
if not exist "build.gradle.kts" goto not_project
if not exist "settings.gradle.kts" goto not_project

git --version >nul 2>nul
if errorlevel 1 goto no_git

REM Remove only local git binding from this exact folder.
if exist ".git\" (
  echo Removing local .git folder...
  rmdir /s /q ".git"
)

REM Prevent parent .git folders from being used.
for %%I in ("%CD%\..") do set "GIT_CEILING_DIRECTORIES=%%~fI"

git init .
git branch -M main
git remote add origin https://github.com/hjk290wer/MasyaLink.git

git add -A
git commit -m "Replace project with MasyaLink update"

echo.
echo Pushing to GitHub with --force...
git push --force origin main
if errorlevel 1 goto push_failed

echo.
echo SUCCESS: pushed to GitHub.
echo Open Actions: https://github.com/hjk290wer/MasyaLink/actions
echo.
pause
exit /b 0

:not_project
echo ERROR: This is not the project root.
echo Required files/folders in current folder:
echo   app\
echo   .github\
echo   build.gradle.kts
echo   settings.gradle.kts
echo.
pause
exit /b 1

:no_git
echo ERROR: Git is not installed or not available in PATH.
pause
exit /b 1

:push_failed
echo.
echo ERROR: Push failed.
echo Check internet connection, GitHub access, and Git authorization.
echo.
pause
exit /b 1
