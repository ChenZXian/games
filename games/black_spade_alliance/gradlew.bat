@echo off
set DIR=%~dp0
set JAVA_CMD=D:\Java\jdk-21\bin\java.exe
"%JAVA_CMD%" -Dorg.gradle.appname=gradlew -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
