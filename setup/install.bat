@ECHO OFF
javac >NUL 2>NUL
IF ERRORLEVEL 9009 (
	IF "%JAVA_HOME%" == "%%JAVA_HOME%%" (
		ECHO javac not found, set JAVA_HOME to your JDK root
		EXIT /B
	) ELSE (
		"%JAVA_HOME%\bin\javac.exe" >NUL 2>NUL
		IF ERRORLEVEL 9009 (
			ECHO javac not found, set JAVA_HOME to your JDK root
			EXIT /B
		) ELSE (
			SET JAVAC="%JAVA_HOME%\bin\javac.exe"
			SET JAVA="%JAVA_HOME%\bin\java.exe"
		)
	)
) ELSE (
	SET JAVAC=javac
	SET JAVA=java
)

REM ECHO javac command: %JAVAC%

%JAVA% 2>NUL >NUL

IF ERRORLEVEL 9009 (
	ECHO java.exe not found in the same place as javac.exe
	ECHO javac command: %JAVAC%
	ECHO java command: %JAVA%
	EXIT /B
)

%JAVAC% Installer.java
IF %ERRORLEVEL% == 0 (
	%JAVA% Installer windows
)