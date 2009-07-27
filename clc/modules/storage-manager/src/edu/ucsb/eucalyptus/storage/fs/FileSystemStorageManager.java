/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.storage.fs;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.storage.StorageManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.channels.FileChannel;

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

public class FileSystemStorageManager implements StorageManager {

    public static final String FILE_SEPARATOR = "/";
    public static final String lvmRootDirectory = "/dev";
    private static boolean initialized = false;
    private static String eucaHome = "/opt/eucalyptus";
    public static final String EUCA_ROOT_WRAPPER = "/usr/lib/eucalyptus/euca_rootwrap";
    public static final int MAX_LOOP_DEVICES = 256;
    private static Logger LOG = Logger.getLogger(FileSystemStorageManager.class);

    private String rootDirectory;
    public FileSystemStorageManager(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public void initialize() {
        if(!initialized) {
            System.loadLibrary("fsstorage");
            initialized = true;
        }
    }

    public void checkPreconditions() throws EucalyptusCloudException {
        String eucaHomeDir = System.getProperty("euca.home");
        if(eucaHomeDir == null) {
            throw new EucalyptusCloudException("euca.home not set");
        }
        eucaHome = eucaHomeDir;
        if(!new File(eucaHome + EUCA_ROOT_WRAPPER).exists()) {
            throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist");
        }
        String returnValue = getLvmVersion();
        if(returnValue.length() == 0) {
            throw new EucalyptusCloudException("Is lvm installed?");
        } else {
            LOG.info(returnValue);
        }

    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }


    public void createBucket(String bucket) throws IOException {
        File bukkit = new File (rootDirectory + FILE_SEPARATOR + bucket);
        if(!bukkit.exists()) {
            if(!bukkit.mkdirs()) {
                throw new IOException("Unable to create bucket: " + bucket);
            }
        }
    }

    public long getSize(String bucket, String object) {
        File objectFile = new File (rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
        if(objectFile.exists())
            return objectFile.length();
        return -1;
    }

    public void deleteBucket(String bucket) throws IOException {
        File bukkit = new File (rootDirectory + FILE_SEPARATOR + bucket);
        if(!bukkit.delete()) {
            throw new IOException("Unable to delete bucket: " + bucket);
        }
    }

    public void createObject(String bucket, String object) throws IOException {
        File objectFile = new File (rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
        if (!objectFile.exists()) {
            if (!objectFile.createNewFile()) {
                throw new IOException("Unable to create: " + objectFile.getAbsolutePath());
            }
        }
    }

    public int readObject(String bucket, String object, byte[] bytes, long offset) throws IOException {
        return readObject(rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object, bytes, offset);
    }

    public int readObject(String path, byte[] bytes, long offset) throws IOException {
        File objectFile = new File (path);
        if (!objectFile.exists()) {
            throw new IOException("Unable to read: " + path);
        }
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(objectFile));
        if (offset > 0) {
            inputStream.skip(offset);
        }
        int bytesRead = inputStream.read(bytes);
        inputStream.close();
        return bytesRead;
    }

    public void deleteObject(String bucket, String object) throws IOException {
        File objectFile = new File (rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
        if (objectFile.exists()) {
            if(!objectFile.delete()) {
                throw new IOException("Unable to delete: " + objectFile.getAbsolutePath());
            }
        }
    }

    public void deleteAbsoluteObject(String object) throws IOException {
        File objectFile = new File (object);
        if (objectFile.exists()) {
            if(!objectFile.delete()) {
                throw new IOException("Unable to delete: " + object);
            }
        }
    }

    public void putObject(String bucket, String object, byte[] base64Data, boolean append) throws IOException {
        File objectFile = new File (rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
        if (!objectFile.exists()) {
            objectFile.createNewFile();
        }
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(objectFile, append));
        outputStream.write(base64Data);
        outputStream.close();
    }

    public void renameObject(String bucket, String oldName, String newName) throws IOException {
        File oldObjectFile = new File (rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + oldName);
        File newObjectFile = new File (rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + newName);
        if(oldObjectFile.exists()) {
            if (!oldObjectFile.renameTo(newObjectFile)) {
                throw new IOException("Unable to rename " + oldObjectFile.getAbsolutePath() + " to " + newObjectFile.getAbsolutePath());
            }
        }
    }

    public void copyObject(String sourceBucket, String sourceObject, String destinationBucket, String destinationObject) throws IOException {
        File oldObjectFile = new File (rootDirectory + FILE_SEPARATOR + sourceBucket + FILE_SEPARATOR + sourceObject);
        File newObjectFile = new File (rootDirectory + FILE_SEPARATOR + destinationBucket + FILE_SEPARATOR + destinationObject);

        FileChannel fileIn = new FileInputStream(oldObjectFile).getChannel();
        FileChannel fileOut = new FileOutputStream(newObjectFile).getChannel();
        fileIn.transferTo(0, fileIn.size(), fileOut);
        fileIn.close();
        fileOut.close();
    }

    public String getObjectPath(String bucket, String object) {
        return rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object;
    }

    public long getObjectSize(String bucket, String object) {
        String absoluteObjectPath = rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object;

        File objectFile = new File(absoluteObjectPath);
        if(objectFile.exists())
            return objectFile.length();
        return -1;
    }

    public native String removeLoopback(String loDevName);

    public native int losetup(String absoluteFileName, String loDevName);

    public native String findFreeLoopback();

    public native String removeLogicalVolume(String lvName);

    public native String reduceVolumeGroup(String vgName, String pvName);

    public native String removePhysicalVolume(String loDevName);

    public native String createVolumeFromLv(String lvName, String volumeKey);

    public native String enableLogicalVolume(String lvName);

    public native String disableLogicalVolume(String lvName);

    public native String removeVolumeGroup(String vgName);

    public native String getLvmVersion();

    public String createLoopback(String fileName) throws EucalyptusCloudException {
        int number_of_retries = 0;
        int status = -1;
        String loDevName;
        do {
            loDevName = findFreeLoopback();
            if(loDevName.length() > 0) {
                status = losetup(fileName, loDevName);
            }
            if(number_of_retries++ >= MAX_LOOP_DEVICES)
                break;
        } while(status != 0);

        if(status != 0) {
            throw new EucalyptusCloudException("Could not create loopback device for " + fileName +
                    ". Please check the max loop value and permissions");
        }
        return loDevName;
    }

    public void deleteSnapshot(String bucket, String snapshotId, String vgName, String lvName, List<String> snapshotSet, boolean removeVg) throws EucalyptusCloudException {
        //load the snapshot set
        ArrayList<String> loDevices = new ArrayList<String>();
        String snapshotLoDev = null;
        String snapshotFileName = null;
        for(String snapshot : snapshotSet) {
            String fileName = rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + snapshot;
            String loDevName = createLoopback(fileName);
            if(loDevName.length() == 0)
                throw new EucalyptusCloudException("could not create loopback device for " + snapshot);
            if(snapshot.equals(snapshotId)) {
                snapshotLoDev = loDevName;
                snapshotFileName = fileName;
            }
            loDevices.add(loDevName);
        }
        //now remove the snapshot
        String absoluteLVName = lvmRootDirectory + FILE_SEPARATOR + vgName + FILE_SEPARATOR + lvName;
        String returnValue = removeLogicalVolume(absoluteLVName);
        if(returnValue.length() == 0) {
            throw new EucalyptusCloudException("Unable to remove logical volume " + absoluteLVName);
        }
        if(removeVg) {
            returnValue = removeVolumeGroup(vgName);
        } else {
            returnValue = reduceVolumeGroup(vgName, snapshotLoDev);
        }
        if(returnValue.length() == 0) {
            throw new EucalyptusCloudException("Unable to remove volume group " + vgName);
        }
        returnValue = removePhysicalVolume(snapshotLoDev);
        if(returnValue.length() == 0) {
            throw new EucalyptusCloudException("Unable to remove physical volume " + snapshotLoDev);
        }

        //unload the snapshots
        for(String loDevice : loDevices) {
            returnValue = removeLoopback(loDevice);
        }

        //remove the snapshot backing store
        try {
            deleteObject("", snapshotFileName);
        } catch(Exception ex) {
            throw new EucalyptusCloudException("could not delete snapshot file " + snapshotFileName);
        }
    }

    public String createVolume(String bucket, List<String> snapshotSet, List<String> vgNames, List<String> lvNames, String snapshotId, String snapshotVgName, String snapshotLvName) throws EucalyptusCloudException {
        String snapshotLoDev = null;
        ArrayList<String> loDevices = new ArrayList<String>();
        for(String snapshot : snapshotSet) {
            String loDevName = createLoopback(rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + snapshot);
            if(loDevName.length() == 0)
                throw new EucalyptusCloudException("could not create loopback device for " + snapshot);
            if(snapshot.equals(snapshotId))
                snapshotLoDev = loDevName;
            loDevices.add(loDevName);
        }

        //enable them
        int i = 0;
        ArrayList<String> absoluteLvNames = new ArrayList<String>();
        String snapshotAbsoluteLvName = null;
        for(String snapshot : snapshotSet) {
            String absoluteLvName = lvmRootDirectory + FILE_SEPARATOR + vgNames.get(i) + FILE_SEPARATOR + lvNames.get(i);
            if(i == 0)
                enableLogicalVolume(absoluteLvName);
            if(snapshotId.equals(snapshot))
                snapshotAbsoluteLvName = absoluteLvName;
            absoluteLvNames.add(absoluteLvName);
            ++i;
        }

        String volumeKey = "walrusvol-" + Hashes.getRandom(16);
        String volumePath = rootDirectory + FILE_SEPARATOR + bucket + FILE_SEPARATOR + volumeKey;
        createVolumeFromLv(snapshotAbsoluteLvName, volumePath);
        if(!(new File(volumePath).exists()))
            throw new EucalyptusCloudException("Unable to create file " + volumePath);

        for(String absoluteLvName : absoluteLvNames) {
            String returnValue = disableLogicalVolume(absoluteLvName);
        }

        //unload the snapshots
        for(String loDevice : loDevices) {
            String returnValue = removeLoopback(loDevice);
        }
        return volumeKey;
    }
}
