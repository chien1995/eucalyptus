/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
/* BRIEF EXAMPLE MSG:
<soapenv:Envelope>.
  <soapenv:Header>
    [..snip..]
    <wsse:Security>
      [..snip..]
      <wsse:BinarySecurityToken xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                              EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
                              ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
                              wsu:Id="CertId-469">[..snip..]</wsse:BinarySecurityToken>
      [..snip..]
      <ds:Signature>
        <ds:KeyInfo Id="KeyId-374652">
          <wsse:SecurityTokenReference xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="STRId-22112351">
            <!-- this thing points to the wsse:BinarySecurityToken above -->
            <wsse:Reference URI="#CertId-469" ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
          </wsse:SecurityTokenReference>
        </ds:KeyInfo>
      </ds:Signature>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>...</soapenv:Body>
</soapenv:Envelope>.
*/

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
#include <openssl/sha.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include "euca_auth.h"
#include "misc.h" /* get_string_stats, logprintf */

#ifndef NO_AXIS /* for compiling on systems without Axis */
#include "oxs_axiom.h"
#include "oxs_x509_cert.h"
#include "oxs_key_mgr.h"
#include "rampart_handler_util.h"
#include "rampart_sec_processed_result.h"
#include "rampart_error.h"

#define NO_U_FAIL(x) do{ \
AXIS2_LOG_ERROR(env->log, AXIS2_LOG_SI, "[rampart][eucalyptus-verify] " #x );\
AXIS2_ERROR_SET(env->error, RAMPART_ERROR_FAILED_AUTHENTICATION, AXIS2_FAILURE);\
return AXIS2_FAILURE; \
}while(0)

