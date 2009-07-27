/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.util;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;
import org.apache.log4j.Logger;

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

public class WalrusProperties {
    private static Logger LOG = Logger.getLogger( WalrusProperties.class );

    public static final String SERVICE_NAME = "Walrus";
    public static final long G = 1024*1024*1024;
    public static final long M = 1024*1024;
    public static final long K = 1024;

    public static String bucketRootDirectory = BaseDirectory.VAR.toString() + "/bukkits";
    public static int MAX_BUCKETS_PER_USER = 5;
    public static long MAX_BUCKET_SIZE = 5 * G;
    public static long IMAGE_CACHE_SIZE = 30 * G;
    public static String WALRUS_URL;
    public static int MAX_TOTAL_SNAPSHOT_SIZE = 50;
    public static int MAX_KEYS = 1000;

    public static final int IO_CHUNK_SIZE = 102400;


    public static void update() {
        try {
            SystemConfiguration systemConfiguration = EucalyptusProperties.getSystemConfiguration();
            bucketRootDirectory = systemConfiguration.getStorageDir();
            MAX_BUCKETS_PER_USER = systemConfiguration.getStorageMaxBucketsPerUser();
            MAX_BUCKET_SIZE = systemConfiguration.getStorageMaxBucketSizeInMB() * M;
            IMAGE_CACHE_SIZE = systemConfiguration.getStorageMaxCacheSizeInMB() * M;
            WALRUS_URL = systemConfiguration.getStorageUrl();
            Integer maxTotalSnapSize = systemConfiguration.getStorageMaxTotalSnapshotSizeInGb();
            if(maxTotalSnapSize != null) {
                if(maxTotalSnapSize > 0) {
                    MAX_TOTAL_SNAPSHOT_SIZE = maxTotalSnapSize;
                }
            }
            UpdateWalrusConfigurationType updateConfig = new UpdateWalrusConfigurationType();
            updateConfig.setBucketRootDirectory(bucketRootDirectory);
            Messaging.send( WALRUS_REF, updateConfig );
        } catch(Exception ex) {
            LOG.warn(ex.getMessage());
        }
    }

    public static final String URL_PROPERTY = "euca.walrus.url";
    public static final String USAGE_LIMITS_PROPERTY = "euca.walrus.usageLimits";
    public static final String WALRUS_OPERATION = "WalrusOperation";
    public static final String AMZ_META_HEADER_PREFIX = "x-amz-meta-";
    public static final String STREAMING_HTTP_GET = "STREAMING_HTTP_GET";
    public static final String STREAMING_HTTP_PUT = "STREAMING_HTTP_PUT";
    public static final String AMZ_ACL = "x-amz-acl";
    public static final String ALL_USERS_GROUP = "http://acs.amazonaws.com/groups/global/AllUsers";
    public static final String AUTHENTICATED_USERS_GROUP = "'http://acs.amazonaws.com/groups/global/AuthenticatedUsers";

    public static final String IGNORE_PREFIX = "x-ignore-";
    public static final String COPY_SOURCE = "x-amz-copy-source";
    public static final String METADATA_DIRECTIVE = "x-amz-metadata-directive";
    public static final String ADMIN = "admin";
    public static String WALRUS_REF = "vm://BukkitInternal";

    public enum Headers {
        Bucket, Key, RandomKey, VolumeId
    }

    public enum ExtendedGetHeaders {
        IfModifiedSince, IfUnmodifiedSince, IfMatch, IfNoneMatch, Range
    }

    public enum ExtendedHeaderDateTypes {
        IfModifiedSince, IfUnmodifiedSince, CopySourceIfModifiedSince, CopySourceIfUnmodifiedSince;

        public static boolean contains(String value) {
            for(ExtendedHeaderDateTypes type: values()) {
                if(type.toString().equals(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum ExtendedHeaderRangeTypes {
        ByteRangeStart, ByteRangeEnd
    }

    public enum WalrusInternalOperations {
        GetDecryptedImage
    }

    public enum GetOptionalParameters {
        IsCompressed
    }

    public enum StorageOperations {
        StoreSnapshot, DeleteWalrusSnapshot, GetSnapshotInfo, GetVolume
    }

    public enum InfoOperations {
        GetSnapshotInfo
    }

    public enum StorageParameters {
        SnapshotVgName, SnapshotLvName
    }

    public enum FormField {
        FormUploadPolicyData, AWSAccessKeyId, key, bucket, acl, policy, success_action_redirect, success_action_status, signature, file
    }

    public enum IgnoredFields {
        AWSAccessKeyId, signature, file, policy, submit
    }

    public enum PolicyHeaders {
        expiration, conditions
    }

    public enum CopyHeaders {
        CopySourceIfMatch, CopySourceIfNoneMatch, CopySourceIfUnmodifiedSince, CopySourceIfModifiedSince
    }
}