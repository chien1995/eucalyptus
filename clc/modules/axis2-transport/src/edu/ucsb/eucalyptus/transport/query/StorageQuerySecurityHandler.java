/*
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.query;

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.keys.AbstractKeyStore;
import edu.ucsb.eucalyptus.keys.ServiceKeyStore;
import edu.ucsb.eucalyptus.util.CaseInsensitiveMap;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import org.bouncycastle.openssl.PEMReader;

import java.io.StringReader;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
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

public class StorageQuerySecurityHandler extends HMACQuerySecurityHandler {

    private static Logger LOG = Logger.getLogger( StorageQuerySecurityHandler.class );

    public QuerySecurityHandler getInstance()
    {
        return new StorageQuerySecurityHandler();
    }

    public String getName()
    {
        return "3";
    }

    public UserInfo authenticate( final HttpRequest httpRequest ) throws QuerySecurityException
    {
        String addr = httpRequest.getRequestURL();

        String verb = httpRequest.getHttpMethod();
        Map<String, String> httpParams = httpRequest.getParameters();
        Map<String, String> headers = httpRequest.getHeaders();
        return handle(addr, verb, httpParams, headers);
    }

    public UserInfo handle(String addr, String verb, Map<String, String> parameters, Map<String, String> headers) throws QuerySecurityException
    {
        CaseInsensitiveMap hdrs = new CaseInsensitiveMap(headers);

        this.checkParameters( hdrs );
        //:: check the signature :://

        String date =  (String) hdrs.remove( SecurityParameter.Date);
        String eucaCert = (String) hdrs.remove(StorageSecurityParameters.EucaCert);
        String signature = (String) hdrs.remove(StorageSecurityParameters.EucaSignature);

        String data = verb + "\n" + date + "\n" + addr + "\n";

        Signature sig;
        boolean valid = false;
        try {
            X509Certificate cert = (X509Certificate)new PEMReader(new StringReader(eucaCert)).readObject();
            AbstractKeyStore keyStore = ServiceKeyStore.getInstance();
            if(keyStore.getCertificateAlias(cert) != null) {
                //cert found in keystore
                PublicKey publicKey = cert.getPublicKey();
                sig = Signature.getInstance("SHA1withRSA");

                sig.initVerify(publicKey);
                sig.update(data.getBytes());
                valid = sig.verify(Base64.decode(signature));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        if(!valid) {
            throw new QuerySecurityException( "User authentication failed." );
        }

        //run as admin
        UserInfo admin = new UserInfo("admin");
        admin.setIsAdministrator(Boolean.TRUE);
        return admin;
    }


    private void checkParameters( final CaseInsensitiveMap header ) throws QuerySecurityException
    {
        if ( !header.containsKey(SecurityParameter.Authorization.toString()))
            throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.Authorization );
        if ( !header.containsKey(SecurityParameter.Date.toString()))
            throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.Date );
    }

    private String[] getSigInfo (String auth_part) {
        int index = auth_part.lastIndexOf(" ");
        String sigString = auth_part.substring(index + 1);
        return sigString.split(":");
    }

    public enum StorageSecurityParameters {
        EucaCert, EucaSignature
    }
}