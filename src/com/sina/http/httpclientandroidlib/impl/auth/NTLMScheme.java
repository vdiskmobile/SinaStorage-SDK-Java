/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.sina.http.httpclientandroidlib.impl.auth;

import com.sina.http.httpclientandroidlib.Header;
import com.sina.http.httpclientandroidlib.HttpRequest;
import com.sina.http.httpclientandroidlib.annotation.NotThreadSafe;
import com.sina.http.httpclientandroidlib.auth.AUTH;
import com.sina.http.httpclientandroidlib.auth.AuthenticationException;
import com.sina.http.httpclientandroidlib.auth.Credentials;
import com.sina.http.httpclientandroidlib.auth.InvalidCredentialsException;
import com.sina.http.httpclientandroidlib.auth.MalformedChallengeException;
import com.sina.http.httpclientandroidlib.auth.NTCredentials;
import com.sina.http.httpclientandroidlib.message.BufferedHeader;
import com.sina.http.httpclientandroidlib.util.Args;
import com.sina.http.httpclientandroidlib.util.CharArrayBuffer;

/**
 * NTLM is a proprietary authentication scheme developed by Microsoft
 * and optimized for Windows platforms.
 *
 * @since 4.0
 */
@NotThreadSafe
public class NTLMScheme extends AuthSchemeBase {

    enum State {
        UNINITIATED,
        CHALLENGE_RECEIVED,
        MSG_TYPE1_GENERATED,
        MSG_TYPE2_RECEVIED,
        MSG_TYPE3_GENERATED,
        FAILED,
    }

    private final NTLMEngine engine;

    private State state;
    private String challenge;

    public NTLMScheme(final NTLMEngine engine) {
        super();
        Args.notNull(engine, "NTLM engine");
        this.engine = engine;
        this.state = State.UNINITIATED;
        this.challenge = null;
    }

    /**
     * @since 4.3
     */
    public NTLMScheme() {
        this(new NTLMEngineImpl());
    }

    public String getSchemeName() {
        return "ntlm";
    }

    public String getParameter(final String name) {
        // String parameters not supported
        return null;
    }

    public String getRealm() {
        // NTLM does not support the concept of an authentication realm
        return null;
    }

    public boolean isConnectionBased() {
        return true;
    }

    @Override
    protected void parseChallenge(
            final CharArrayBuffer buffer,
            final int beginIndex, final int endIndex) throws MalformedChallengeException {
        this.challenge = buffer.substringTrimmed(beginIndex, endIndex);
        if (this.challenge.length() == 0) {
            if (this.state == State.UNINITIATED) {
                this.state = State.CHALLENGE_RECEIVED;
            } else {
                this.state = State.FAILED;
            }
        } else {
            if (this.state.compareTo(State.MSG_TYPE1_GENERATED) < 0) {
                this.state = State.FAILED;
                throw new MalformedChallengeException("Out of sequence NTLM response message");
            } else if (this.state == State.MSG_TYPE1_GENERATED) {
                this.state = State.MSG_TYPE2_RECEVIED;
            }
        }
    }

    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request) throws AuthenticationException {
        NTCredentials ntcredentials = null;
        try {
            ntcredentials = (NTCredentials) credentials;
        } catch (final ClassCastException e) {
            throw new InvalidCredentialsException(
             "Credentials cannot be used for NTLM authentication: "
              + credentials.getClass().getName());
        }
        String response = null;
        if (this.state == State.FAILED) {
            throw new AuthenticationException("NTLM authentication failed");
        } else if (this.state == State.CHALLENGE_RECEIVED) {
            response = this.engine.generateType1Msg(
                    ntcredentials.getDomain(),
                    ntcredentials.getWorkstation());
            this.state = State.MSG_TYPE1_GENERATED;
        } else if (this.state == State.MSG_TYPE2_RECEVIED) {
            response = this.engine.generateType3Msg(
                    ntcredentials.getUserName(),
                    ntcredentials.getPassword(),
                    ntcredentials.getDomain(),
                    ntcredentials.getWorkstation(),
                    this.challenge);
            this.state = State.MSG_TYPE3_GENERATED;
        } else {
            throw new AuthenticationException("Unexpected state: " + this.state);
        }
        final CharArrayBuffer buffer = new CharArrayBuffer(32);
        if (isProxy()) {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
            buffer.append(AUTH.WWW_AUTH_RESP);
        }
        buffer.append(": NTLM ");
        buffer.append(response);
        return new BufferedHeader(buffer);
    }

    public boolean isComplete() {
        return this.state == State.MSG_TYPE3_GENERATED || this.state == State.FAILED;
    }

}
