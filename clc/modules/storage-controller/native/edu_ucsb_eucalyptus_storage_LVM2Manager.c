/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 *
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms, with
 *   or without modification, are permitted provided that the following
 *   conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *   THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *   LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *   SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *   BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *   THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *   OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *   WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *   ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/

/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

#include <edu_ucsb_eucalyptus_storage_LVM2Manager.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <sys/wait.h>
#include <signal.h>

#define EUCALYPTUS_ENV_VAR_NAME  "EUCALYPTUS"

static const char* blockSize = "1M";
jstring run_command(JNIEnv *env, char *cmd, int outfd) {
	FILE* fd;
	int pid;
	char readbuffer[256];
	char absolute_cmd[256];
    char* home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) {
        home = strdup (""); /* root by default */
    } else {
        home = strdup (home);
    }

    snprintf(absolute_cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap %s", home, cmd);
    fprintf(stderr, "command: %s\n", absolute_cmd);
	bzero(readbuffer, 256);
	fd = popen(absolute_cmd, "r");
	if(fgets(readbuffer, 256, fd)) {
	    char* ptr = strchr(readbuffer, '\n');
	    if(ptr != NULL) {
		    *ptr = '\0';
	    }
	}
	pclose(fd);
	return (*env)->NewStringUTF(env, readbuffer);
}

int run_command_and_get_status(JNIEnv *env, char *cmd, int outfd) {
	FILE* fd;
	int pid;
	int status;
	char absolute_cmd[256];
    char* home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) {
        home = strdup (""); /* root by default */
    } else {
        home = strdup (home);
    }

    snprintf(absolute_cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap %s", home, cmd);
    fprintf(stderr, "command: %s\n", absolute_cmd);
	fd = popen(absolute_cmd, "r");
	status = pclose(fd);
	return WEXITSTATUS(status);
}

