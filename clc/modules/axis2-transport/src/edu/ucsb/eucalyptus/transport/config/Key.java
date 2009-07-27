/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.config;

import java.util.HashMap;
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

public enum Key {

  MEP( "mep", Mep.IN_ONLY ),
  WSSEC_FLOW( "wssecFlow", Mep.IN_OUT ),
  CONF( "conf", null ),
  WSSEC_POLICY( "wssecPolicy", null ),
  NAMESPACE( "namespace", "http://msgs.eucalyptus.ucsb.edu/" ),
  //:: IN property keys :://
  AUTHENTICATOR( "authenticatorClass", edu.ucsb.eucalyptus.transport.auth.CertAuthentication.class ),
  WSDL( "wsdl", null ),
  HTTP_QUERY_SUPPORT( "httpQuerySupport", false ),
  //:: OUT property keys :://
  CACHE_HTTP_CLIENT( "cacheHttpClient", true ),
  TIMEOUT( "timeout", 30 ),
  MAX_ACTIVE( "maxActive", -1 ),
  MAX_IDLE( "maxIdle", -1 ),
  MIN_IDLE( "minIdle", 1 );

  private String key;
  private Object defaultValue;

  Key( final String key, final Object defaultValue )
  {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  public Mep getMep( Map<String, String> props )
  {
    if ( !props.containsKey( key ) ) return ( Mep ) defaultValue;
    return Mep.valueOf( props.get( key ) );
  }

  public Object getNewInstance( Map<String, String> props )
  {
    try
    {
      return this.getClass( props ).newInstance();
    }
    catch ( InstantiationException e )
    {
      return null;
    }
    catch ( IllegalAccessException e )
    {
      return null;
    }
  }

  public Class getClass( Map<String, String> props )
  {
    if ( props.containsKey( key ) )
      try
      {
        return Class.forName( props.get( key ) );
      }
      catch ( ClassNotFoundException e ) {return null;}
    return ( Class ) defaultValue;
  }

  public boolean getBoolean( Map<String, String> props )
  {
    if ( !props.containsKey( key ) ) return ( Boolean ) defaultValue;
    return Boolean.parseBoolean( props.get( key ) );
  }

  public String getString( Map<String, String> props )
  {
    if ( !props.containsKey( key ) ) return ( String ) defaultValue;
    return props.get( key );
  }

  public int getInt( Map<String, String> props )
  {
    if ( !props.containsKey( key ) ) return ( Integer ) defaultValue;
    try { return Integer.parseInt( props.get( key ) ); } catch ( NumberFormatException e ) { return ( Integer ) defaultValue; }
  }

  public String getDefaultValue()
  {
    if( this.defaultValue != null )
    {
      if( this.defaultValue instanceof String || this.defaultValue instanceof Boolean  || this.defaultValue instanceof Integer ) return this.defaultValue.toString();
      if( this.defaultValue instanceof Mep ) return ((Mep)this.defaultValue).name();
      if( this.defaultValue instanceof Class ) return ((Class)this.defaultValue).getCanonicalName();
    }
    return null;
  }

  public String getKey()
  {
    return key;
  }

  public static Map<String, String> getDefaultProperties()
  {
    HashMap<String,String> defProps = new HashMap<String,String>();
    for( Key prop : Key.values() )
      if( prop.getDefaultValue() != null )
      defProps.put( prop.getKey(), prop.getDefaultValue());
    return defProps;
  }
}
