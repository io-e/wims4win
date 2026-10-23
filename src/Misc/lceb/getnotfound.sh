#!/bin/sh

if [ $# -ne 1 ]
then
	echo 'Wrong number of arguments. Specify the executable to use.'
	exit
fi

while
	read input
do
	if [ `./$1 $input | grep "NOTFOUND" | wc -l` -eq 1 ]
	then
		echo "$input"
	fi
done
