@echo off
set DIR=%~dp0
if exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
  exit /b %ERRORLEVEL%
)
echo Missing gradle\wrapper\gradle-wrapper.jar. Open with Android Studio or regenerate wrapper with local Gradle.
exit /b 1
