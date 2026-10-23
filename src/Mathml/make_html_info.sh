#!/bin/sh

htmlfile="wims_mathml_info.html"

idx=`date +%s`
datum=`date +%m/%Y`
listfile="/tmp/list.$idx"
if [ -e wims_mathml.l ] ; then
echo "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">
<html>
 <head>
  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">
  <title>Latex supported in wims_mathml</title>
 </head>
 <style>
 li{display:inline-block;}
 div { display: none;}
 a:active + div { display: block;background-color:yellow;}
 </style>
 <body>
  <ul>" > $htmlfile
cat wims_mathml.l | grep "^\"" | awk -F\" '{if($2 != "<" && length($2)>5) print $2}' | tr '&{([]})#\' ' ' | sort -duif> $listfile
while read line ; do

 chk=`echo $line | wc -c | tr -d '[:blank:]'`
 if [ $chk -gt 1 ] ; then
  math=`./wims_mathml --tex-size "150%" --tex-string "\\\\$line"`
  chk=`echo $math | grep "&" | wc -l | tr -d '[:blank:]'`
  if [ "$math" = "\$ERROR" ] || [ "$math" = "ERROR" ] || [ "$chk" = "0" ] ; then
   echo "<li><a>\\\\$line</a><div>see latex syntax</div></li>" >> $htmlfile
  else
   echo "<li><a>$math</a><div>\\$line</div></li>" >> $htmlfile
  fi
 fi
done < "$listfile"

rm $listfile
echo "   </li>
  </ul>
  <script>

  </script>
 </body>
</html>
">> $htmlfile
else
 place=`pwd`
 echo "
 no file called \"wims_mathml.l\" in this script directory $place ! "
 read whatever
fi

exit
