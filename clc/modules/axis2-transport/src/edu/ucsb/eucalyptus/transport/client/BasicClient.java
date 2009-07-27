/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.client;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.transport.config.Axis2OutProperties;
import edu.ucsb.eucalyptus.transport.config.Mep;
import edu.ucsb.eucalyptus.transport.util.Defaults;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
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

public class BasicClient implements Client {

  private static Logger LOG = Logger.getLogger( BasicClient.class );
  private String uri;
  private ServiceClient serviceClient;
  private HttpClient httpClient;
  private boolean valid;

  private Axis2OutProperties properties;

  public BasicClient( String uri, Axis2OutProperties properties ) throws AxisFault {
    this.valid = false;
    this.uri = uri;
    this.properties = properties;
    MultiThreadedHttpConnectionManager httpConnMgr = Defaults.getDefaultHttpManager();
    this.httpClient = new HttpClient( httpConnMgr );
    this.serviceClient = new ServiceClient( this.properties.getAxisContext(), null );
    this.valid = true;
  }

  public void activate() {
    try {
      this.serviceClient.cleanupTransport();
      this.httpClient.getHttpConnectionManager().closeIdleConnections( 0 );
      this.serviceClient.engageModule( Defaults.WS_SECURITY_MODULE );
      this.serviceClient.engageModule( Defaults.WS_ADDRESSING_MODULE );
    }
    catch ( AxisFault axisFault ) {
      LOG.error( axisFault, axisFault );
    }

    AxisOperation op = null;
    if ( this.properties.getMep().equals( Mep.OUT_ONLY ) )
      op = this.serviceClient.getAxisService().getOperation( ServiceClient.ANON_OUT_ONLY_OP );
    else if ( this.properties.getMep().equals( Mep.OUT_IN ) )
      op = this.serviceClient.getAxisService().getOperation( ServiceClient.ANON_OUT_IN_OP );
    else
      op = this.serviceClient.getAxisService().getOperation( ServiceClient.ANON_OUT_IN_OP );

    this.serviceClient.setOptions( this.getOptions() );

    op.getMessage( WSDLConstants.MESSAGE_LABEL_IN_VALUE ).getPolicySubject().clear();
    op.getMessage( WSDLConstants.MESSAGE_LABEL_OUT_VALUE ).getPolicySubject().clear();
    if ( this.properties.getOutPolicy() != null )
      op.getMessage( WSDLConstants.MESSAGE_LABEL_OUT_VALUE ).getPolicySubject().attachPolicy( this.properties.getOutPolicy() );
    if ( this.properties.getInPolicy() != null )
      op.getMessage( WSDLConstants.MESSAGE_LABEL_IN_VALUE ).getPolicySubject().attachPolicy( this.properties.getInPolicy() );

    this.valid = true;
  }

  private Options getOptions() {
    Options options = new Options();
    EndpointReference targetEndpoint = new EndpointReference( this.uri );
    options.setTo( targetEndpoint );
    options.setProperty( HTTPConstants.CHUNKED, Boolean.TRUE );
    options.setProperty( HTTPConstants.REUSE_HTTP_CLIENT, Boolean.TRUE );
    options.setProperty( HTTPConstants.CACHED_HTTP_CLIENT, this.httpClient );
    options.setTimeOutInMilliSeconds( this.properties.getTimeout() );
    return options;
  }

  public void passivate() throws AxisFault {
    this.serviceClient.cleanupTransport();
  }

  public void destroy() throws AxisFault {
    this.serviceClient.cleanupTransport();
    this.serviceClient.cleanup();
  }

  public EucalyptusMessage send( EucalyptusMessage msg ) throws AxisFault {
    OMElement omMsg = this.properties.getBinding().toOM( msg );
    LOG.trace( "Sending to " + this.uri + ": " + msg.getClass().getSimpleName() );
    LOG.trace( omMsg );
    OMElement omResponse = this.sync( omMsg );
    LOG.trace( omResponse );
    EucalyptusMessage msgResponse = null;
    try {
      Class targetClass = msg.getClass();
      while ( !targetClass.getSimpleName().endsWith( "Type" ) ) targetClass = targetClass.getSuperclass();
      Class responseClass = Class.forName( targetClass.getName().replaceAll( "Type", "" ) + "ResponseType" );
      msgResponse = ( EucalyptusMessage ) this.properties.getBinding().fromOM( omResponse, responseClass );
    }
    catch ( ClassNotFoundException e ) {
      LOG.error( e );
      LOG.debug( e, e );
    }
    return msgResponse;
  }

  public OMElement sync( OMElement omMsg ) throws AxisFault {
    this.activate();
    this.serviceClient.getOptions().setAction( omMsg.getLocalName() );
    OMElement omResponse = null;
    Exception ex = null;
    for ( int i = 0; i < 5; i++ ) {
      try {
        omResponse = this.serviceClient.sendReceive( omMsg );
        break;
      }
      catch ( RuntimeException e ) {
        LOG.error( String.format( EucalyptusProperties.DEBUG_FSTRING, omMsg.getLocalName()+":"+i, e.getMessage() ) );
        ex = e;
      }
      catch ( Exception e ) {
        LOG.error( String.format( EucalyptusProperties.DEBUG_FSTRING, omMsg.getLocalName()+":"+i, e.getMessage() ) );
        ex = e;
      }
    }
    if( omResponse == null ) {
      LOG.debug(ex,ex);
      throw AxisFault.makeFault( ex );
    }
    return omResponse;
  }

  public void dispatch( EucalyptusMessage msg ) throws AxisFault {
    OMElement omMsg = this.properties.getBinding().toOM( msg );
    this.async( omMsg );
  }

  public void async( OMElement omMsg ) throws AxisFault {
    long startTime = System.currentTimeMillis();
    this.activate();
    this.serviceClient.getOptions().setAction( omMsg.getLocalName() );
    try {
      this.serviceClient.fireAndForget( omMsg );
    }
    catch ( AxisFault axisFault ) {
      LOG.error( axisFault, axisFault );
      throw axisFault;
    }
  }

  public String getUri() {
    return uri;
  }

  public boolean isValid() {
    return valid;
  }

}
