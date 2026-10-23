#! /bin/sh
#
# chroot package cannot be updated automatically, because it is
# system-dependent.

whome=`pwd`

eraselink="public_html/modules/template
public_html/modules/adm
public_html/modules/home
public_html/modules/classes
public_html/html
"

if [ -z "$loadversion" ]; then
 echo Please execute this via an appropriate update script.
 exit
fi
if [ ! -f public_html/wims ]; then
 echo Wrong execution directory.
 exit
fi
if [ ! -f download/wims-$loadversion.tgz ]; then
 echo Wrong updating procedure: no tgz file found.
fi

rm -Rf update 2>/dev/null
mkdir -p update
cd update

echo Expanding system package wims-$loadversion.tgz.
tar -xzf ../download/wims-$loadversion.tgz
echo `date '+%H:%M:%S'`: Compilation starts.
cd src
./configure >>../../log/update2.log 2>&1
make all >>../../log/update2.log 2>&1
cd ..
if [ ! -f public_html/wims ]; then
 echo Compilation failed. Sorry.
 exit
fi
echo `date '+%H:%M:%S'`: Compilation finished.
rm -f log/unsecure log/update-version
if [ -d ../testing ]; then
 load=$whome/testing
else
 load=$whome
 for d in $eraselink
 do
  find $load/$d -type l -exec rm '{}' \;
 done
 if [ -d $load/public_html/w/adm/local ]; then
  ln -s ../../w/adm/local $load/public_html/modules/adm
 fi
 if [ -f $load/public_html/bin/ch..root ]; then
  cmp -s src/Misc/chroot.c $load/src/Misc/chroot.c 2>/dev/null && rm -f public_html/bin/ch..root
 fi
 if [ -f $load/public_html/bin/wrap..exec ]; then
  cmp -s src/Misc/wrap.c $load/src/Misc/wrap.c 2>/dev/null && rm -f public_html/bin/wrap..exec
 fi
 if [ -f $load/bin/wimsd ]; then
  cmp -s src/Wimsd/wimsd.c $load/src/Wimsd/wimsd.c 2>/dev/null && rm -f bin/wimsd
 fi
 if [ -f public_html/bin/ch..root ] || [ -f public_html/bin/wrap..exec ] || [ -f bin/wimsd ]; then
  echo yes >$load/log/unsecure
  for f in bin/wimsd public_html/bin/ch..root public_html/bin/wrap..exec; do
   [ -f $f ] && rm -f $load/$f
  done
 fi
 mkdir -p $load/tmp/log
 mv -f tmp/log/wimslogd.new $load/tmp/log
fi

echo WIMS server shut down.
echo `date '+%H:%M:%S'`: update system files.
$load/bin/server-interrupt >>../log/update2.log
cp -upR * $load

echo `date '+%H:%M:%S'`: System update finished.
#$load/bin/server-resume

#cd $load/public_html/modules
#for f in $whome/download/wims_modules*
#do
# echo Expanding $f
# tar -xzf $f
#done

cd $load
echo `date '+%H:%M:%S'`: Update system indexes.
bin/update
bin/mkindex
for m in H3/algebra/spuzzle U2/algebra/qpuzzle; do
 if [ -x public_html/modules/$m/makepieces ]; then
  public_html/modules/$m/makepieces
 fi
done
bin/server-resume
echo `date '+%H:%M:%S'`: Clean up.
rm -Rf $whome/update
rm -f tmp/log/wimslogd.pid
echo `date '+%H:%M:%S'`: End of update.

