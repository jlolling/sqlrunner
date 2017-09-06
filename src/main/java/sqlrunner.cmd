 @echo off
 setLocal EnableDelayedExpansion
 set CLASSPATH="
 for /R . %%a in (*.jar) do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
 set CLASSPATH=!CLASSPATH!"
 start javaw -Xmx8192m -cp %CLASSPATH%  sqlrunner.Main
