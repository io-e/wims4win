#! /bin/sh
# logs and referer of documents, user and class connections in wims/log/...
# Now must be called from wimslogd and housekeep.daily.

if [ "$site_accounting" != "yes" ]; then exit; fi

# working directory. Changeable.
cd `dirname $0`/..
wims_home=`pwd`
tmpdir=$wims_home/tmp/log
cd log

if [ -z "$1" ]; then workday=`date -d "22 hours ago" '+%Y%m%d'`; else workday=$1;fi

test=`grep $workday account/00access.act | awk '{print $1}'`
if [ ! -z "$test" ]; then echo Accounting already done for $workday.; exit; fi

# log date for using newccsum version  
if [ ! -f ccaccount/newccsum ]; then
  echo "$workday" > ccaccount/newccsum;
fi;

cat access.log* | grep $workday'\.' | grep -v 'devel/' | grep -v 'error' >$tmpdir/acctmp.log

cc=`grep -c . $tmpdir/acctmp.log`
cd=`cat $tmpdir/acctmp.log | awk '{print $3}' | sort | uniq | grep -c .`
cs=`cat session.log* | grep -c $workday`
if [ $cd -gt 0 ]; then
  avcc=`expr $cc / $cd`
  avcs=`expr $cs / $cd`
else
  avcc=0
  avcs=0
fi
echo "$workday	$cc	$cs	$cd	$avcc	$avcs" >>account/00access.act

ml=`cat $tmpdir/acctmp.log | awk 'length($5) < 39 {print $5}' | grep / | sort | uniq`

for i in $ml
do
 cc=`grep -c $i $tmpdir/acctmp.log`
 cd=`grep $i $tmpdir/acctmp.log | awk '{print $3}' | sort | uniq | grep -c .`
 j=`echo $i | tr '/' '@'`
 if [ $cd -gt 0 ]; then
   avcc=`expr $cc / $cd`
 else
   avcc=0
 fi
 echo "$workday	$cc	$cd	$avcc" >>account/$j
done

# Document accounting
mkdir -p account/doc
list=`cat modules/adm/doc.* |\
 awk '/^'$workday'\./ && $4=="read" {print $5" "$3}' |\
 grep -v '/' |\
 awk '$1<100000 {print}' | sort -n`
tot=`echo "$list" | awk '{print $1}' | sort -n | uniq`
for d in $tot
do
 cc=`echo "$list" | grep -c '^'$d`
 cd=`echo "$list" | grep '^'$d | awk '{print $2}' | sort | uniq | grep -c .`
 if [ $cd -gt 0 ]; then
  avcd=`expr $cc / $cd`
 else
  avcd=0
 fi
 echo "$workday	$cc	$cd	$avcd" >>account/doc/$d
done
cc=`echo "$list" | grep -c .`
cd=`echo "$list" | awk '{print $2}' | sort | uniq | grep -c .`
if [ $cd -gt 0 ]; then
  avcc=`expr $cc / $cd`
else
  avcc=0
fi
echo "$workday	$cc	$cd	$avcc" >>account/doc/0all

# clear unwanted module accounts
rm -f account/home account/adm@* account/devel@* account/classes@* account/help@* account/light >/dev/null 2>&1

# exit

# Now referer logs.

cat /tmp/referer-home.log | sort | uniq | grep 'http://' | grep -v 'wims\.unice\.fr' | sed 's!http://!!g' | awk -F '#' '{print $1}' >>referer.log
echo >/tmp/referer-home.log

cat referer.log* | grep $workday | awk '{print $3"      	"$4}' \
	| sort | uniq | sort -k 1 >$tmpdir/acctmp.log

grep -v '??' $tmpdir/acctmp.log >referer/refuniq.log
a=`grep -c '??' $tmpdir/acctmp.log`
echo "$workday	$a" >>referer/refauto.cnt

sed 's!?!/!g' referer/refuniq.log | awk -F/ '{print $1"/"$2}' > referer/refstem.log
awk -F/ '{print $1}' referer/refstem.log >referer/refsite.log

for s in stem uniq site
do
 cat referer/ref$s.log | tr '\?' ' ' | awk '{print $2}' | uniq -c | sort -nr \
 	| awk '{print '$workday'" "$1"	"$2}' >> referer/ref$s.cnt
done

rm -f $tmpdir/acctmp.log

month=`date -d $workday '+%m'`
day=`date -d $workday '+%d'`
year=`date -d $workday '+%Y'`

awk 'BEGIN {a=0.0};
	substr($1,1,4)=="'$month$day'" {a=a+$3};
	END {printf "'$year$month$day' %4d\n",int(a/0.288+0.5)};
	' load-$year$month >>account/00load.avg

# user and class connection time accounting

ccsum_outdir=ccaccount
ccsum_date=$workday

rm -f $tmpdir/cc.log 2>/dev/null
[ -f $ccsum_outdir/done ] && grep '^'$ccsum_date $ccsum_outdir/done >/dev/null && exit

cat access.log access.log.old* 2>/dev/null | grep '^'$ccsum_date'.*,' | sort >$tmpdir/cc.log
if [ ! -s $tmpdir/cc.log ]; then exit; fi

export ccsum_outdir ccsum_date
../bin/ccsum $tmpdir/cc.log
rm -f $tmpdir/cc.log

