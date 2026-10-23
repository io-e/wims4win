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

/* WWW interactive multipurpose server, shared memory definitions */

#ifndef wimsshm_h

#define SHM_NAME "/wims.shm"
#define SHM_SIZE 4096

typedef struct shm_struct {
    int dm_pid, dm_time, dm_lock;
    char loadavg[64];
    int haschroot;
    char myip[32];

    char *cf_commonpath;
    char *cf_aliased_cgi;
    char *cf_aliased_getfile;
    int   cf_backup_hour;
    char *cf_cgi_name;
    int   cf_class_limit;
    int   cf_class_quota;
    int   cf_superclass_quota;
    char *cf_class_regpass;
    int   cf_class_user_limit;
    char *cf_css;
    char *cf_default_anim_format;
    char *cf_default_ins_format;
    char *cf_default_insplot_font;
    int   cf_default_texposition;
    char *cf_devel_modules;
    int   cf_doc_quota;
    char *cf_doc_regpass;
    int   cf_examlog_limit;

    int fcachecnt;
} shm_struct;

struct shm_struct *shmptr;

#define wimsshm_h
#endif

