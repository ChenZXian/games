@echo off
set DIR=%~dp0
set JAVA_EXEC=%JAVA_HOME%\bin\java.exe
if not exist "%JAVA_EXEC%" set JAVA_EXEC=java
"%JAVA_EXEC%" -classpath "%DIR%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
