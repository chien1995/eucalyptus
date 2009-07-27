/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#ifndef INCLUDE_MISC_H
#define INCLUDE_MISC_H

#include <stdarg.h>

#ifndef NO_AXIS /* for compiling on systems without Axis */
#include <neethi_policy.h>
#include <neethi_util.h>
#include <axutil_utils.h>
#include <axis2_client.h>
#include <axis2_stub.h>
int InitWSSEC(axutil_env_t *env, axis2_stub_t *stub, char *policyFile);
#endif

enum {EUCADEBUG2, EUCADEBUG, EUCAINFO, EUCAWARN, EUCAERROR, EUCAFATAL};

char * replace_string (char ** stringp, char * source, char * destination );
int sscanf_lines (char * lines, char * format, void * varp);
char * system_output (char * shell_command );
char *getConfString(char *configFile, char *key);

/**
 * Search in file #path# for a variable named #name#. It will put
 * whatever after the = in value (which will need to be freed by the
 * caller). 
 *
 * Returns -1 on error (open file, out of memory, parse error ...) 
 *          0 if variable not found in file
 *          1 if found and value is indeed valid
 *
 * Examples of parsed line:
 * TEST="test uno due tre"
 *      TEST = prova
 * TEST=prova
 */
int
get_conf_var(	const char *path,
		const char *name,
		char **value);

/**
 * The next 2 functions deal with turning a variable values (that is a
 * string) into a NULL terminated array of strings (char **). Example:
 * 	var="hostname1 hostname2"
 * it will return
 * 	()[0] = hostname1
 * 	()[1] = hostname2
 * 	()[2] = NULL
 *
 * the return array needs to be freed and you can use free_char_list() to
 * do so.
 *
 * Return NULL if something went wrong (probably out of memory, or an
 * array of strings. Notice that if something is wrong in the parsing
 * (the variable contains only spaces) you'll get back an array with only
 * one element and the element is NULL.
 */
void
free_char_list(char **value);

char **
from_var_to_char_list(const char *var);

// dan's functions
int logprintf(const char *format, ...);
int logprintfl(int level, const char *format, ...);
void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other);
int logfile(char *file, int in_loglevel);
int check_directory(char *dir);
int check_file(char *file);
// argument checker
int param_check(char *func, ...);
// end of dan't functions

#ifdef DEBUG
#define PRINTF(a) logprintf a
#else
#define PRINTF(a)
#endif

#ifdef DEBUG1
#define PRINTF1(a) logprintf a
#else
#define PRINTF1(a)
#endif

#ifdef DEBUGXML
#define PRINTF_XML(a) logprintf a
#else
#define PRINTF_XML(a)
#endif

int hash_code (const char * s);
char * get_string_stats (const char * s);
int run (const char * arg1, ...);
int vrun (const char * fmt, ...);
int cat (const char * file_name);
int logcat (int debug_level, const char * file_name);
int touch (const char * path);
int diff (const char * path1, const char * path2);
long long dir_size (const char * path);
char * file2str (const char * path); /* read file 'path' into a new string */
long long str2longlong (const char * str, const char * begin, const char * end); /* extract integer from str bound by 'begin' and 'end' */
pid_t timewait(pid_t pid, int *status, int timeout);

#endif
