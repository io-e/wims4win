# Process this file with autoconf to produce a configure script.

AC_INIT(WIMS, esyscmd(awk 'ORS=""; {print $0}' ../version), wimsdev@groupes.renater.fr)
AC_CONFIG_SRCDIR([wims.c])
AC_CONFIG_HEADER([config.h:config.hin])

AC_DEFUN([WIMS_NEED], [
  AC_PATH_PROG($1,$2)
  if test -z "`eval echo '$'$1`"; then
    echo ERROR: WIMS requires $3
    echo Please install this package then restart configure.
    exit 1
  fi
])
AC_DEFUN([WIMS_FAIL], [
  if test "$2" = no; then
    echo
    echo ERROR! Your system does not have $1
    echo WIMS cannot be installed. Please report.
    echo
    exit 1
  fi
])
AC_DEFUN([WIMS_CHECK_FUNC], [
  AC_CHECK_FUNCS($1)
  WIMS_FAIL($1, $ac_cv_func_$1)
])

# Checks for programs.
AC_PROG_LN_S
if test "$LN_S" != "ln -s"; then
  WIMS_FAIL([symbolic links], no)
fi
AC_PROG_AWK
AC_PROG_YACC
AC_PROG_CC(gcc cc)
AC_PROG_CXX
AC_PROG_CPP
AC_PROG_LEX
AC_PROG_MAKE_SET
AC_PROG_RANLIB
AC_CHECK_PROG(STRIP,strip,strip,:)

#WIMS_NEED([TEX], [tex], [TeX])
#WIMS_NEED([GP], [gp], [Pari/GP])
#WIMS_NEED([CONVERT], [convert], [ImageMagick])

# Checks for libraries.
SAVE=$LIBS
AC_CHECK_LIB([m], [cos])
WIMS_FAIL([Math library libm], $ac_cv_lib_m_cos)

AC_CHECK_FUNCS([rint])
WIMS_CHECK_FUNC(floor)
WIMS_CHECK_FUNC(pow)
WIMS_CHECK_FUNC(sqrt)

LIBS=$SAVE
AC_CHECK_LIB([gd], [gdFontCacheSetup])
AC_CHECK_FUNCS([gdFontCacheSetup])
GDLIB=$LIBS

LIBS=$SAVE
AC_CHECK_LIB([crypt], [crypt])
AC_CHECK_FUNCS([crypt])
CRYPTLIB=$LIBS

LIBS=$SAVE
AC_CHECK_LIB([fl], [main])
FLEXLIB=$LIBS

LIBS=$SAVE
fun=gethostbyname
AC_CHECK_FUNCS($fun)
if test "$ac_cv_func_$fun" = no; then
  AC_CHECK_LIB([nsl], $fun)
  WIMS_FAIL($fun, $ac_cv_lib_$fun)
fi

WIMS_CHECK_FUNC(gethostname)

fun=socket
AC_CHECK_FUNCS($fun)
if test "$ac_cv_func_$fun" = no; then
  AC_CHECK_LIB([socket], [$fun])
  if test "$ac_cv_lib_$fun" = no; then
    AC_CHECK_LIB([socket], [$fun], [], [], [-lnsl])
    WIMS_FAIL($fun, $ac_cv_lib_$fun)
  fi
fi

fun=inet_ntoa
AC_CHECK_FUNCS($fun)
if test "$ac_cv_func_$fun" = no; then
  AC_CHECK_LIB([socket], [$fun], [], [], [-lnsl])
fi
fun=inet_aton
AC_CHECK_FUNCS($fun)
if test "$ac_cv_func_$fun" = no; then
  AC_CHECK_LIB([resolv], [$fun])
fi
NETLIBS=$LIBS

LIBS=$SAVE

