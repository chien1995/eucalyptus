package edu.ucsb.eucalyptus.cloud.cluster;
/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.ResourceType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;
import org.apache.log4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

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

public class ClusterNodeState {
  private static Logger LOG = Logger.getLogger( ClusterNodeState.class );
  private Cluster parent;
  private ConcurrentNavigableMap<String, VmTypeAvailability> typeMap;
  private NavigableSet<ResourceToken> pendingTokens;
  private NavigableSet<ResourceToken> submittedTokens;
  private NavigableSet<ResourceToken> redeemedTokens;
  private int virtualTimer;

  public ClusterNodeState( Cluster parent ) {
    this.parent = parent;
    this.typeMap = new ConcurrentSkipListMap<String, VmTypeAvailability>();

    for ( VmType v : VmTypes.list() )
      this.typeMap.putIfAbsent( v.getName(), new VmTypeAvailability( v, 0, 0 ) );

    this.pendingTokens = new ConcurrentSkipListSet<ResourceToken>();
    this.submittedTokens = new ConcurrentSkipListSet<ResourceToken>();
    this.redeemedTokens = new ConcurrentSkipListSet<ResourceToken>();
  }

  public synchronized ResourceToken getResourceAllocation( String requestId, String userName, String vmTypeName, Integer quantity ) throws NotEnoughResourcesAvailable {
    VmTypeAvailability vmType = this.typeMap.get( vmTypeName );
    NavigableSet<VmTypeAvailability> sorted = this.sorted();

    LOG.warn( "BEFORE ALLOCATE ============================" );
    LOG.warn( sorted );
    //:: if not enough, then bail out :://
    if ( vmType.getAvailable() < quantity ) throw new NotEnoughResourcesAvailable();

    Set<VmTypeAvailability> tailSet = sorted.tailSet( vmType );
    Set<VmTypeAvailability> headSet = sorted.headSet( vmType );
    LOG.warn( "DURING ALLOCATE ============================" );
    LOG.warn("TAILSET: " + tailSet );
    LOG.warn("HEADSET: " + headSet );
    //:: decrement available resources across the "active" partition :://
    for ( VmTypeAvailability v : tailSet )
      v.decrement( quantity );
    for ( VmTypeAvailability v : headSet )
      v.setAvailable( vmType.getAvailable() );
    LOG.warn( "AFTER ALLOCATE ============================" );
    LOG.warn( sorted );

    ResourceToken token = new ResourceToken( this.parent.getName(), requestId, userName, quantity, this.virtualTimer++, vmTypeName );
    this.pendingTokens.add( token );
    return token;
  }

  public synchronized void releaseToken( ResourceToken token ) {
    this.pendingTokens.remove( token );
    this.submittedTokens.remove( token );
  }

  public synchronized void submitToken( ResourceToken token ) throws NoSuchTokenException {
    if ( this.pendingTokens.remove( token ) )
      this.submittedTokens.add( token );
    else
      throw new NoSuchTokenException();
  }

  public synchronized void redeemToken( ResourceToken token ) throws NoSuchTokenException {
    if ( this.submittedTokens.remove( token ) )
      this.redeemedTokens.add( token );
    else
      throw new NoSuchTokenException();
  }


  public synchronized void update( List<ResourceType> rscUpdate ) {
    int outstandingCount = 0;
    for( ResourceToken t : this.pendingTokens )
      outstandingCount += t.getAmount();
    for( ResourceToken t : this.submittedTokens )
      outstandingCount += t.getAmount();
    for( ResourceToken t : this.redeemedTokens )
      outstandingCount += t.getAmount();
    this.redeemedTokens.clear();

    for ( ResourceType rsc : rscUpdate ) {
      VmTypeAvailability vmAvailable = this.typeMap.get( rsc.getInstanceType().getName() );
      if ( vmAvailable == null ) continue;
      vmAvailable.setAvailable( rsc.getAvailableInstances() );
      vmAvailable.decrement( outstandingCount );
      vmAvailable.setMax( rsc.getMaxInstances() );
    }
  }

  private NavigableSet<VmTypeAvailability> sorted() {
    NavigableSet<VmTypeAvailability> available = new TreeSet<VmTypeAvailability>();
    for ( String typeName : this.typeMap.keySet() )
      available.add( this.typeMap.get( typeName ) );
    available.add( VmTypeAvailability.ZERO );
    LOG.debug("Resource information for " + this.parent.getName() );
    return available;
  }

  public VmTypeAvailability getAvailability( String vmTypeName ) {
    return this.typeMap.get( vmTypeName );
  }

  public static ResourceComparator getComparator( VmTypeInfo vmTypeInfo ) {
    return new ResourceComparator( vmTypeInfo );
  }

  public static class ResourceComparator implements Comparator<ClusterNodeState> {

    private VmTypeInfo vmTypeInfo;

    ResourceComparator( final VmTypeInfo vmTypeInfo ) {
      this.vmTypeInfo = vmTypeInfo;
    }

    public int compare( final ClusterNodeState o1, final ClusterNodeState o2 ) {
      return o1.getAvailability( this.vmTypeInfo.getName() ).getAvailable() - o2.getAvailability( this.vmTypeInfo.getName() ).getAvailable();
    }
  }

}
