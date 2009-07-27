/*
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.List;
import java.util.zip.Adler32;

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

@Entity
@Table( name = "counters" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Counters {

  @Id
  @Column( name = "id" )
  private Long id = -1l;
  @Column( name = "msg_count" )
  private Long messageId;

  public Counters()
  {
    this.id = 0l;
    this.messageId = 0l;
  }

  public Long getMessageId()
  {
    return messageId;
  }

  public void setMessageId( Long messageId )
  {
    this.messageId = messageId;
  }

  @Transient
  private static long tempId = -1;
  @Transient
  private static Adler32 digest = new Adler32();
  public static synchronized long getIdBlock( int length )
  {
    ensurePersistence( );
    long oldTemp = tempId;
    tempId+=length;
    return oldTemp;
  }

  public synchronized static String getNextId()
  {
    ensurePersistence( );
    tempId++;
    return Hashes.getDigestBase64( Long.toString( tempId ), Hashes.Digest.SHA512, false ).replaceAll( "\\.","" );
  }

  private static void ensurePersistence( )
  {
    long modulus = 100l;
    if ( tempId < 0 )
    {
      Counters find = null;
      EntityManager em = EntityWrapper.getEntityManagerFactory( EucalyptusProperties.NAME).createEntityManager(  );
      Session session = (Session) em.getDelegate();
      List<Counters> found = ( List<Counters> ) session.createSQLQuery( "select * from counters" ).addEntity( Counters.class ).list();
      if( found.isEmpty() )
      {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Counters newCounters = new Counters();
        em.persist(newCounters);
        em.flush();
        tx.commit();
        find = newCounters;
      }
      else
        find = found.get(0);
      tempId = find.getMessageId() + modulus;
      em.close();
    }
    else if ( tempId % modulus == 0 )
    {
      EntityManager em = EntityWrapper.getEntityManagerFactory( EucalyptusProperties.NAME).createEntityManager(  );
      Session session = (Session) em.getDelegate();
      Transaction tx = session.beginTransaction();
      tx.begin();
      Counters find = ( Counters ) session.createSQLQuery( "select * from counters" ).addEntity( Counters.class ).list().get( 0 );
      tempId += modulus;
      find.setMessageId( tempId );
      session.save( find );
      tx.commit();
      em.close();
    }
  }
}
