@ECHO OFF
SET DIR=%~dp0
IF "%DIR%"=="" SET DIR=.
SET APP_HOME=%DIR%
java -Dorg.gradle.appname=gradlew -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
