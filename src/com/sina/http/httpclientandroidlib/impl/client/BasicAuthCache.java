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

import java.util.HashMap;

import com.sina.http.httpclientandroidlib.HttpHost;
import com.sina.http.httpclientandroidlib.annotation.NotThreadSafe;
import com.sina.http.httpclientandroidlib.auth.AuthScheme;
import com.sina.http.httpclientandroidlib.client.AuthCache;
import com.sina.http.httpclientandroidlib.conn.SchemePortResolver;
import com.sina.http.httpclientandroidlib.conn.UnsupportedSchemeException;
import com.sina.http.httpclientandroidlib.impl.conn.DefaultSchemePortResolver;
import com.sina.http.httpclientandroidlib.util.Args;

/**
 * Default implementation of {@link AuthCache}.
 *
 * @since 4.0
 */
@NotThreadSafe
public class BasicAuthCache implements AuthCache {

    private final HashMap<HttpHost, AuthScheme> map;
    private final SchemePortResolver schemePortResolver;

    /**
     * Default constructor.
     *
     * @since 4.3
     */
    public BasicAuthCache(final SchemePortResolver schemePortResolver) {
        super();
        this.map = new HashMap<HttpHost, AuthScheme>();
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver :
            DefaultSchemePortResolver.INSTANCE;
    }

    public BasicAuthCache() {
        this(null);
    }

    protected HttpHost getKey(final HttpHost host) {
        if (host.getPort() <= 0) {
            final int port;
            try {
                port = schemePortResolver.resolve(host);
            } catch (final UnsupportedSchemeException ignore) {
                return host;
            }
            return new HttpHost(host.getHostName(), port, host.getSchemeName());
        } else {
            return host;
        }
    }

    public void put(final HttpHost host, final AuthScheme authScheme) {
        Args.notNull(host, "HTTP host");
        this.map.put(getKey(host), authScheme);
    }

    public AuthScheme get(final HttpHost host) {
        Args.notNull(host, "HTTP host");
        return this.map.get(getKey(host));
    }

    public void remove(final HttpHost host) {
        Args.notNull(host, "HTTP host");
        this.map.remove(getKey(host));
    }

    public void clear() {
        this.map.clear();
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

}