int run_command_and_get_pid(char *cmd, char **args) {
    int fd[2];
    pipe(fd);
    int pid = -1;

    if ((pid = fork()) == -1) {
        perror("Could not run command");
        return -1;
    }

   if (pid == 0) {
        //daemonize
        DIR *proc_fd_dir;
        struct dirent *fd_dir;
        int fd_to_close;
        char fd_path[128];
        int my_pid = getpid();

        umask(0);
        int sid = setsid();
        if(sid < 0)
            return -1;
        char* home = getenv (EUCALYPTUS_ENV_VAR_NAME);
        if (!home) {
            home = strdup (""); /* root by default */
        } else {
        home = strdup (home);
        }
        chdir(home);

        //close all open fds
        snprintf(fd_path, 128, "/proc/%d/fd", my_pid);

        if ((proc_fd_dir = opendir(fd_path)) == NULL) {
            perror("ERROR: Cannot opendir\n");
            return -1;
        }

        while ((fd_dir = readdir(proc_fd_dir)) != NULL) {
            if (isdigit(fd_dir->d_name[0])) {
                fd_to_close =  atoi(fd_dir->d_name);
                close(fd_to_close);
            }
        }

        freopen( "/dev/null", "r", stdin);
        freopen( "/dev/null", "w", stdout);
        freopen( "/dev/null", "w", stderr);
        exit(execvp(cmd, args));
   } else {
        close(fd[1]);
   }
   return pid;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_getAoEStatus
  (JNIEnv *env, jobject obj, jstring processId) {
    const jbyte* pid = (*env)->GetStringUTFChars(env, processId, NULL);

    char command[128];
    snprintf(command, 128, "cat /proc/%s/cmdline", pid);

    jstring returnValue = run_command(env, command, 1);
    (*env)->ReleaseStringUTFChars(env, processId, pid);
    return returnValue;
}

JNIEXPORT void JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createSnapshot
(JNIEnv *env, jobject obj, jstring snapshotId) {
	const jbyte *snapshot_id;
	snapshot_id = (*env)->GetStringUTFChars(env, snapshotId, NULL);
	(*env)->ReleaseStringUTFChars(env, snapshotId, snapshot_id);
}

JNIEXPORT jint JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_losetup
  (JNIEnv *env, jobject obj, jstring fileName, jstring loDevName) {
	const jbyte* filename = (*env)->GetStringUTFChars(env, fileName, NULL);
    const jbyte* lodevname = (*env)->GetStringUTFChars(env, loDevName, NULL);

	char command[512];
	snprintf(command, 512, "losetup %s %s", lodevname, filename);
	int returnValue = run_command_and_get_status(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, fileName, filename);
	(*env)->ReleaseStringUTFChars(env, loDevName, lodevname);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_getLoopback
  (JNIEnv *env, jobject obj, jstring loDevName) {
	const jbyte* lodevname = (*env)->GetStringUTFChars(env, loDevName, NULL);

	char command[128];
	snprintf(command, 128, "losetup %s", lodevname);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, loDevName, lodevname);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_findFreeLoopback
  (JNIEnv *env, jobject obj) {
	char command[64];
	snprintf(command, 64, "losetup -f");
	jstring returnValue = run_command(env, command, 1);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createEmptyFile
(JNIEnv *env, jobject obj, jstring fileName, jint size) {
	char command[256];
	const jbyte* filename = (*env)->GetStringUTFChars(env, fileName, NULL);
    size = size * 1024;
	snprintf(command, 256, "dd if=/dev/zero of=%s count=1 bs=%s seek=%d", filename, blockSize, size-1);

	jstring returnValue = run_command(env, command, 2);

	(*env)->ReleaseStringUTFChars(env, fileName, filename);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createAbsoluteEmptyFile
(JNIEnv *env, jobject obj, jstring fileName, jlong size) {
	char command[256];
	const jbyte* filename = (*env)->GetStringUTFChars(env, fileName, NULL);
    fprintf(stderr, "hello\n");

    size = size / (1024 * 1024);
	snprintf(command, 256, "dd if=/dev/zero of=%s count=1 bs=%s seek=%ld", filename, blockSize, size-1);

	jstring returnValue = run_command(env, command, 2);

	(*env)->ReleaseStringUTFChars(env, fileName, filename);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createPhysicalVolume
(JNIEnv *env, jobject obj, jstring loDevName) {
	const jbyte* dev_name = (*env)->GetStringUTFChars(env, loDevName, NULL);
	char command[128];

	snprintf(command, 128, "pvcreate %s", dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, loDevName, dev_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createVolumeGroup
(JNIEnv *env, jobject obj, jstring pvName, jstring vgName) {
	const jbyte* dev_name = (*env)->GetStringUTFChars(env, pvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "vgcreate %s %s", vg_name, dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, dev_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createLogicalVolume
(JNIEnv *env, jobject obj, jstring vgName, jstring lvName) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "lvcreate -n %s -l 100%%FREE %s", lv_name, vg_name);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);

	return returnValue;
}

JNIEXPORT jint JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_aoeExport
(JNIEnv *env, jobject obj, jstring iface, jstring lvName, jint major, jint minor) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	const jbyte* if_name = (*env)->GetStringUTFChars(env, iface, NULL);
	char major_str[4];
	char minor_str[4];
    char *args[7];
    char rootwrap[256];
    char* home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!home) {
        home = strdup (""); /* root by default */
    } else {
        home = strdup (home);
    }

    snprintf(rootwrap, 256, "%s/usr/lib/eucalyptus/euca_rootwrap", home);

    snprintf(major_str, 4, "%d", major);
    snprintf(minor_str, 4, "%d", minor);

    args[0] = rootwrap;
    args[1] = "vblade";
    args[2] = major_str;
    args[3] = minor_str;
    args[4] = (char *) if_name;
    args[5] = (char *) lv_name;
    args[6] = (char *) NULL;

    int pid = run_command_and_get_pid(rootwrap, args);
	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
	(*env)->ReleaseStringUTFChars(env, iface, if_name);
	return pid;
}

JNIEXPORT void JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_aoeUnexport
  (JNIEnv *env, jobject obj, jint vblade_pid) {
    //TODO: blind kill. Hope for the best.
   char command[128];

   snprintf(command, 128, "kill %d", vblade_pid);
   run_command(env, command, 1);
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removeLogicalVolume
  (JNIEnv *env, jobject obj, jstring lvName) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
    char command[128];

	snprintf(command, 128, "lvremove -f %s", lv_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removeVolumeGroup
  (JNIEnv *env, jobject obj, jstring vgName) {
    const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
    char command[128];

	snprintf(command, 128, "vgremove %s", vg_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_disableLogicalVolume
  (JNIEnv *env, jobject obj, jstring lvName) {
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	char command[256];

	snprintf(command, 256, "lvchange -an %s", lv_name);
    jstring returnValue = run_command(env, command, 1);

    (*env)->ReleaseStringUTFChars(env, lvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removePhysicalVolume
  (JNIEnv *env, jobject obj, jstring pvName) {
    const jbyte* pv_name = (*env)->GetStringUTFChars(env, pvName, NULL);
    char command[128];

	snprintf(command, 128, "pvremove %s", pv_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, pv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_removeLoopback
  (JNIEnv *env, jobject obj, jstring loDevName) {
    const jbyte* lo_dev_name = (*env)->GetStringUTFChars(env, loDevName, NULL);
    char command[128];

	snprintf(command, 128, "losetup -d %s", lo_dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, loDevName, lo_dev_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_createSnapshotLogicalVolume
  (JNIEnv *env, jobject obj, jstring lvName, jstring snapLvName) {
	const jbyte* lv_name = (*env)->GetStringUTFChars(env, lvName, NULL);
	const jbyte* snap_lv_name = (*env)->GetStringUTFChars(env, snapLvName, NULL);
	char command[256];

	snprintf(command, 256, "lvcreate -n %s -s -l 100%%FREE %s", snap_lv_name, lv_name);
	jstring returnValue = run_command(env, command, 1);
	(*env)->ReleaseStringUTFChars(env, lvName, lv_name);
	(*env)->ReleaseStringUTFChars(env, snapLvName, snap_lv_name);

	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_extendVolumeGroup
  (JNIEnv *env, jobject obj, jstring pvName, jstring vgName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, pvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "vgextend %s %s", vg_name, dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, dev_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_reduceVolumeGroup
  (JNIEnv *env, jobject obj, jstring vgName, jstring pvName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, pvName, NULL);
	const jbyte* vg_name = (*env)->GetStringUTFChars(env, vgName, NULL);
	char command[256];

	snprintf(command, 256, "vgreduce %s %s", vg_name, dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, pvName, dev_name);
	(*env)->ReleaseStringUTFChars(env, vgName, vg_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_suspendDevice
  (JNIEnv *env, jobject obj, jstring deviceName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, deviceName, NULL);
	char command[128];

	snprintf(command, 128, "dmsetup -v suspend %s", dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, deviceName, dev_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_resumeDevice
  (JNIEnv *env, jobject obj, jstring deviceName) {
    const jbyte* dev_name = (*env)->GetStringUTFChars(env, deviceName, NULL);
	char command[128];

	snprintf(command, 128, "dmsetup -v resume %s", dev_name);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, deviceName, dev_name);
	return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_duplicateLogicalVolume
  (JNIEnv *env, jobject obj, jstring oldLvName, jstring newLvName) {
    const jbyte* old_lv_name = (*env)->GetStringUTFChars(env, oldLvName, NULL);
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, newLvName, NULL);
	char command[256];

	snprintf(command, 256, "dd if=%s of=%s bs=%s", old_lv_name, lv_name, blockSize);
	jstring returnValue = run_command(env, command, 1);

	(*env)->ReleaseStringUTFChars(env, oldLvName, old_lv_name);
	(*env)->ReleaseStringUTFChars(env, newLvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_enableLogicalVolume
  (JNIEnv *env, jobject obj, jstring absoluteLvName) {
    const jbyte* lv_name = (*env)->GetStringUTFChars(env, absoluteLvName, NULL);
	char command[256];

	snprintf(command, 256, "lvchange -ay %s", lv_name);
    jstring returnValue = run_command(env, command, 1);

    (*env)->ReleaseStringUTFChars(env, absoluteLvName, lv_name);
    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_getLvmVersion
  (JNIEnv *env, jobject obj) {
	char command[128];

   	snprintf(command, 128, "lvm version");
    jstring returnValue = run_command(env, command, 1);

    return returnValue;
}

JNIEXPORT jstring JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_getVblade
  (JNIEnv *env, jobject obj) {
	char command[128];

   	snprintf(command, 128, "which vblade");
    jstring returnValue = run_command(env, command, 1);

    return returnValue;
}

void sigchld(int signal)
{
 while (0 < waitpid(-1, NULL, WNOHANG));
}

JNIEXPORT void JNICALL Java_edu_ucsb_eucalyptus_storage_LVM2Manager_initialize
  (JNIEnv *env, jobject obj) {
  signal(SIGCHLD, sigchld);
}

