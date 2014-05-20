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

package com.sina.http.httpclientandroidlib.impl.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sina.http.httpclientandroidlib.FormattedHeader;
import com.sina.http.httpclientandroidlib.Header;
import com.sina.http.httpclientandroidlib.HttpResponse;
import com.sina.http.httpclientandroidlib.androidextra.HttpClientAndroidLog;
import com.sina.http.httpclientandroidlib.annotation.Immutable;
import com.sina.http.httpclientandroidlib.auth.AuthScheme;
import com.sina.http.httpclientandroidlib.auth.AuthSchemeRegistry;
import com.sina.http.httpclientandroidlib.auth.AuthenticationException;
import com.sina.http.httpclientandroidlib.auth.MalformedChallengeException;
import com.sina.http.httpclientandroidlib.client.AuthenticationHandler;
import com.sina.http.httpclientandroidlib.client.params.AuthPolicy;
import com.sina.http.httpclientandroidlib.client.protocol.ClientContext;
import com.sina.http.httpclientandroidlib.protocol.HTTP;
import com.sina.http.httpclientandroidlib.protocol.HttpContext;
import com.sina.http.httpclientandroidlib.util.Asserts;
import com.sina.http.httpclientandroidlib.util.CharArrayBuffer;

/* LogFactory removed by HttpClient for Android script. */

/**
 * Base class for {@link AuthenticationHandler} implementations.
 *
 * @since 4.0
 *
 * @deprecated (4.2)  use {@link com.sina.http.httpclientandroidlib.client.AuthenticationStrategy}
 */
@Deprecated
@Immutable
public abstract class AbstractAuthenticationHandler implements AuthenticationHandler {

    public HttpClientAndroidLog log = new HttpClientAndroidLog(getClass());

    private static final List<String> DEFAULT_SCHEME_PRIORITY =
        Collections.unmodifiableList(Arrays.asList(new String[] {
                AuthPolicy.SPNEGO,
                AuthPolicy.NTLM,
                AuthPolicy.DIGEST,
                AuthPolicy.BASIC
    }));

    public AbstractAuthenticationHandler() {
        super();
    }

    protected Map<String, Header> parseChallenges(
            final Header[] headers) throws MalformedChallengeException {

        final Map<String, Header> map = new HashMap<String, Header>(headers.length);
        for (final Header header : headers) {
            final CharArrayBuffer buffer;
            int pos;
            if (header instanceof FormattedHeader) {
                buffer = ((FormattedHeader) header).getBuffer();
                pos = ((FormattedHeader) header).getValuePos();
            } else {
                final String s = header.getValue();
                if (s == null) {
                    throw new MalformedChallengeException("Header value is null");
                }
                buffer = new CharArrayBuffer(s.length());
                buffer.append(s);
                pos = 0;
            }
            while (pos < buffer.length() && HTTP.isWhitespace(buffer.charAt(pos))) {
                pos++;
            }
            final int beginIndex = pos;
            while (pos < buffer.length() && !HTTP.isWhitespace(buffer.charAt(pos))) {
                pos++;
            }
            final int endIndex = pos;
            final String s = buffer.substring(beginIndex, endIndex);
            map.put(s.toLowerCase(Locale.US), header);
        }
        return map;
    }

    /**
     * Returns default list of auth scheme names in their order of preference.
     *
     * @return list of auth scheme names
     */
    protected List<String> getAuthPreferences() {
        return DEFAULT_SCHEME_PRIORITY;
    }

    /**
     * Returns default list of auth scheme names in their order of preference
     * based on the HTTP response and the current execution context.
     *
     * @param response HTTP response.
     * @param context HTTP execution context.
     *
     * @since 4.1
     */
    protected List<String> getAuthPreferences(
            final HttpResponse response,
            final HttpContext context) {
        return getAuthPreferences();
    }

    public AuthScheme selectScheme(
            final Map<String, Header> challenges,
            final HttpResponse response,
            final HttpContext context) throws AuthenticationException {

        final AuthSchemeRegistry registry = (AuthSchemeRegistry) context.getAttribute(
                ClientContext.AUTHSCHEME_REGISTRY);
        Asserts.notNull(registry, "AuthScheme registry");
        Collection<String> authPrefs = getAuthPreferences(response, context);
        if (authPrefs == null) {
            authPrefs = DEFAULT_SCHEME_PRIORITY;
        }

        if (this.log.isDebugEnabled()) {
            this.log.debug("Authentication schemes in the order of preference: "
                + authPrefs);
        }

        AuthScheme authScheme = null;
        for (final String id: authPrefs) {
            final Header challenge = challenges.get(id.toLowerCase(Locale.ENGLISH));

            if (challenge != null) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug(id + " authentication scheme selected");
                }
                try {
                    authScheme = registry.getAuthScheme(id, response.getParams());
                    break;
                } catch (final IllegalStateException e) {
                    if (this.log.isWarnEnabled()) {
                        this.log.warn("Authentication scheme " + id + " not supported");
                        // Try again
                    }
                }
            } else {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Challenge for " + id + " authentication scheme not available");
                    // Try again
                }
            }
        }
        if (authScheme == null) {
            // If none selected, something is wrong
            throw new AuthenticationException(
                "Unable to respond to any of these challenges: "
                    + challenges);
        }
        return authScheme;
    }

}
