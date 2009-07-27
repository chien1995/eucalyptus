package edu.ucsb.eucalyptus.admin.server;
/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */
import com.google.common.collect.Sets;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.cluster.Clusters;
import edu.ucsb.eucalyptus.cloud.cluster.VmTypes;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.ClusterStateType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import edu.ucsb.eucalyptus.util.Messaging;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

public class RemoteInfoHandler {

  private static Logger LOG = Logger.getLogger( RemoteInfoHandler.class );

  public static synchronized void setClusterList( List<ClusterInfoWeb> newClusterList )
  {
    List<ClusterStateType> list = new ArrayList<ClusterStateType>();
    for ( ClusterInfoWeb cw : newClusterList ) {
      LOG.info( "Adding cluster for update: " + cw.getName() + " - " + cw.getHost() + ":" + cw.getPort() );
      list.add( new ClusterStateType( cw.getName(), cw.getHost(), cw.getPort() ) );
    }
    Messaging.dispatch( EucalyptusProperties.CLUSTERSINK_REF, list );
  }

  public static synchronized List<ClusterInfoWeb> getClusterList()
  {
    List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>();
    for ( ClusterStateType c : Clusters.getInstance().getClusters() )
      clusterList.add( new ClusterInfoWeb( c.getName(), c.getHost(), c.getPort(), "/foo/bar", 0, 0) ); // TODO Sunil: add SC configuration params
    return clusterList;
  }

  public static List<VmTypeWeb> getVmTypes()
  {
    List<VmTypeWeb> ret = new ArrayList<VmTypeWeb>();
    for( VmType v : VmTypes.list() )
      ret.add( new VmTypeWeb( v.getName(), v.getCpu(), v.getMemory(), v.getDisk() ) );
    return ret;
  }

  public static void setVmTypes( final List<VmTypeWeb> vmTypes ) throws SerializableException {
    Set<VmType> newVms = Sets.newTreeSet();
    for ( VmTypeWeb vmw : vmTypes ) {
      newVms.add( new VmType( vmw.getName(), vmw.getCpu(), vmw.getDisk(), vmw.getMemory() ) );
    }
    try {
      VmTypes.update( newVms );
    }
    catch ( EucalyptusCloudException e ) {
      throw new SerializableException( e.getMessage() );
    }
  }
}
