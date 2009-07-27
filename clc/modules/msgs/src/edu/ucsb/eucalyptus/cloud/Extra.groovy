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

package edu.ucsb.eucalyptus.cloud
/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

import edu.ucsb.eucalyptus.constants.HasName
import edu.ucsb.eucalyptus.msgs.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

public class Pair {

  public static List<Pair> getPaired(List one, List two) {
    List<Pair> newList = new ArrayList<Pair>();
    for ( int idx = 0; idx < one.size(); idx++ )
      newList.add(new Pair(one[ idx ], two[ idx ]));
    return newList;
  }

  def String left, right;

  def Pair(final left, final right) {
    this.left = left;
    this.right = right;
  }

}
public interface RequestTransactionScript extends Serializable {
  public EucalyptusMessage getRequestMessage();
}
public class VmAllocationInfo implements RequestTransactionScript {

  RunInstancesType request;
  RunInstancesResponseType reply;
  String userData;
  Long reservationIndex;
  String reservationId;
  VmImageInfo imageInfo;
  VmKeyInfo keyInfo;
  VmTypeInfo vmTypeInfo;

  List<Network> networks = new ArrayList<Network>();

  List<ResourceToken> allocationTokens = new ArrayList<ResourceToken>();
  List<String> addresses = new ArrayList<String>();

  def VmAllocationInfo() {}

  def VmAllocationInfo(final RunInstancesType request) {
    this.request = request;
    this.reply = request.getReply();
  }

  public EucalyptusMessage getRequestMessage() {
    return this.getRequest();
  }
}

public class VmDescribeType extends EucalyptusMessage {

  ArrayList<String> instancesSet = new ArrayList<String>();
}
public class VmDescribeResponseType extends EucalyptusMessage {

  String originCluster;
  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmRunResponseType extends EucalyptusMessage {

  ArrayList<VmInfo> vms = new ArrayList<VmInfo>();
}

public class VmInfo extends EucalyptusData {

  String imageId;
  String kernelId;
  String ramdiskId;
  String instanceId;
  VmTypeInfo instanceType = new VmTypeInfo();
  String keyValue;
  Date launchTime;
  String stateName;
  NetworkConfigType netParams = new NetworkConfigType();
  String ownerId;
  String reservationId;
  String serviceTag;
  String userData;
  String launchIndex;
  ArrayList<String> groupNames = new ArrayList<String>();
  ArrayList<AttachedVolume> volumes = new ArrayList<AttachedVolume>();

  String placement;

  ArrayList<String> productCodes = new ArrayList<String>();
}

public class VmRunType extends EucalyptusMessage {

  /** these are for more convenient binding later on but really should be done differently... sigh    **/

  String reservationId, userData;
  int min, max, vlan, launchIndex;

  VmImageInfo imageInfo;
  VmTypeInfo vmTypeInfo;
  VmKeyInfo keyInfo;

  List<String> instanceIds = new ArrayList<String>();
  List<String> macAddresses = new ArrayList<String>();
  List<String> networkNames = new ArrayList<String>();


  def VmRunType() {}

  def VmRunType(final RunInstancesType request,
                final String reservationId, final String userData, final int amount,
                final VmImageInfo imageInfo, final VmTypeInfo vmTypeInfo, final VmKeyInfo keyInfo,
                final List<String> instanceIds, final List<String> macAddresses,
                final int vlan, final List<String> networkNames) {
    this.correlationId = request.correlationId;
    this.userId = request.userId;
    this.effectiveUserId = request.effectiveUserId;
    this.reservationId = reservationId;
    this.userData = userData;
    this.min = amount;
    this.max = amount;
    this.vlan = vlan;
    this.imageInfo = imageInfo;
    this.vmTypeInfo = vmTypeInfo;
    this.keyInfo = keyInfo;
    this.instanceIds = instanceIds;
    this.macAddresses = macAddresses;
    this.networkNames = networkNames;
  }

  def VmRunType(RunInstancesType request) {
    this.effectiveUserId = request.effectiveUserId;
    this.correlationId = request.correlationId;
    this.userId = request.userId;
  }




