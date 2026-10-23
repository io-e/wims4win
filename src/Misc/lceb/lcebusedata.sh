#!/bin/sh

if [ $# -ne 2 ]
then
	echo 'Wrong number of arguments. Specify the executable and the data file to use.'
	exit
fi

cat $2 | xargs --max-lines=1 ./$1
