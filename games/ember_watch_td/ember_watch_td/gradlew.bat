@ECHO OFF
SET DIR=%~dp0
IF EXIST "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) ELSE (
  ECHO gradle-wrapper.jar is missing. Open the project in Android Studio or provide the wrapper jar.
  EXIT /B 1
)
