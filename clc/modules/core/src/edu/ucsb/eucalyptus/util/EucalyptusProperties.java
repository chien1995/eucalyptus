/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.util;

import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

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

public class EucalyptusProperties {

  private static Logger LOG = Logger.getLogger( EucalyptusProperties.class );

  public static List<String> getDisabledOperations() {
    if( System.getProperty("euca.ebs.disable") != null ) {
      disableBlockStorage = true;
      return Lists.newArrayList( "CreateVolume", "DeleteVolume", "DescribeVolumes", "AttachVolume", "DetachVolume",
                                 "CreateSnapshot", "DeleteSnapshot", "DescribeSnapshots" );
    }
    return Lists.newArrayList(  );
  }

  public static boolean disableNetworking = false;
  public static boolean disableBlockStorage = false;

  public static String NAME = "eucalyptus";
  public static String WWW_NAME = "jetty";
  public static String NETWORK_DEFAULT_NAME = "default";
  public static String DEBUG_FSTRING = "[%12s] %s";
  public static String CLUSTERSINK_REF = "vm://ClusterSink";

  public enum NETWORK_PROTOCOLS {

    tcp, udp, icmp
  }

  public static String NAME_SHORT = "euca2";
  public static String FSTRING = "::[ %-20s: %-50.50s ]::\n";
  public static String IMAGE_MACHINE = "machine";
  public static String IMAGE_KERNEL = "kernel";
  public static String IMAGE_RAMDISK = "ramdisk";
  public static String IMAGE_MACHINE_PREFIX = "emi";
  public static String IMAGE_KERNEL_PREFIX = "eki";
  public static String IMAGE_RAMDISK_PREFIX = "eri";

  public static String getDName( String name ) {
    return String.format( "CN=www.eucalyptus.com, OU=Eucalyptus, O=%s, L=Santa Barbara, ST=CA, C=US", name );
  }

  public static SystemConfiguration getSystemConfiguration() throws EucalyptusCloudException {
    EntityWrapper<SystemConfiguration> confDb = new EntityWrapper<SystemConfiguration>();
    SystemConfiguration conf = null;
    try {
      conf = confDb.getUnique( new SystemConfiguration() );
    }
    catch ( EucalyptusCloudException e ) {
      confDb.rollback();
      throw new EucalyptusCloudException( "Failed to load system configuration", e );
    }
    if( conf.getRegistrationId() == null ) {
      conf.setRegistrationId( UUID.randomUUID().toString() );
    }
    if( conf.getSystemReservedPublicAddresses() == null ) {
      conf.setSystemReservedPublicAddresses( 10 );
    }
    if( conf.getMaxUserPublicAddresses() == null ) {
      conf.setMaxUserPublicAddresses( 5 );
    }
    if( conf.isDoDynamicPublicAddresses() == null ) {
      conf.setDoDynamicPublicAddresses( true );
    }
    confDb.commit();
    String walrusUrl = null;
    try {
      walrusUrl = ( new URL( conf.getStorageUrl() + "/" ) ).toString();
    }
    catch ( MalformedURLException e ) {
      throw new EucalyptusCloudException( "System is misconfigured: cannot parse Walrus URL.", e );
    }

    return conf;
  }

  public enum TokenState {

    preallocate, returned, accepted, submitted, allocated, redeemed;
  }
}
