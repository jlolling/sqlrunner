 @echo off
 setLocal EnableDelayedExpansion
 set CLASSPATH="
 for /R . %%a in (*.jar) do (
   set CLASSPATH=!CLASSPATH!;%%a
 )
 set CLASSPATH=!CLASSPATH!"
 start javaw -cp %CLASSPATH%  sqlrunner.Main
