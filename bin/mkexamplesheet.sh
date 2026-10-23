#! /bin/sh
#
# script to look at each example class and copy sheet in public_html/bases/sheet/$class_lang/$class_sheetadress/$class_number/
# to be execute at before each realize of a wims package.
#
# authors : F. Guerimand, bpr
# mail : fwguerima@free.fr
# this script is launch by mkindex. Do not use it directly
#

## level, lang, supervisor, email are those of the class
## if information is in the file .sheet$i, take it
## if not, take the information in the file .sheets
## if not, make something by default

whome="$1"
if [ -z "$whome" ]; then
 echo "don't use this script directly. execute mkindex";
 exit
fi;
target="$whome/public_html/bases/sheet";
mkdir -p $target;
# delete already (case of level changes)
cd $target;
oldlist=`ls -d */[EKHU][1-6]/[1-9][1-9][1-9][1-9]`;
rm -r $oldlist;
cd $whome/log/classes/;
clist=`ls -d [0-9]*`;
for cls in $clist; do
 if [ $cls -lt 9000 ]; then
  level=`awk -F'=' '$1=="!set class_level" {print $2; exit}' ./$cls/.def`;
  slevel=`awk -F'=' '$1=="!set class_slevel" {print $2; exit}' ./$cls/.def`;
  lang=`awk -F'=' '$1=="!set class_lang" {print $2; exit}' ./$cls/.def`;
  sup=`awk -F'=' '$1=="!set class_supervisor" {print $2; exit}' ./$cls/.def`;
  email=`awk -F'=' '$1=="!set class_email" {print $2; exit}' ./$cls/.def`;
  domain=`awk -F'=' '$1=="!set class_domain" {print $2; exit}' ./$cls/.def`;
  sheetadress=`awk -F'=' '$1=="!set class_sheetadress" {print $2; exit}' ./$cls/.def`;
  if [ -n "$sheetadress" -a -n "$lang" ]; then
   cd $cls/sheets/;
  # echo "--------------- $cls ; $level ; $lang"
   slist=`ls .sheet[0-9]*`;
   tmptarget="$target/$lang/$sheetadress/$cls";
   mkdir -p $tmptarget;
   for sh in $slist; do
    num=${sh##.sheet};
    # test if statut of sheet is non 0
    statut=`awk -v no="$num" 'BEGIN{cpt=0;} {a=substr($0,1,1); if(a==":")cpt++; if(cpt==no){print($0);exit;}}' .sheets`;
    if [ "$statut" != ":0" ]; then
     entete=`awk '{a=substr($0,1,1); if(a==":")exit; else print($0);}' $sh`;
     namesh=${sh##.};
     if [ -z "`grep -r :classes/ $sh`" ]; then
      if [ -n "$entete" ]; then
        #First case : information is in file .sheet$i : keep it (but not title and desc)
        # echo "$sh : direct copy";
        title=`awk -v no="$num" 'BEGIN{cpt=0;l=0;} {a=substr($0,1,1); if(a==":")cpt++; if(cpt==no){l++;if(l==3){print($0);exit;}} }' .sheets`;
        desc=`awk -v no="$num" 'BEGIN{cpt=0;l=0;} {a=substr($0,1,1); if(a==":")cpt++; if(cpt==no){l++;if(l==4){print($0);exit;}} }' .sheets`;
        echo "$title
$desc" >$tmptarget/$namesh.def;
        awk 'FNR>2{print $0}' $sh >>$tmptarget/$namesh.def;
      else
        #second case : information not in file .sheet$i : make it with files .sheets and .def.
        dt=`perl -e 'for (@ARGV) {@S=stat($_); print scalar(localtime($S[9]))}' $sh`
        dt=${dt##* };
        title=`awk -v no="$num" 'BEGIN{cpt=0;l=0;} {a=substr($0,1,1); if(a==":")cpt++; if(cpt==no){l++;if(l==3){print($0);exit;}} }' .sheets`;
        desc=`awk -v no="$num" 'BEGIN{cpt=0;l=0;} {a=substr($0,1,1); if(a==":")cpt++; if(cpt==no){l++;if(l==4){print($0);exit;}} }' .sheets`;
        keyword=`awk -v no="$num" 'BEGIN{cpt=0;l=0;} {a=substr($0,1,1); if(a==":")cpt++; if(cpt==no){l++;if(l==6){print($0);exit;}} }' .sheets`;
        domain1=`awk -v no="$num" 'BEGIN{cpt=0;l=0;} {a=substr($0,1,1); if(a==":")cpt++; if(cpt==no){l++;if(l==5){print($0);exit;}} }' .sheets`;
        ## if keyword is empty, take the level (only for H)
        if [ ! -n "$keyword" ]; then
          if [ -n "$slevel" ]; then
           keyword=`echo "$slevel"`;
          else
           keyword=`echo "level$level"`;
          fi ;
        fi;
        # echo "$sh : make presentation"; leave the empty line after email for description
        # of the sheet in the search engine
        echo "$title
$desc
??,$dt
2
$level
$domain, $domain1
$keyword
$sup
$email

" > $tmptarget/$namesh.def;
        cat $sh >> $tmptarget/$namesh.def;
      fi;
     fi;
     # else
     #  echo "$sh : EMPTY sheet";
    fi;
   done;
   cd ../..;
 # else
 # echo "-----------------------NOTHING ---------- $cls ; $level ; $lang";
  fi;
 fi;
done;
