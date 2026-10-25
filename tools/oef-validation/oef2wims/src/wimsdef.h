/*    Copyright (C) 1998-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* WWW interactive multipurpose server, header definitions */

#ifndef wimsdef_h
#define wimsdef_h
/* maximal (multi-)line length. */
#define MAX_LINELEN 45000
/* limit of items in a list; will never exceed MAX_LINELEN/2. */
#define MAX_LIST 8192
/* maximal name length */
#define MAX_NAMELEN 63
/* maximal file name length, complete path. */
#define MAX_FNAME 199
/* maximal module name length. */
#define MAX_MODULELEN 100
/* maximal number of resident executions */
#define MAX_MULTIEXEC 4
/* maximal file length for webget */
#define MAX_WEBGETFLEN (32*1024*1024)
/* max length of class name */
#define MAX_CLASSLEN 31
/* max length of session name */
#define MAX_SESSIONLEN 23
/* max number of exercises in a class (sheets and exams)
 * if it is greater than MAX_EXOS * MAX_SHEETS + MAX_EXAMS,
 * the limitation is not useful
 */
#define MAX_CLASSEXOS 2048
/* Must be less than 99 to be sure that some sessions filename
   do not cause buffer overflow (see exam.c line 71)
 */
/* max number of exercises in a sheet or exam; not modifiable */
#define MAX_EXOS 64
/* max number of sheets; not modifiable */
#define MAX_SHEETS 64
/* max number of freeworks; */
#define MAX_FREEWORKS 64
/* max number of user in class to use module grades; */
#define MAX_USERFORGRADES 200
/* max number of exams; not used except in Misc/exostat and in adm modules */
#define MAX_EXAMS 80
/* max number of votes */
#define MAX_VOTES 64
/* max numbers of replies in OEF */
#define MAX_OEFREPLIES 100
/* max numbers of choices in OEF */
#define MAX_OEFCHOICES 30
/* max number of values for one techvar*/
#define MAX_TECHVARVAL 20
/* max limit of number of techvar (in each level of struture ) */
#define MAX_LTECHVAR 64
/* max length of data files: records, activities, definitions. */
#define MAX_FILELEN (2048*1024-1)
/* max number of filewrite calls in one request */
#define MAX_FWRITE 64
/* max total size of filewrite in one request */
#define MAX_FWRITE_SIZE (128*1024)
/* max languages on a site, hard limit */
#define MAX_LANGUAGES 32
/* max subclasses in a structure (must be less than 99)*/
#define MAX_SUBCLASSES 32
/* max number of lines in a work file */
/* Limit for datafield cache */
#define LINE_LIMIT 16384
/* size of cached datafields */
#define DATAFIELD_LIMIT 16384
/* max required points */
#define MAX_REQUIRE 200
/* lengthmax of the seed as a char */
#define SEEDSIZE 12
/* maximum of the number of seed/score used in getseed*/
#define MAX_SCORESEED 70
/* maximum of the number of exercises presented with the same seed
  if it is changed, search for MAX_SEEDSCORE in public_html
  and change the value (in non adm modules)
*/
#define MAX_SEEDSCORE 6
/* maximum of complementary regvars*/
#define MAX_REGVARS 10
/* temporary log file */
#define TEMP_LOG_FILE "../log/temp.log"
/* Tag string for randfile */
#define tag_string "\n:"

/* Define current version of the server here */
#define wims_version PACKAGE_VERSION
#ifdef VERSION_DATE
  #define wims_version_date VERSION_DATE
#else
  #define wims_version_date ""
#endif

#define SHORTSWNAME "WIMS"
#define LONGSWNAME "WWW Interactive Multipurpose Server"
#define SEEDSCORES (2*MAX_SCORESEED*SEEDSIZE)
typedef struct scoreresult {
  unsigned short int sh, exo, pad, seedlastcnt, active;
  float require, weight, score, mean, last, try, best, level, new, seedscorelast;
  char seedlast[SEEDSIZE];
  char seedscores[SEEDSCORES];

} scoreresult;

#endif
