@echo off

set ARGS=

:setupArgs
if ""%1""=="""" goto doneStart
set ARGS=%ARGS% "%1"
shift
goto setupArgs

:doneStart

java -server -Xss512M -Xmx16G -cp lib\graph.jar;lib\spinja.jar;lib\ndfs.jar driver.Main %ARGS%
