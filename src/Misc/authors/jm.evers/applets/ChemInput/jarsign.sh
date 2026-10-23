#!/bin/sh

# used for signing home_compiled new java-jars

echo "Give a name for the keystore 
no spaces or strange charactera allowed !
"

read myname
mykey=".keystore"
myname="Schaersvoorde"
jar="ChemInput.jar"


if [ ! -e $mykey ] ; then
    keytool -genkey -alias $myname
else
    echo ".keystore file exists"
fi

if [ ! -e $myname.x509 ] ; then
    keytool -export -alias $myname -rfc -file $myname.x509
else
    echo "$myname.x509 file exists"
fi
    
jarsigner $jar $myname
