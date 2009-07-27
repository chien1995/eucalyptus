/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.binding;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.jibx.JiBXDataSource;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.UnmarshallingContext;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

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

public class Binding {

  private static Logger LOG = Logger.getLogger( Binding.class );
  private String name;
  private IBindingFactory bindingFactory;
  private String bindingErrorMsg;
  private int[] bindingNamespaceIndexes;
  private String[] bindingNamespacePrefixes;

  protected Binding( String name ) throws JiBXException
  {
    this.name = name;
    this.buildRest();
  }

  protected Binding( String name, Class seedClass ) throws JiBXException
  {
    this.name = name;
    this.seed( seedClass );
  }

  public void seed( Class seedClass ) throws JiBXException
  {
    if ( seedClass.getSimpleName().equals( "Eucalyptus" ) )
    {
      bindingFactory = BindingDirectory.getFactory( this.name, edu.ucsb.eucalyptus.msgs.RunInstancesType.class );
    }
    else if ( seedClass.getSimpleName().equals( "Walrus" ) )
    {
      bindingFactory = BindingDirectory.getFactory( this.name, edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType.class );
    }
    else if ( seedClass.getSimpleName().equals("StorageController")) {
      bindingFactory = BindingDirectory.getFactory(this.name, edu.ucsb.eucalyptus.msgs.StorageRequestType.class);
    } else {
      Method[] methods = seedClass.getDeclaredMethods();
      for ( Method m : methods )
        try
        {
          bindingFactory = BindingDirectory.getFactory( this.name, m.getReturnType() );
          break;
        }
        catch ( Exception e )
        {
          this.bindingErrorMsg = e.getMessage();
          LOG.warn( "No binding for " + m.getName(), e );
        }
      if ( bindingFactory == null )
        throw new JiBXException( "Failed to construct BindingFactory for class: " + seedClass );
    }
    buildRest();
  }

  private void buildRest()
  {
//:: TODO: chop chop :://
//    int[] indexes = null;
//    String[] prefixes = null;
//    if ( bindingFactory != null )
//    {
//      String[] nsuris = bindingFactory.getNamespaces();
//      int xsiindex = nsuris.length;
//      while ( --xsiindex >= 0 && !"http://www.w3.org/2001/XMLSchema-instance".equals( nsuris[ xsiindex ] ) ) ;
//      // get actual size of index and prefix arrays to be allocated
//      int nscount = 0;
//      int usecount = nscount;
//      if ( xsiindex >= 0 )
//        usecount++;
//      // allocate and initialize the arrays
//      indexes = new int[usecount];
//      prefixes = new String[usecount];
//      if ( xsiindex >= 0 )
//      {
//        indexes[ nscount ] = xsiindex;
//        prefixes[ nscount ] = "xsi";
//      }
//    }
//    this.bindingNamespaceIndexes = indexes;
//    this.bindingNamespacePrefixes = prefixes;
  }

  public OMElement toOM( Object param ) {
    return toOM( param, null );
  }
  public OMElement toOM( Object param, String altNs ) {
    OMFactory factory = OMAbstractFactory.getOMFactory();
    if ( param == null )
      throw new RuntimeException( "Cannot bind null value" );
    else if ( !( param instanceof IMarshallable ) )
      throw new RuntimeException( "No JiBX <mapping> defined for class " + param.getClass() );
    if ( bindingFactory == null ) {
      try {
        bindingFactory = BindingDirectory.getFactory( this.name, param.getClass() );
      }
      catch ( JiBXException e ) {
        LOG.error( e, e );
        throw new RuntimeException( this.bindingErrorMsg );
      }
    }

    IMarshallable mrshable = ( IMarshallable ) param;
    OMDataSource src = new JiBXDataSource( mrshable, bindingFactory );
    int index = mrshable.JiBX_getIndex();
    OMNamespace appns = factory.createOMNamespace( bindingFactory.getElementNamespaces()[ index ], "" );
    OMElement retVal = factory.createOMElement( src, bindingFactory.getElementNames()[ index ], appns );
    String origNs = retVal.getNamespace().getNamespaceURI();
    if( altNs != null && !altNs.equals( origNs ) ) {
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream( );
        XMLStreamWriter xmlStream = XMLOutputFactory.newInstance().createXMLStreamWriter(bos);
        retVal.serialize( xmlStream );
        xmlStream.flush();
        xmlStream.close();
        String retString = bos.toString();
        retString = retString.replaceAll( origNs, altNs );
        ByteArrayInputStream bis = new ByteArrayInputStream( retString.getBytes( ));
        StAXOMBuilder stAXOMBuilder = new StAXOMBuilder(bis);
        retVal = stAXOMBuilder.getDocumentElement();
      } catch ( XMLStreamException e ) {
        LOG.error( e, e );
      }
    }

    return retVal;
  }

  public UnmarshallingContext getNewUnmarshalContext( OMElement param ) throws JiBXException
  {
    if ( bindingFactory == null )
      try
      {
        bindingFactory = BindingDirectory.getFactory( this.name, Class.forName( "edu.ucsb.eucalyptus.msgs." + param.getLocalName() + "Type" ) );
      }
      catch ( Exception e )
      {
        LOG.error( e, e );
        throw new RuntimeException( this.bindingErrorMsg );
      }
    UnmarshallingContext ctx = ( UnmarshallingContext ) this.bindingFactory.createUnmarshallingContext();
    IXMLReader reader = new StAXReaderWrapper( param.getXMLStreamReaderWithoutCaching(), "SOAP-message", true );
    ctx.setDocument( reader );
    ctx.toTag();
    return ctx;
  }

  public Object fromOM( String text ) throws Exception
  {
    XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader( new ByteArrayInputStream( text.getBytes() ) );
    StAXOMBuilder builder = new StAXOMBuilder( parser );
    return this.fromOM( builder.getDocumentElement() );
  }

  public Object fromOM( OMElement param, Class type ) throws AxisFault
  {
    try
    {
      UnmarshallingContext ctx = getNewUnmarshalContext( param );
      return ctx.unmarshalElement( type );
    }
    catch ( Exception e )
    {
      LOG.fatal( e,e );
      throw new AxisFault( e.getMessage() );
    }
  }

  public Object fromOM( OMElement param ) throws AxisFault
  {
    try
    {
      UnmarshallingContext ctx = getNewUnmarshalContext( param );
      return ctx.unmarshalElement( Class.forName( "edu.ucsb.eucalyptus.msgs." + param.getLocalName() + "Type" ) );
    }
    catch ( Exception e )
    {
      LOG.fatal( e,e );
      throw new AxisFault( e.getMessage() );
    }
  }

}