axis2_status_t __euca_authenticate(const axutil_env_t *env,axis2_msg_ctx_t *out_msg_ctx, axis2_op_ctx_t *op_ctx)
{
  //***** First get the message context before doing anything dumb w/ a NULL pointer *****/
  axis2_msg_ctx_t *msg_ctx = NULL; //<--- incoming msg context, it is NULL, see?
  msg_ctx = axis2_op_ctx_get_msg_ctx(op_ctx, env, AXIS2_WSDL_MESSAGE_LABEL_IN);  

  //***** Print everything from the security results, just for testing now *****//
  rampart_context_t *rampart_context = NULL;
  axutil_property_t *property = NULL;

  property = axis2_msg_ctx_get_property(msg_ctx, env, RAMPART_CONTEXT);
  if(property)
  {
     rampart_context = (rampart_context_t *)axutil_property_get_value(property, env);
     //     AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," ======== PRINTING PROCESSED WSSEC TOKENS ======== ");
     rampart_print_security_processed_results_set(env,msg_ctx);
  }

  //***** Extract Security Node from header from enveloper from msg_ctx *****//
  axiom_soap_envelope_t *soap_envelope = NULL;
  axiom_soap_header_t *soap_header = NULL;
  axiom_node_t *sec_node = NULL;


  soap_envelope = axis2_msg_ctx_get_soap_envelope(msg_ctx, env);
  if(!soap_envelope) NO_U_FAIL("SOAP envelope cannot be found."); 
  soap_header = axiom_soap_envelope_get_header(soap_envelope, env);
  if (!soap_header) NO_U_FAIL("SOAP header cannot be found.");
  sec_node = rampart_get_security_header(env, msg_ctx, soap_header); // <---- here it is!
  if(!sec_node)NO_U_FAIL("No node wsse:Security -- required: ws-security");

  //***** Find the wsse:Reference to the BinarySecurityToken *****//
  //** Path is: Security/
  //** *sec_node must be non-NULL, kkthx **//
  axiom_node_t *sig_node = NULL;
  axiom_node_t *key_info_node = NULL;
  axiom_node_t *sec_token_ref_node = NULL;
  /** the ds:Signature node **/
  sig_node = oxs_axiom_get_first_child_node_by_name(env,sec_node, OXS_NODE_SIGNATURE, OXS_DSIG_NS, OXS_DS );
  if(!sig_node)NO_U_FAIL("No node ds:Signature -- required: signature");
  /** the ds:KeyInfo **/
  key_info_node = oxs_axiom_get_first_child_node_by_name(env, sig_node, OXS_NODE_KEY_INFO, OXS_DSIG_NS, NULL );
  if(!key_info_node)NO_U_FAIL("No node ds:KeyInfo -- required: signature key");
  /** the wsse:SecurityTokenReference **/ 
  sec_token_ref_node = oxs_axiom_get_first_child_node_by_name(env, key_info_node,OXS_NODE_SECURITY_TOKEN_REFRENCE, OXS_WSSE_XMLNS, NULL);
  if(!sec_token_ref_node)NO_U_FAIL("No node wsse:SecurityTokenReference -- required: signing token");
  //** in theory this is the branching point for supporting all kinds of tokens -- we only do BST Direct Reference **/

  //***** Find the wsse:Reference to the BinarySecurityToken *****//
  //** *sec_token_ref_node must be non-NULL **/
  axis2_char_t *ref = NULL;
  axis2_char_t *ref_id = NULL;
  axiom_node_t *token_ref_node = NULL;
  axiom_node_t *bst_node = NULL;
  /** the wsse:Reference node **/
  token_ref_node = oxs_axiom_get_first_child_node_by_name(env, sec_token_ref_node,OXS_NODE_REFERENCE, OXS_WSSE_XMLNS, NULL);
  /** pull out the name of the BST node **/
  ref = oxs_token_get_reference(env, token_ref_node);
  ref_id = axutil_string_substring_starting_at(axutil_strdup(env, ref), 1);
  /** get the wsse:BinarySecurityToken used to sign the message **/
  bst_node = oxs_axiom_get_node_by_id(env, sec_node, "Id", ref_id, OXS_WSU_XMLNS);
  if(!bst_node){oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_ELEMENT_FAILED, "Error retrieving elementwith ID=%s", ref_id);NO_U_FAIL("Cant find the required node");}


  //***** Find the wsse:Reference to the BinarySecurityToken *****//
  //** *bst_node must be non-NULL **/
  axis2_char_t *data = NULL;
  oxs_x509_cert_t *_cert = NULL;
  oxs_x509_cert_t *recv_cert = NULL;
  axis2_char_t *file_name = NULL;
  axis2_char_t *recv_x509_buf = NULL;
  axis2_char_t *msg_x509_buf = NULL;

  /** pull out the data from the BST **/
  data = oxs_axiom_get_node_content(env, bst_node);
  /** create an oxs_X509_cert **/
  _cert = oxs_key_mgr_load_x509_cert_from_string(env, data);
  if(_cert)
  {
    //***** FINALLY -- we have the certificate used to sign the message.  authenticate it HERE *****//
    msg_x509_buf = oxs_x509_cert_get_data(_cert,env);
    if(!msg_x509_buf)NO_U_FAIL("OMG WHAT NOW?!");
    recv_x509_buf = (axis2_char_t *)rampart_context_get_receiver_certificate(rampart_context, env);
    if(recv_x509_buf)
        recv_cert = oxs_key_mgr_load_x509_cert_from_string(env, recv_x509_buf);
    else
    {
        file_name = rampart_context_get_receiver_certificate_file(rampart_context, env);
        if(!file_name) NO_U_FAIL("Policy for the service is incorrect -- ReceiverCertificate is not set!!");
        recv_cert = oxs_key_mgr_load_x509_cert_from_pem_file(env, file_name);
    }
    recv_x509_buf = oxs_x509_cert_get_data(recv_cert,env);

    if( axutil_strcmp(recv_x509_buf,msg_x509_buf)!=0){
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," --------- Received x509 certificate value ---------" );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI, msg_x509_buf );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," --------- Local x509 certificate value! ---------" );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI, recv_x509_buf );
      AXIS2_LOG_CRITICAL(env->log,AXIS2_LOG_SI," ---------------------------------------------------" );
      NO_U_FAIL("The certificate specified is invalid!");
    }
  }
  else 
  {
    oxs_error(env, OXS_ERROR_LOCATION, OXS_ERROR_DEFAULT, "Cannot load certificate from string =%s", data); 
    NO_U_FAIL("Failed to build certificate from BinarySecurityToken");
  }
  oxs_x509_cert_free(_cert, env);
  oxs_x509_cert_free(recv_cert, env);
  return AXIS2_SUCCESS;

}
#endif /* NO_AXIS */

