#! /bin/sh
base=`pwd`
classpath=$base
for file in `ls -1 $base/*.jar`
do
  classpath="$classpath:$file"
done

java -Xmx1024m -cp $classpath sqlrunner.Main $1