  public String toString() {
    /** TODO-1.4: do something reasonable here    **/
    return this.correlationId;
  }

}

public class VmImageInfo {

  String imageId;
  String kernelId;
  String ramdiskId;
  String imageLocation;
  String kernelLocation;
  String ramdiskLocation;
  ArrayList<String> productCodes = new ArrayList<String>();
  ArrayList<String> ancestorIds = new ArrayList<String>();
  Long size = 0l;

  def VmImageInfo(final imageId, final kernelId, final ramdiskId, final imageLocation, final kernelLocation, final ramdiskLocation, final productCodes) {
    this.imageId = imageId;
    this.kernelId = kernelId;
    this.ramdiskId = ramdiskId;
    this.imageLocation = imageLocation;
    this.kernelLocation = kernelLocation;
    this.ramdiskLocation = ramdiskLocation;
    this.productCodes = productCodes;
  }

  def VmImageInfo() {}

}

public class VmKeyInfo {

  String name = "";
  String value = "";
  String fingerprint = "";

  def VmKeyInfo(final name, final value, final fingerprint) {
    this.name = name;
    this.value = value;
    this.fingerprint = fingerprint;
  }

  def VmKeyInfo() {}

}

public class Network implements HasName {

  String name;
  String networkName;
  String userName;
  ArrayList<PacketFilterRule> rules = new ArrayList<PacketFilterRule>();
  ConcurrentMap<String, NetworkToken> networkTokens = new ConcurrentHashMap<String, NetworkToken>();

  def Network() {}

  def Network(final String userName, final String networkName) {
    this.userName = userName;
    this.networkName = networkName;
    this.name = this.userName + "-" + this.networkName;
  }


  public NetworkToken addTokenIfAbsent(NetworkToken token) {
    this.networkTokens.putIfAbsent(token.getCluster(), token);
  }

  public NetworkToken getToken(String cluster) {
    return this.networkTokens.get(cluster);
  }

  public boolean hasToken(String cluster) {
    return this.networkTokens.get(cluster);
  }


  public NetworkToken removeToken(String cluster) {
    return this.networkTokens.remove(cluster);
  }

  public boolean isPeer( String peerName, String peerNetworkName ) {
    return (Boolean) this.rules.collect{ pf -> pf.peers.contains( new VmNetworkPeer( peerName, peerNetworkName )) }.max();
  }

  public int compareTo(final Object o) {
    Network that = (Network) o;
    return this.getName().compareTo(that.getName());
  }

  @Override
  public String toString() {
    return "Network{" +
           "name='" + name + '\'' +
           ", networkName='" + networkName + '\'' +
           ", userName='" + userName + '\'' +
           ", rules=" + rules +
           ", networkTokens=" + networkTokens +
           '}';
  }

}

public class NetworkToken implements Comparable {

  String networkName;
  String cluster;
  int vlan;
  String userName;
  String name;

  def NetworkToken(final String cluster, final String userName, final String networkName, final int vlan) {
    this.networkName = networkName;
    this.cluster = cluster;
    this.vlan = vlan;
    this.userName = userName;
    this.name = this.userName + "-" + this.networkName;
  }

  @Override
  boolean equals(final Object o) {
    if ( this == o ) return true;
    if ( !(o instanceof NetworkToken) ) return false;
    NetworkToken that = (NetworkToken) o;

    if ( !cluster.equals(that.cluster) ) return false;
    if ( !networkName.equals(that.networkName) ) return false;
    if ( !userName.equals(that.userName) ) return false;

    return true;
  }

  @Override
  int hashCode() {
    int result;

    result = networkName.hashCode();
    result = 31 * result + cluster.hashCode();
    result = 31 * result + userName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "NetworkToken{" +
           "networkName='" + networkName + '\'' +
           ", cluster='" + cluster + '\'' +
           ", vlan=" + vlan +
           ", userName='" + userName + '\'' +
           ", name='" + name + '\'' +
           '}';
  }