static int initialized = 0;

#define FILENAME 512
static char cert_file [FILENAME];
static char pk_file   [FILENAME];

int euca_init_cert (void)
{	
    if (initialized) return 0;
    
    char root [] = "";
	char * euca_home = getenv("EUCALYPTUS");
    if (!euca_home) {
        euca_home = root;
    }
    snprintf (cert_file, FILENAME, "%s/var/lib/eucalyptus/keys/node-cert.pem", euca_home);
    snprintf (pk_file,   FILENAME, "%s/var/lib/eucalyptus/keys/node-pk.pem", euca_home);

	#define ERR "Error: required file %s not found by euca_init_cert(). Is $EUCALYPTUS set?\n"
	#define OK  "euca_init_cert(): using file %s\n"
    #define CHK_FILE(n) \
        if ((fd=open(n, O_RDONLY))<0) {\
                logprintfl (EUCAERROR, ERR, n); return 1; \
        } else { \
                close (fd); logprintfl (EUCAINFO, OK, n); \
        }

    int fd; 
	CHK_FILE(cert_file)
	CHK_FILE(pk_file)
	
	initialized = 1;
	return 0;
}

/* caller must free the returned string */
char * euca_get_cert (unsigned char options) 
{
	if (!initialized) euca_init_cert ();
		
    char * cert_str = NULL;
    int s, fp;

    struct stat st;
    if (stat (cert_file, &st) != 0) {
        logprintfl (EUCAERROR, "error: cannot stat the certificate file %s\n", cert_file); 

    } else if ( (s = st.st_size*2) < 1) { /* *2 because we'll add characters */
        logprintfl (EUCAERROR, "error: certificate file %s is too small\n", cert_file); 

    } else if ( (cert_str = malloc (s+1)) == NULL ) { 
        logprintfl (EUCAERROR, "error: out of memory\n");

    } else if ( (fp = open (cert_file, O_RDONLY)) < 0 ) {
        logprintfl (EUCAERROR, "error: failed to open certificate file %s\n", cert_file);
        free (cert_str);
        cert_str = NULL;

    } else {
        ssize_t ret = -1;
        int got = 0; 

        while ( got < s && (ret = read (fp, cert_str + got, 1) ) == 1 ) {
            if ( options & CONCATENATE_CERT ) { /* omit all newlines */
                if ( cert_str [got] == '\n' ) 
                    continue;
            } else {
                if ( options & INDENT_CERT ) /* indent lines 2 through N with TABs */
                    if ( cert_str [got] == '\n' )
                        cert_str [++got] = '\t'; 
            }
            got++;
        }
        
        if (ret != 0) {
            logprintfl (EUCAERROR, "error: failed to read whole certificate file %s\n", cert_file);
            free (cert_str);
            cert_str = NULL;

        } else {
            if ( options & TRIM_CERT ) {
                if ( cert_str [got-1] == '\t' || 
                     cert_str [got-1] == '\n' ) got--;
                if ( cert_str [got-1] == '\n' ) got--; /* because of indenting */ 
            }
            cert_str [got] = '\0';
        }
        close (fp);
    }
    return cert_str;
}

/* caller must free the returned string */
char * base64_enc (unsigned char * in, int size)
{
  char * out_str = NULL;
  BIO * biomem, * bio64;
  
  if ( (bio64 = BIO_new (BIO_f_base64 ())) == NULL) {
    logprintfl (EUCAERROR, "error: BIO_new(BIO_f_base64()) failed\n");
  } else {
    BIO_set_flags (bio64, BIO_FLAGS_BASE64_NO_NL); /* no long-line wrapping */
    if ( (biomem = BIO_new (BIO_s_mem ())) == NULL) {
      logprintfl (EUCAERROR, "error: BIO_new(BIO_s_mem()) failed\n");
    } else {
      bio64 = BIO_push (bio64, biomem);
      if ( BIO_write (bio64, in, size)!=size) {
	logprintfl (EUCAERROR, "error: BIO_write() failed\n");
      } else {
	BUF_MEM * buf;
	(void) BIO_flush (bio64);
	BIO_get_mem_ptr (bio64, &buf);
	if ( (out_str = malloc(buf->length+1)) == NULL ) {
	  logprintfl (EUCAERROR, "error: out of memory for Base64 buf\n");
	} else {
	  memcpy (out_str, buf->data, buf->length);
	  out_str [buf->length] = '\0';
	}
      }
    }
    BIO_free_all (bio64); /* frees both bio64 and biomem */
  }
  return out_str;
}

