/*    Copyright (C) 2012 WIMSDEV
 *    This file is part of the WIMS package.
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
#include "wims.h"
int disable_mathml;
int mathml(char *p, int option ){
  if (strlen(p)==0) return 1 ;
  if( mathalign_base <= 1){
    internal_error(" why is wims trying mathml()?\n");
    return 0; // go to insmath with gifs
  }
  if( disable_mathml==1 || atoi(getvar("disable_mathml"))==1) return 0;
  if(strlen(p) > MAX_LINELEN ){ // too big ? probably too big for gifs as well ; but error signalling is better in gif-methods
    mathalign_base = 1;// 0 or 1 position of tex_gifs
    return 0; // go to insmath with gifs
  }
//singlespace(p); // needed for check unbalanced \left\right in wims_mathml.cc --> insmath.c
  singlespace(p);
  int my_pipe[2];
  pid_t pid;
  if(pipe(my_pipe)){// pipe could not be opened...
    internal_error("mathml(): pipe() failure.\n");
    mathalign_base = 1;
    return 0; // go to insmath with gifs
  }
  else
  {
    pid = fork();
    if (pid == (pid_t) 0){
          /*
           this buffer should probably be set to 0.5 * MAX_LINELEN : mathml is very big !
           if output mathml string larger ;ERROR will be signaled by wims_mathml.
          */
      char mml_buffer[MAX_LINELEN+1];
      sprintf(mml_buffer, "%d", MAX_LINELEN);// int --> char
          /* setting --tex-size argument to wims_mathml.cc */
      char mathml_tex_size[64];/*this should be a string like "250%"*/
      int texsize_list[]={20,30,50,70,90,100,120,130,150,180,220,300};
          /*
          analogue to  mathfonts.c but now we talk %  and we could make any amount of changes
          not limited by font-size !!
          when math_with_gifs is removed from wims, we can rethink / improve this
          */
      int idx = 5;
      int use_js_zoom = 0;
          /*
          default js-zoom in mathml if disabled
          enable via adm/light
          next code is stolen from wims.c
          */
      if(getvar("useropts") != NULL || getvar("wims_useropts") != NULL){
        char *u;
        u = getvar("useropts");
            /* set via adm/light or via cookie? */
        if(u == NULL || *u == 0) u=getvar("wims_useropts"); /* wims_user? look into def file */
        if(u != NULL && *u != 0){
          if(myisdigit(u[0])){ /* 2 digit code : 12,22,32,...,92  [tex_size mathalignbase] */
            idx = u[0] - '0';
            if(idx < 0 || idx > 11) idx = 5; /* the default value 100% */
          }
          if(myisdigit(u[2]) && u[2]!=0){
                /*
                 suppose we add 0 or 1 to useropts for using js-zoom on mathml?
                 3 digitcode:  621 == fontsize idx=6 & use mathml & use zoom
                */
             use_js_zoom = u[2] - '0';
            if(use_js_zoom > 1 || use_js_zoom < 0){
              use_js_zoom = 0;
                      /* disable js-zooming of mathml */
            }
          }
        }
      }
      else
      { /* we take the default setting from adm/management config */
        idx = atoi(getvar("wims_texbasesize")) ;
        if(idx < 0 || idx > 11){ idx = 5; } /* the default value 100% */
      }
          /*
           check is a module wants to disable zooming (eg drag&drop or others )
           !set disable_zoom=yes
           if not set: disable_zoom="no"; see config.c
          */
      if( strcmp( getvar("disable_zoom") , "yes" ) == 0 ) use_js_zoom = 0;
          /* now write the "char" 100% into variable "mathml_tex_size" */
      snprintf(mathml_tex_size,sizeof(mathml_tex_size),"%d%%",texsize_list[idx]);
          /* int --> char : added % sign (needed for mathml) [%% = escaped %] */
      if(strlen(mathml_tex_size) == 0 ){ // this should not happen
        sprintf(mathml_tex_size,"%s","100%");
            /* if it goes wrong we set 100% */
      }
      char zoom[2];
      snprintf(zoom, 2 ,"%d",use_js_zoom);/* int --> char : "0" or "1" */
/*
jm.evers 30/9/2015

FOR TESTING SYNCHRONISATION FONTSIZE HTML--MATHML
SET DEFAULT tex-size = 100 %
and use a
<span style="fonst-size:1em" ><math .. > ... </math></span>
in wims_mathml.y

zooming will be adressed (rewritten) when things are OK !!
since zooming is only interesting in native MathML browsers (FireFox and Gecko family)
the zooming could be prepared in exec.c (where mathjax is included for all other browsers)
by adding a zoom function and mouselistener on span element (wims_mathml.y)

see forum post of Eric Reyssat
http://wimsedu.info/?topic=unites-de-mesure-et-mathml/#post-3105

*/
      char *argv[]={"wims_mathml","--use-zoom",zoom,"--tex-size 100%","--max-mml-size",mml_buffer,"--tex-string",p,NULL};
  /* This is the child process. Close other end first. */
      close(my_pipe[0]);
      dup2(my_pipe[1], 1);  /* send stdout to the pipe */
      dup2(my_pipe[1], 2);  /* send stderr to the pipe */
      close(my_pipe[1]);
      execv("../bin/wims_mathml",argv);
      internal_error("could not execute wims_mathml\n");
      mathalign_base = 1;
      return 0; /* go to insmath with gifs ! */
    }
    else
    {
      if (pid < (pid_t) 0){
        close(my_pipe[0]);  /* close the read end of the pipe in the parent */
        close(my_pipe[1]);  /* close the write end of the pipe in the parent */
        internal_error("mathml(): fork() failure.\n");
        mathalign_base = 1;
        return 0; /* go to insmath with gifs */
      }
      else
      {
        int status;
        FILE *stream;
        close(my_pipe[1]);  /* close the write end of the pipe in the parent */
        stream = fdopen (my_pipe[0], "r");
        char buffer[MAX_LINELEN+1];
            /* make buffer filled with 'zero/null' */
            memset(buffer,'\0',MAX_LINELEN); /* or use bzero(buffer,maxsize); */
            /* read output from program line by line */
        if (option == 1) {
          *p=0;
          while ( fgets(buffer, MAX_LINELEN, stream) != NULL ){
            if(strcmp(buffer,"ERROR") != 0)
              mystrncpy(p, buffer, MAX_LINELEN-1);
            else /* ERROR close stream; close pipe; wait for clean exit */
            {
              fclose (stream); /* do not know if this is really needed... but it won't hurt ? */
              close(my_pipe[0]);
              waitpid(pid, &status, 0);
              mathalign_base=1; /* go to insmath with gifs */
              return 0;
            }
          }
        }
        else /* this will probably not used ? remove it ? */
        {
          while ( fgets(buffer, MAX_LINELEN, stream) != NULL ){
            if(strcmp(buffer,"ERROR") != 0) output("%s", buffer);
            else
            {
              mathalign_base=1;
              return 0;
            }
          }
        }
        fclose (stream);
        close(my_pipe[0]);
        waitpid(pid, &status, 0);
      }
    }
  }
  return 1;
}
