/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.query;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

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

public class EucalyptusQuerySecurityHandler extends HMACQuerySecurityHandler {

  private static Logger LOG = Logger.getLogger( EucalyptusQuerySecurityHandler.class );

  public static SimpleDateFormat[] iso8601 = {
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'Z" )
  };

  public QuerySecurityHandler getInstance()
  {
    return new EucalyptusQuerySecurityHandler();
  }

  public String getName()
  {
    return "1";
  }

  public UserInfo authenticate( final HttpRequest httpRequest ) throws QuerySecurityException
  {
    return handle( "http://" + httpRequest.getHostAddr() + httpRequest.getRequestURL(), httpRequest.getHttpMethod(), httpRequest.getParameters(), httpRequest.getHeaders() );
  }

  public UserInfo handle(String urlString, String verb, Map<String, String> parameters, Map<String, String> headers ) throws QuerySecurityException
  {
    this.checkParameters( parameters );

    URL url = null;
    try {
      url = new URL( urlString );
    } catch ( MalformedURLException e ) {
      throw new QuerySecurityException( e.getMessage() );
    }
    String host = url.getHost();
    String addr = url.getPath();
    //:: check the signature :://
    String sig = parameters.remove( SecurityParameter.Signature.toString() );
    //:: check the signature version type here :://


    String queryId = parameters.get( SecurityParameter.AWSAccessKeyId.toString() );
    String queryKey = findQueryKey( queryId );

    String paramString = makeSubjectString( parameters );
    String paramString2 = makePlusSubjectString( parameters );

    LOG.info( "VERSION1-SIG1: " + paramString );
    LOG.info( "VERSION1-SIG2: " + paramString2 );

    String headerHost = headers.get( "Host" );
    String headerPort = "8773";
    if( headerHost != null && headerHost.contains( ":" ) ) {
      String[] hostTokens = headerHost.split( ":" );
      headerHost = hostTokens[0];
      if( hostTokens.length > 1 && hostTokens[1] != null && "".equals( hostTokens[1] ) ) {
        headerPort = hostTokens[1];
      }
    }
//    String paramString3 = makeV2SubjectString( verb, host, addr, parameters ); this should never work...
    String paramString3 = makeV2SubjectString( verb, headerHost, addr, parameters );
    String paramString4 = makeV2SubjectString( verb, headerHost+":"+headerPort, addr, parameters );
    String paramString5 = makeV2SubjectString( verb, headerHost, addr.replaceFirst("/services",""), parameters );

    String authSig = checkSignature( queryKey, paramString );
    String authSig2 = checkSignature( queryKey, paramString2 );

    String authv2sha256 = checkSignature256( queryKey, paramString3 );
    String authv2sha256port = checkSignature256( queryKey, paramString4 );
    String authv2sha256typica = checkSignature256( queryKey, paramString5 );
    LOG.info( "VERSION2-SHA256:        " + authv2sha256 + " -- " + sig );
    LOG.info( "VERSION2-SHA256-HEADER: " + authv2sha256port + " -- " + sig );
    LOG.info( "VERSION2-SHA256-TYPICA: " + authv2sha256typica + " -- " + sig );

    if ( !authSig.equals( sig ) && !authSig2.equals( sig ) && !authv2sha256.equals( sig ) && !authv2sha256port.equals( sig ) && !authv2sha256typica.equals( sig ) )
      throw new QuerySecurityException( "User authentication failed." );

    //:: check the timestamp :://
    Calendar now = Calendar.getInstance();
    Calendar expires = null;
    if ( parameters.containsKey( SecurityParameter.Timestamp.toString() ) )
    {
      String timestamp = parameters.remove( SecurityParameter.Timestamp.toString() );
      expires = parseTimestamp( timestamp );
      expires.add( Calendar.MINUTE, 5 );
    }
    else
    {
      String exp = parameters.remove( SecurityParameter.Expires.toString() );
      expires = parseTimestamp( exp );
    }
    if ( now.after( expires ) )
      throw new QuerySecurityException( "Message has expired." );

    for ( Axis2QueryDispatcher.OperationParameter op : Axis2QueryDispatcher.OperationParameter.values() ) parameters.remove( op.name() );
    parameters.remove( Axis2QueryDispatcher.RequiredQueryParams.SignatureVersion.toString() );
    parameters.remove( Axis2QueryDispatcher.RequiredQueryParams.Version.toString() );
    parameters.remove( "SignatureMethod" );

    return findUserId( parameters.remove( SecurityParameter.AWSAccessKeyId.toString() ) );
  }

  private void checkParameters( final Map<String, String> parameters ) throws QuerySecurityException
  {
    if ( !parameters.containsKey( SecurityParameter.AWSAccessKeyId.toString() ) )
      throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.AWSAccessKeyId );
    if ( !parameters.containsKey( SecurityParameter.Signature.toString() ) )
      throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.Signature );
    if ( !parameters.containsKey( SecurityParameter.Timestamp.toString() )
         && !parameters.containsKey( SecurityParameter.Expires.toString() ) )
      throw new QuerySecurityException( "One of the following parameters must be specified: " + SecurityParameter.Timestamp + " OR " + SecurityParameter.Expires );
  }

}