/* caller must free the returned string */
char *base64_dec(unsigned char *in, int size)
{
  BIO *bio64, *biomem;
  char *buf=NULL;

  buf = malloc(sizeof(char) * size);
  bzero(buf, size);

  if ((bio64 = BIO_new(BIO_f_base64())) == NULL) {
    logprintfl(EUCAERROR, "BIO_new(BIO_f_base64()) failed\n");
  } else {
    BIO_set_flags (bio64, BIO_FLAGS_BASE64_NO_NL); /* no long-line wrapping */

    if ((biomem = BIO_new_mem_buf(in, size)) == NULL) {
      logprintfl(EUCAERROR, "BIO_new_mem_buf() failed\n");
    } else {
      biomem = BIO_push(bio64, biomem);

      if ((BIO_read(biomem, buf, size)) <= 0) {
        logprintfl(EUCAERROR, "BIO_read() read failed\n");
      }
      //      BIO_free_all(biomem);
    }
    BIO_free_all(bio64);
  }

  return buf;
}


/* caller must free the returned string */
char * euca_sign_url (const char * verb, const char * date, const char * url)
{
	if (!initialized) euca_init_cert ();
		
    char * sig_str = NULL;
    RSA * rsa = NULL;
    FILE * fp = NULL;

    if ( verb==NULL || date==NULL || url==NULL ) return NULL;

    if ( ( rsa = RSA_new() ) == NULL ) {
      logprintfl (EUCAERROR, "error: RSA_new() failed\n");
    } else if ( ( fp = fopen (pk_file, "r") ) == NULL) {
      logprintfl (EUCAERROR, "error: failed to open private key file %s\n", pk_file);
      RSA_free (rsa);
    } else {
      logprintfl (EUCADEBUG2, "euca_sign_url(): reading private key file %s\n", pk_file);
      PEM_read_RSAPrivateKey(fp, &rsa, NULL, NULL); /* read the PEM-encoded file into rsa struct */
      if ( rsa==NULL ) {
	logprintfl (EUCAERROR, "error: failed to read private key file %s\n", pk_file);
      } else {
	unsigned char * sig;
        
	// RSA_print_fp (stdout, rsa, 0); /* (for debugging) */
	if ( (sig = malloc(RSA_size(rsa))) == NULL) {
	  logprintfl (EUCAERROR, "error: out of memory (for RSA key)\n");
	} else {
	  unsigned char sha1 [SHA_DIGEST_LENGTH];
#define BUFSIZE 2024
	  char input [BUFSIZE];
	  unsigned int siglen;
	  int ret;
	  
	  /* finally, SHA1 and sign with PK */
	  assert ((strlen(verb)+strlen(date)+strlen(url)+4)<=BUFSIZE);
	  snprintf (input, BUFSIZE, "%s\n%s\n%s\n", verb, date, url);
	  logprintfl (EUCADEBUG2, "euca_sign_url(): signing input %s\n", get_string_stats(input));	
	  SHA1 ((unsigned char *)input, strlen(input), sha1);
	  if ((ret = RSA_sign (NID_sha1, sha1, SHA_DIGEST_LENGTH, sig, &siglen, rsa))!=1) {
	    logprintfl (EUCAERROR, "error: RSA_sign() failed\n");
	  } else {
	    logprintfl (EUCADEBUG2, "euca_sign_url(): signing output %d\n", sig[siglen-1]);	
	    sig_str = base64_enc (sig, siglen);
	    logprintfl (EUCADEBUG2, "euca_sign_url(): base64 signature %s\n", get_string_stats((char *)sig_str));	
	  }
	  free (sig);
	}
	RSA_free (rsa);
      }            
      fclose(fp);
    }
    
    return sig_str;
}
