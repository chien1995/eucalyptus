/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport;

import edu.ucsb.eucalyptus.transport.http.Axis2HttpListener;
import edu.ucsb.eucalyptus.util.BaseDirectory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractConnector;

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

public class Axis2Connector extends AbstractConnector {

  private static Logger LOG = Logger.getLogger( Axis2Connector.class );

  public static String PROTOCOL = "axis2";
//  public static String CONFIG_FILE = BaseDirectory.CONF.toString() + File.separator + "axis2.xml";
//  public static String CONFIG_CLUSTER_FILE = BaseDirectory.CONF.toString() + File.separator + "cluster-axis2.xml";
//  public static String WSPOLICY_FILE = BaseDirectory.CONF.toString() + File.separator + "policy.xml";
//  public static String WSPOLICY_CLUSTER_FILE = BaseDirectory.CONF.toString() + File.separator + "cluster-policy.xml";
  private String defaultConf;
  private String defaultWssecPolicy;
  private String deployPath = BaseDirectory.VAR.toString();
  private ConfigurationContext axisConfig;
  private Axis2HttpListener http;

  public Axis2Connector()
  {
    registerSupportedProtocol( "http" );
    registerSupportedProtocol( "https" );
  }

  public Axis2Connector( final String defaultConf, final String defaultWssecPolicy )
  {
    this.defaultConf = defaultConf;
    this.defaultWssecPolicy = defaultWssecPolicy;
    try
    {
      this.doInitialise();
    }
    catch ( InitialisationException e )
    {
      LOG.error( e, e );
    }
  }

  public void doInitialise() throws InitialisationException
  {
    try
    {
      this.axisConfig = ConfigurationContextFactory.createConfigurationContextFromFileSystem( this.deployPath, this.defaultConf );
    }
    catch ( AxisFault axisFault )
    {
      Logger.getLogger( Axis2Connector.class ).error( axisFault.getMessage(), axisFault );
    }
  }

  public void doConnect() throws Exception
  {
    this.http = new Axis2HttpListener( this.axisConfig );
  }

  public String getProtocol()
  {
    return PROTOCOL;
  }

  public ConfigurationContext getAxisConfig()
  {
    return axisConfig;
  }

  public void setAxisConfig( ConfigurationContext axisConfig )
  {
    this.axisConfig = axisConfig;
  }

  public String getDefaultConf()
  {
    return defaultConf;
  }

  public void setDefaultConf( String defaultConf )
  {
    this.defaultConf = defaultConf;
  }

  public String getDeployPath()
  {
    return deployPath;
  }

  public void setDeployPath( String deployPath )
  {
    this.deployPath = deployPath;
  }

  public Axis2HttpListener getHttp()
  {
    return http;
  }

  public void setHttp( Axis2HttpListener http )
  {
    this.http = http;
  }

  public String getDefaultWssecPolicy()
  {
    return defaultWssecPolicy;
  }

  public void setDefaultWssecPolicy( String defaultWssecPolicy )
  {
    this.defaultWssecPolicy = defaultWssecPolicy;
  }

  public void addHttpPortListener( String host, int port ) throws AxisFault
  {
    this.http.addHttpListener( host, port );
  }

  public void doDisconnect() throws Exception {}

  public void doStart() throws MuleException {}

  public void doStop() throws MuleException {}

  public void doDispose() {}
}
