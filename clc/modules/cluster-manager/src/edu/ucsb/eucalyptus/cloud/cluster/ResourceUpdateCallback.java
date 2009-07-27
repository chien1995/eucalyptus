package edu.ucsb.eucalyptus.cloud.cluster;
/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
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

public class ResourceUpdateCallback extends QueuedEventCallback<DescribeResourcesType> implements Runnable {

  private static Logger LOG = Logger.getLogger( ResourceUpdateCallback.class );

  private Cluster parent;
  private static int SLEEP_TIMER = 3 * 1000;
  private boolean firstTime = true;

  public ResourceUpdateCallback( final Cluster parent ) {
    this.parent = parent;
  }

  public void process( final Client cluster, final DescribeResourcesType msg ) throws Exception {
    DescribeResourcesResponseType reply = ( DescribeResourcesResponseType ) cluster.send( msg );
    parent.getNodeState().update( reply.getResources() );
    LOG.debug("Adding node service tags: " + reply.getServiceTags() );
    parent.updateNodeInfo( reply.getServiceTags() );
//    if ( !parent.getNodeTags().isEmpty() && this.firstTime ) {
//      this.firstTime = false;
//      this.parent.fireNodeThreads();
//    }
  }

  public void run() {
    do {
      DescribeResourcesType drMsg = new DescribeResourcesType();
      drMsg.setUserId( EucalyptusProperties.NAME );
      drMsg.setEffectiveUserId( EucalyptusProperties.NAME );
      for ( VmType v : VmTypes.list() ) drMsg.getInstanceTypes().add( v.getAsVmTypeInfo() );

      this.parent.getMessageQueue().enqueue( new QueuedEvent( this, drMsg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );

  }

}