# Checks for header files.
AC_FUNC_ALLOCA
AC_HEADER_DIRENT
AC_HEADER_STDC
AC_HEADER_SYS_WAIT
AC_MEMORY_H
AC_CHECK_HEADERS([crypt.h arpa/inet.h fcntl.h malloc.h netdb.h netinet/in.h \
stdlib.h string.h sys/socket.h sys/time.h unistd.h utime.h])
WIMS_FAIL([ANSI C header files], $ac_cv_header_stdc)
WIMS_FAIL([Header file unistd.h], $ac_cv_header_unistd_h)

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_C_INLINE
AC_TYPE_PID_T
AC_TYPE_SIZE_T
AC_HEADER_TIME
AC_STRUCT_TM
AC_CHECK_TYPES(socklen_t, [], [], [
#include <sys/types.h>
#include <sys/socket.h>])

# Runpath
RPATH=
case "$CC+$with_dynamic" in
  gcc+yes) RPATH="-Xlinker -rpath -Xlinker ../lib:../../lib:../../../lib";;
esac

# Checks for library functions.
AC_FUNC_MALLOC
WIMS_FAIL(malloc, $ac_cv_func_malloc_0_nonnull)
AC_FUNC_REALLOC
WIMS_FAIL(realloc, $ac_cv_func_realloc_0_nonnull)
AC_FUNC_FORK
WIMS_FAIL([working fork], $ac_cv_func_fork_works)

AC_FUNC_ERROR_AT_LINE
AC_FUNC_LSTAT
AC_FUNC_MEMCMP
AC_TYPE_SIGNAL
AC_FUNC_STAT
AC_FUNC_STRTOD
AC_CHECK_FUNCS([dup2 regcomp putenv strcasecmp strchr strdup strerror utime])

WIMS_CHECK_FUNC(alarm)
WIMS_CHECK_FUNC(getcwd)
WIMS_CHECK_FUNC(gettimeofday)
WIMS_CHECK_FUNC(memmove)
WIMS_CHECK_FUNC(memset)
WIMS_CHECK_FUNC(mkdir)
WIMS_CHECK_FUNC(rmdir)
WIMS_CHECK_FUNC(select)
WIMS_CHECK_FUNC(strncasecmp)
WIMS_CHECK_FUNC(strpbrk)
WIMS_CHECK_FUNC(strrchr)
WIMS_CHECK_FUNC(strstr)
WIMS_CHECK_FUNC(utime)

# setenv ?
AC_CHECK_FUNCS([setenv])
if test "$ac_cv_func_setenv" = no; then
  common=../Common
  SETENV_OBJS=$common/setenv.o
# FIXME: there should not be various xmallocs with different semantics. Only
# SETENV_OBJS should be necessary.
  ALLSETENV_OBJS="$SETENV_OBJS $common/xmalloc.o"
else
  SETENV_OBJS=
  ALLSETENV_OBJS=
fi

# Case insensitive file system?
AC_CHECK_FILE(WIMS.C, D_CASE_INSENSITIVE_FS="-DCASE_INSENSITIVE_FS=1",
                      D_CASE_INSENSITIVE_FS=)
# Static libm?
AC_CHECK_FILE(/usr/lib/libm.a,STATIC_LIB=-static,STATIC_LIB=)

# Configure options
AC_ARG_WITH(units, [  --without-units	  Don't compile units-filter (units recognition)],echo Compile units-filter: $with_units, with_units=yes; echo Compile units-filter: yes)
BUILD_UNITS=
if test "$with_units" = "yes"; then
  case "$LEXLIB" in
  -lfl | -ll) BUILD_UNITS=units-filter;;
  *) with_units=no; echo "Wait ... flex is not available, Compile units : no";;
  esac
fi

AC_ARG_WITH(chemeq,[  --without-chemeq	  Don't compile chemeq (chemical equation analyzer)],echo Compile chemeq: $with_chemeq, with_chemeq=yes; echo Compile chemeq: yes)
BUILD_CHEMEQ=
if test "$with_chemeq" = "yes"; then
  case "$LEXLIB" in
  -lfl | -ll) if test "$CXX" = "g++"; then
          case `"$CXX" --version` in
          [ 1*|2.[1-8]*|2.9[1-4]*|2.95.[1-2]) ]
              with_chemeq=no
              echo "Wait ... I need at least g++-2.95.3, Compile chemeq: no" ;;
            *) BUILD_CHEMEQ=chemeq;;
          esac
        else
          with_chemeq=no
          echo "Wait ... g++ is not available, Compile chemeq: no"
        fi;;
  *) with_chemeq=no
     echo "Wait ... flex is not available, Compile chemeq: no";;
  esac
fi

AC_ARG_WITH(wimsd,[  --without-wimsd	  Don't compile wimsd (standalone http server)],echo Compile wimsd: $with_wimsd, with_wimsd=yes; echo Compile wimsd: yes)
if test "$with_wimsd" = "yes"; then
  BUILD_WIMSD=wimsd
fi

if test -n "$SOURCE_DATE_EPOCH"; then
  # When a debian package is built, SOURCE_DATE_EPOCH is defined, so
  # the build date is taken from the head of the file debian/changelog.
  # This provides reproducible builds, and more security in Debian.
  date=`date --utc --date="@$SOURCE_DATE_EPOCH" +%Y-%m-%d`
else
  date=`date +%Y-%m-%d`
fi

DEFINES="-DGNU_SOURCE -DVERSION_DATE=\\\"$date\\\""
AC_SUBST(D_CASE_INSENSITIVE_FS)
AC_SUBST(STATIC_LIB)
AC_SUBST(DEFINES)
AC_SUBST(STRIP)
AC_SUBST(RPATH)
AC_SUBST(BUILD_UNITS)
AC_SUBST(BUILD_CHEMEQ)
AC_SUBST(BUILD_WIMSD)
AC_SUBST(SETENV_OBJS)
AC_SUBST(ALLSETENV_OBJS)
AC_SUBST(NETLIBS)
AC_SUBST(FLEXLIB)
AC_SUBST(GDLIB)
AC_SUBST(CRYPTLIB)

AC_CONFIG_FILES([Flydraw/Makefile
                 Interfaces/Makefile
                 Lib/Makefile
                 Makefile
                 Misc/Makefile
                 Misc/symtext/Makefile
                 Misc/mathexp/Makefile
                 Misc/chemeq/Makefile
                 Misc/chemeq/src/Makefile
                 Misc/units-filter/src/Makefile
                 Misc/voronoiW/Makefile
                 Misc/bioinfo/Makefile
                 Misc/checkmol/Makefile
                 Misc/crossword/Makefile
                 Misc/lceb/Makefile
                 Misc/moneyprint/Makefile
                 Misc/toascii/Makefile
                 Misc/scienceprint/Makefile
                 Misc/sigdigits/Makefile
                 Misc/canvasdraw/Makefile
                 Misc/whirlgif/Makefile
                 Misc/authors/jm.evers/applets/Makefile
		 Misc/applets/Makefile
                 Module/Makefile
                 Module/drawode/Makefile
                 OefMsg2wims/Makefile
                 Wimsd/Makefile
                 Wimslogd/Makefile
                 Texgif/Makefile
                 Mathml/Makefile])
AC_OUTPUT
