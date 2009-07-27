/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.config;

import edu.ucsb.eucalyptus.transport.Axis2InOnlyMessageReceiver;
import edu.ucsb.eucalyptus.transport.Axis2InOutMessageReceiver;
import edu.ucsb.eucalyptus.transport.Axis2MessageReceiver;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;

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

public class Axis2OperationBuilder {

  private static Logger LOG = Logger.getLogger( Axis2OperationBuilder.class );

  public static AxisOperation getAxisOperation( String operationName, Axis2MessageReceiver msgReceiver ) throws AxisFault
  {
    if ( Mep.IN_OUT.equals( msgReceiver.getProperties().getMep() ) )
      return getInOutOperation( operationName, msgReceiver );
    else if ( Mep.IN_ONLY.equals( msgReceiver.getProperties().getMep() ) )
      return getInOnlyOperation( operationName, msgReceiver );
    return getInOutOperation( operationName, msgReceiver );
  }

  public static AxisOperation getInOutOperation( String operationName, Axis2MessageReceiver msgReceiver ) throws AxisFault
  {
    AxisOperation newOperation = new InOutAxisOperation( new QName( operationName ) );

    //:: setup the msg recveiver :://
    newOperation.setMessageExchangePattern( msgReceiver.getProperties().getMep().toString() );
    newOperation.setMessageReceiver( new Axis2InOutMessageReceiver( msgReceiver, msgReceiver.getProperties().getServiceClass() ) );

    //:: set the addressing mappings :://
    newOperation.setOutputAction( operationName + "Response" );
    newOperation.setSoapAction( operationName );
    newOperation.setWsamappingList( new ArrayList( Arrays.asList( operationName ) ) );
    newOperation.addParameter( AddressingConstants.ADDR_VALIDATE_ACTION, Boolean.FALSE );

    //:: setup the wssec policy :://
    if ( msgReceiver.getProperties().getInPolicy() != null )
      newOperation.getMessage( WSDLConstants.MESSAGE_LABEL_IN_VALUE ).getPolicySubject().attachPolicy( msgReceiver.getProperties().getInPolicy() );
    if ( msgReceiver.getProperties().getOutPolicy() != null )
      newOperation.getMessage( WSDLConstants.MESSAGE_LABEL_OUT_VALUE ).getPolicySubject().attachPolicy( msgReceiver.getProperties().getOutPolicy() );

    LOG.warn( String.format( "OPERATION: %s MEP: %s POLICY: %s", operationName, msgReceiver.getProperties().getMep().name(), msgReceiver.getProperties().getWssecFlow().name() ) );
    return newOperation;
  }

  public static AxisOperation getInOnlyOperation( String operationName, Axis2MessageReceiver msgReceiver ) throws AxisFault
  {
    AxisOperation newOperation = new InOnlyAxisOperation( new QName( operationName ) );

    //:: setup the msg recveiver :://
    newOperation.setMessageExchangePattern( msgReceiver.getProperties().getMep().toString() );
    newOperation.setMessageReceiver( new Axis2InOnlyMessageReceiver( msgReceiver, msgReceiver.getProperties().getServiceClass() ) );
    newOperation.addParameter( AddressingConstants.ADDR_VALIDATE_ACTION, Boolean.FALSE );

    //:: set the addressing mappings :://
    newOperation.setSoapAction( operationName );
    newOperation.setWsamappingList( new ArrayList( Arrays.asList( operationName ) ) );

    //:: setup the wssec policy :://
    if ( msgReceiver.getProperties().getInPolicy() != null )
      newOperation.getMessage( WSDLConstants.MESSAGE_LABEL_IN_VALUE ).getPolicySubject().attachPolicy( msgReceiver.getProperties().getInPolicy() );

    LOG.warn( String.format( "OPERATION: %s MEP: %s POLICY: %s", operationName, msgReceiver.getProperties().getMep().name(), msgReceiver.getProperties().getWssecFlow().name() ) );
    return newOperation;
  }

}