  @Override
  public int compareTo(Object o) {
    NetworkToken that = (NetworkToken) o;
    return (!this.cluster.equals(that.cluster) && (this.vlan == that.vlan)) ? this.vlan - that.vlan : this.cluster.compareTo(that.cluster);
  }

  public StopNetworkType getStopMessage() {
    return new StopNetworkType(this.vlan, this.name);
  }

}

public class ResourceToken implements Comparable {

  String cluster;
  String correlationId;
  String userName;
  ArrayList<String> instanceIds = new ArrayList<String>();
  ArrayList<String> addresses = new ArrayList<String>();
  ArrayList<NetworkToken> networkTokens = new ArrayList<NetworkToken>();
  int amount;
  String vmType;
  Date creationTime;
  int sequenceNumber;

  public ResourceToken(final String cluster, final String correlationId, final String userName, final int amount, final int sequenceNumber, final String vmType) {
    this.cluster = cluster;
    this.correlationId = correlationId;
    this.userName = userName;
    this.amount = amount;
    this.sequenceNumber = sequenceNumber;
    this.creationTime = Calendar.getInstance().getTime();
    this.vmType = vmType;
  }



  @Override
  public boolean equals(final Object o) {
    if ( this == o ) return true;
    if ( !(o instanceof ResourceToken) ) return false;

    ResourceToken that = (ResourceToken) o;

    if ( amount != that.amount ) return false;
    if ( !cluster.equals(that.cluster) ) return false;
    if ( !correlationId.equals(that.correlationId) ) return false;
    if ( !creationTime.equals(that.creationTime) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = cluster.hashCode();
    result = 31 * result + correlationId.hashCode();
    result = 31 * result + amount;
    result = 31 * result + creationTime.hashCode();
    return result;
  }

  @Override
  public int compareTo(final Object o) {
    ResourceToken that = (ResourceToken) o;
    return this.getSequenceNumber() - that.getSequenceNumber();
  }

  @Override
  public String toString() {
    return String.format("ResourceToken={ cluster=%10s, vmType=%10s, amount=%04d ]", this.cluster, this.vmType, this.amount);
  }

}

public class NodeInfo implements Comparable {

  String serviceTag;
  String name;
  Date lastSeen;
  NodeCertInfo certs = new NodeCertInfo();
  NodeLogInfo logs = new NodeLogInfo();

  @Override
  public String toString() {
    return "NodeInfo{" +
           "serviceTag='" + serviceTag.replaceAll("services/EucalyptusNC","") + '\'' +
           ", name='" + name + '\'' +
           ", lastSeen=" + lastSeen +
           ", certs=" + certs +
           ", logs=" + logs +
           '}';
  }

  def NodeInfo(final String serviceTag) {
    this.name = (new URI(serviceTag)).getHost();
    this.serviceTag = serviceTag;
    this.lastSeen = new Date();
    this.certs.setServiceTag(this.serviceTag);
    this.logs.setServiceTag(this.serviceTag);
  }



  def NodeInfo(NodeInfo orig, final NodeCertInfo certs) {
    this(orig.serviceTag);
    this.logs = orig.logs;
    this.certs = certs;
  }

  def NodeInfo(NodeInfo orig, final NodeLogInfo logs) {
    this(orig.serviceTag);
    this.certs = orig.certs;
    this.logs = logs;
  }



  def NodeInfo(final NodeCertInfo certs) {
    this(certs.getServiceTag());
    this.certs = certs;
  }

  def NodeInfo(final NodeLogInfo logs) {
    this(logs.getServiceTag());
    this.logs = logs;
  }



  public void touch() {
    this.lastSeen = new Date();
  }

  public int compareTo(Object o) {
    return this.serviceTag.compareTo(((NodeInfo) o).serviceTag);
  }

  boolean equals(final o) {
    if ( this == o ) return true;
    if ( !(o instanceof NodeInfo) ) return false;
    NodeInfo nodeInfo = (NodeInfo) o;
    if ( !serviceTag.equals(nodeInfo.serviceTag) ) return false;
    return true;
  }

  int hashCode() {
    return serviceTag.hashCode();
  }

}
