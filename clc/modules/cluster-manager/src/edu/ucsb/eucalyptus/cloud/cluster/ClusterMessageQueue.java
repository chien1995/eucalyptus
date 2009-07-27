/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.entities.ClusterInfo;
import edu.ucsb.eucalyptus.transport.Axis2MessageDispatcher;
import edu.ucsb.eucalyptus.transport.client.*;
import edu.ucsb.eucalyptus.transport.config.Key;
import edu.ucsb.eucalyptus.transport.util.Defaults;
import edu.ucsb.eucalyptus.util.BaseDirectory;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

public class ClusterMessageQueue implements Runnable {

  private static Logger LOG = Logger.getLogger( ClusterMessageQueue.class );
  private Cluster parent;
  private BasicClient client;
  private BasicClient logClient;
  private BlockingQueue<QueuedEvent> msgQueue;
  private int offerInterval = 10;
  private int pollInterval = 10;
  private final int messageQueueSize = 100;
  private AtomicBoolean finished;

  public ClusterMessageQueue( Cluster parent )
  {
    this.parent = parent;
    this.finished = new AtomicBoolean( false );
    this.msgQueue = new LinkedBlockingQueue<QueuedEvent>(messageQueueSize);
    OutboundEndpoint endpoint = Defaults.getDefaultOutboundEndpoint( this.parent.getClusterInfo().getUri(), ClusterInfo.NAMESPACE, 15, 10, 20 );
    Axis2MessageDispatcherFactory clientFactory = new Axis2MessageDispatcherFactory();
    try
    {
      Axis2MessageDispatcher dispatcher = ( Axis2MessageDispatcher ) clientFactory.create( endpoint );
      this.client = dispatcher.getClient();
    }
    catch ( MuleException e )
    {
      LOG.error( e, e );
    }
    OutboundEndpoint logEndpoint = Defaults.getDefaultOutboundEndpoint( this.parent.getClusterInfo().getUri().replaceAll( "EucalyptusCC", "EucalyptusGL" ), ClusterInfo.NAMESPACE, 15, 10, 20 );
    logEndpoint.getProperties().remove( Key.WSSEC_POLICY.getKey() );
    logEndpoint.getProperties().put( Key.WSSEC_POLICY.getKey(), BaseDirectory.CONF.toString() + File.separator + "off-policy.xml" );
    try
    {
      Axis2MessageDispatcher dispatcher = ( Axis2MessageDispatcher ) clientFactory.create( logEndpoint );
      this.logClient = dispatcher.getClient();
    }
    catch ( MuleException e )
    {
      LOG.error( e, e );
    }
    LOG.info( "Created message queue for cluster " + this.parent.getClusterInfo().getName() );

  }

  public void enqueue( QueuedEvent event )
  {
    LOG.info( "Queued message of type " + event.getCallback().getClass().getSimpleName() + " for cluster " + this.parent.getClusterInfo().getName() );
    boolean inserted = false;
    while ( !inserted )
      try
      {
        if ( this.msgQueue.contains( event ) ) return;
        if(this.msgQueue.offer( event, offerInterval, TimeUnit.MILLISECONDS ))
	        inserted = true;
      }
      catch ( InterruptedException e )
      {
        LOG.error( e, e );
      }
  }

  public void stop()
  {
    this.finished.lazySet( true );
  }

  public void run()
  {
    while ( !finished.get() )
      try
      {
        long start = System.currentTimeMillis();
        //:: consume a message from the request queue :://
        QueuedEvent event = this.msgQueue.poll( pollInterval, TimeUnit.MILLISECONDS );
        if ( event != null ) // msg == null if the queue was empty and we timed out
        {
          LOG.trace( "Dequeued message of type " + event.getCallback().getClass().getSimpleName() );
          long msgStart = System.currentTimeMillis();
          try {
            event.trigger( event instanceof QueuedLogEvent ? this.logClient : this.client );
            this.parent.setReachable( true );
          } catch ( Exception e ) {
            LOG.error( e );
            this.parent.setReachable( false );
          } finally {
            event.getCallback().notifyHandler();
          }
          LOG.warn( String.format( "[q=%04dms,send=%04dms,qlen=%02d] message type %s, cluster %s",
                                   msgStart -start, System.currentTimeMillis() - msgStart, this.msgQueue.size(),
                                   event.getCallback().getClass().getSimpleName(), this.parent.getClusterInfo().getName() ) );
        }
      }
      catch ( Exception e )
      {
        LOG.error( e, e );
      }
  }

  @Override
  public String toString() {
    return "ClusterMessageQueue{" +
           "msgQueue=" + msgQueue.size() +
           '}';
  }
}
