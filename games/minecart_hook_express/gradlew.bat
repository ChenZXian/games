@ECHO OFF
SET DIR=%~dp0
SET JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
IF NOT EXIST "%JAR%" (
  ECHO Missing gradle-wrapper.jar
  EXIT /B 1
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
