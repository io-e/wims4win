#! /bin/sh
# this script get user by class and level each week and write in wimshome/log/stat/stat.class and wimshome/log/class/stat.user
# format is date K3 E1 E2 E3 E4 E5 E6 H1 H2 H3 H4 H5 H6 U1 U2 U3 U4 U5 G


# Home directory of wims.
cd `dirname $0`/..
WIMS_HOME=`pwd`

today=`date '+%Y%m%d'`
weekday=`date '+%w'`
mkdir -p $WIMS_HOME/log/stat;
#if [ $weekday -ne 1 -a -e $WIMS_HOME/log/stat/today ]; then exit; fi;

if [ -e $WIMS_HOME/log/stat/server.geoloc ]; then
  servertown=`cut -d, -f1 $WIMS_HOME/log/stat/server.geoloc`;
  servergeoloc=`cut -d, -f2-3 $WIMS_HOME/log/stat/server.geoloc`;
else
  servertown=""
  servergeoloc=""
fi;

rm -f $WIMS_HOME/log/stat/today;

cd $WIMS_HOME/log/classes
cpt=0
for cls in [0-9]*
do
  if [ $cls -gt 9999 ]; then
    level=`awk -F'=' '$1=="!set class_level" {print $2; exit}' $cls/.def`;
    if [ "$level" != "" ]; then
      nuser=`cat $cls/.userlist | grep -c '^:'`
      # class with less than 5 users not count
      if [ $nuser -ge 5 ]; then
        location=`awk -F'=' '$1=="!set class_location" {print $2; exit}' $cls/.def`;
        geoloc=`awk -F'=' '$1=="!set class_geoloc" {print $2; exit}' $cls/.def`;
        echo "$cls,$level,$nuser,$location,$geoloc" >> $WIMS_HOME/log/stat/today;
        if [ "$geoloc" != "" -a "$geoloc"!="$servergeoloc" ]; then
          echo "$location,$geoloc" >> $WIMS_HOME/log/stat/location.tmp;
        else
          cpt=`expr $cpt + 1`
        fi;
      fi;
    fi;
  fi;
done;
c=`awk -F ',' 'BEGIN {  t["K3"]=0
  t["E1"]=0
  t["E2"]=0
  t["E3"]=0
  t["E4"]=0
  t["E5"]=0
  t["E6"]=0
  t["H1"]=0
  t["H2"]=0
  t["H3"]=0
  t["H4"]=0
  t["H5"]=0
  t["H6"]=0
  t["U1"]=0
  t["U2"]=0
  t["U3"]=0
  t["U4"]=0
  t["U5"]=0
  t["G"]=0} {if ($2 in t) t[$2]++}
  END {print t["K3"],t["E1"],t["E2"],t["E3"],t["E4"],t["E5"],t["E6"],t["H1"],t["H2"],t["H3"],t["H4"],t["H5"],t["H6"],t["U1"],t["U2"],t["U3"],t["U4"],t["U5"],t["G"]}' $WIMS_HOME/log/stat/today;`
old=`tail -n 1 $WIMS_HOME/log/stat/stat.class | cut -d" " -f2-20`;
if [ "$old" != "$c" ]; then
  echo ":$today $c" >> $WIMS_HOME/log/stat/stat.class;
fi;
c=`awk -F ',' 'BEGIN {t["K3"]=0
  t["E1"]=0
  t["E2"]=0
  t["E3"]=0
  t["E4"]=0
  t["E5"]=0
  t["E6"]=0
  t["H1"]=0
  t["H2"]=0
  t["H3"]=0
  t["H4"]=0
  t["H5"]=0
  t["H6"]=0
  t["U1"]=0
  t["U2"]=0
  t["U3"]=0
  t["U4"]=0
  t["U5"]=0
  t["G"]=0} {if ($2 in t) t[$2]+=$3}
  END {print t["K3"],t["E1"],t["E2"],t["E3"],t["E4"],t["E5"],t["E6"],t["H1"],t["H2"],t["H3"],t["H4"],t["H5"],t["H6"],t["U1"],t["U2"],t["U3"],t["U4"],t["U5"],t["G"]}' $WIMS_HOME/log/stat/today;`
old=`tail -n 1 $WIMS_HOME/log/stat/stat.user | cut -d" " -f2-20`;
if [ "$old" != "$c" ]; then
  echo ":$today $c" >> $WIMS_HOME/log/stat/stat.user;
fi;

# --------------- make geolocalisation of classe index
cd $WIMS_HOME/log/stat;
rm -f class.geoloc
if [ -e location.tmp ]; then
  # group identical town and geoloc
  sort location.tmp | uniq -c -ii | sed "s/^\ *//g" | awk -F" " '{ORS=" "
  print $1","
  for(i=2;i<=NF;i++) print $i
  ORS="\n"
  print ""}' | awk -F"," '{print ":"$2","$1","$3","$4}' > class.geoloc;
  rm -f location.tmp;
fi;
# every class not geolocalized are localized in the server town localization (if define).
if [ "$servergeoloc" != "" -a $cpt -gt 0 ]; then
  echo ":$servertown,$cpt,$servergeoloc" >> class.geoloc;
fi;
