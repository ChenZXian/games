@ECHO OFF
SET DIRNAME=%~dp0
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIRNAME%
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

IF "%JAVA_HOME%"=="" GOTO findJavaFromPath
SET _JAVA_HOME=%JAVA_HOME:"=%
SET JAVA_EXE=%_JAVA_HOME%\bin\java.exe
IF EXIST "%JAVA_EXE%" GOTO execute

ECHO WARN: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
GOTO findJavaFromPath

:findJavaFromPath
SET JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
IF "%ERRORLEVEL%" == "0" GOTO execute

ECHO ERROR: JAVA_HOME is invalid and no 'java' command could be found in your PATH.
GOTO fail

:execute
"%JAVA_EXE%" -Dorg.gradle.appname=%APP_BASE_NAME% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:fail
EXIT /B 1
