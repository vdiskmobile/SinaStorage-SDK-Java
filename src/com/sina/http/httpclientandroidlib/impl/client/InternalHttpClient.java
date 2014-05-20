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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sina.http.httpclientandroidlib.HttpException;
import com.sina.http.httpclientandroidlib.HttpHost;
import com.sina.http.httpclientandroidlib.HttpRequest;
import com.sina.http.httpclientandroidlib.androidextra.HttpClientAndroidLog;
import com.sina.http.httpclientandroidlib.annotation.ThreadSafe;
import com.sina.http.httpclientandroidlib.auth.AuthSchemeProvider;
import com.sina.http.httpclientandroidlib.auth.AuthState;
import com.sina.http.httpclientandroidlib.client.ClientProtocolException;
import com.sina.http.httpclientandroidlib.client.CookieStore;
import com.sina.http.httpclientandroidlib.client.CredentialsProvider;
import com.sina.http.httpclientandroidlib.client.config.RequestConfig;
import com.sina.http.httpclientandroidlib.client.methods.CloseableHttpResponse;
import com.sina.http.httpclientandroidlib.client.methods.Configurable;
import com.sina.http.httpclientandroidlib.client.methods.HttpExecutionAware;
import com.sina.http.httpclientandroidlib.client.methods.HttpRequestWrapper;
import com.sina.http.httpclientandroidlib.client.params.ClientPNames;
import com.sina.http.httpclientandroidlib.client.params.HttpClientParamConfig;
import com.sina.http.httpclientandroidlib.client.protocol.HttpClientContext;
import com.sina.http.httpclientandroidlib.config.Lookup;
import com.sina.http.httpclientandroidlib.conn.ClientConnectionManager;
import com.sina.http.httpclientandroidlib.conn.ClientConnectionRequest;
import com.sina.http.httpclientandroidlib.conn.HttpClientConnectionManager;
import com.sina.http.httpclientandroidlib.conn.ManagedClientConnection;
import com.sina.http.httpclientandroidlib.conn.routing.HttpRoute;
import com.sina.http.httpclientandroidlib.conn.routing.HttpRoutePlanner;
import com.sina.http.httpclientandroidlib.conn.scheme.SchemeRegistry;
import com.sina.http.httpclientandroidlib.cookie.CookieSpecProvider;
import com.sina.http.httpclientandroidlib.impl.execchain.ClientExecChain;
import com.sina.http.httpclientandroidlib.params.HttpParams;
import com.sina.http.httpclientandroidlib.params.HttpParamsNames;
import com.sina.http.httpclientandroidlib.protocol.BasicHttpContext;
import com.sina.http.httpclientandroidlib.protocol.HttpContext;
import com.sina.http.httpclientandroidlib.util.Args;
import com.sina.http.httpclientandroidlib.util.Asserts;

/* LogFactory removed by HttpClient for Android script. */

/**
 * Internal class.
 *
 * @since 4.3
 */
@ThreadSafe
@SuppressWarnings("deprecation")
class InternalHttpClient extends CloseableHttpClient {

    public HttpClientAndroidLog log = new HttpClientAndroidLog(getClass());

    private final ClientExecChain execChain;
    private final HttpClientConnectionManager connManager;
    private final HttpRoutePlanner routePlanner;
    private final Lookup<CookieSpecProvider> cookieSpecRegistry;
    private final Lookup<AuthSchemeProvider> authSchemeRegistry;
    private final CookieStore cookieStore;
    private final CredentialsProvider credentialsProvider;
    private final RequestConfig defaultConfig;
    private final List<Closeable> closeables;

    public InternalHttpClient(
            final ClientExecChain execChain,
            final HttpClientConnectionManager connManager,
            final HttpRoutePlanner routePlanner,
            final Lookup<CookieSpecProvider> cookieSpecRegistry,
            final Lookup<AuthSchemeProvider> authSchemeRegistry,
            final CookieStore cookieStore,
            final CredentialsProvider credentialsProvider,
            final RequestConfig defaultConfig,
            final List<Closeable> closeables) {
        super();
        Args.notNull(execChain, "HTTP client exec chain");
        Args.notNull(connManager, "HTTP connection manager");
        Args.notNull(routePlanner, "HTTP route planner");
        this.execChain = execChain;
        this.connManager = connManager;
        this.routePlanner = routePlanner;
        this.cookieSpecRegistry = cookieSpecRegistry;
        this.authSchemeRegistry = authSchemeRegistry;
        this.cookieStore = cookieStore;
        this.credentialsProvider = credentialsProvider;
        this.defaultConfig = defaultConfig;
        this.closeables = closeables;
    }

    private HttpRoute determineRoute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws HttpException {
        HttpHost host = target;
        if (host == null) {
            host = (HttpHost) request.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        Asserts.notNull(host, "Target host");
        return this.routePlanner.determineRoute(host, request, context);
    }

    private void setupContext(final HttpClientContext context) {
        if (context.getAttribute(HttpClientContext.TARGET_AUTH_STATE) == null) {
            context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.PROXY_AUTH_STATE) == null) {
            context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.AUTHSCHEME_REGISTRY) == null) {
            context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIESPEC_REGISTRY) == null) {
            context.setAttribute(HttpClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIE_STORE) == null) {
            context.setAttribute(HttpClientContext.COOKIE_STORE, this.cookieStore);
        }
        if (context.getAttribute(HttpClientContext.CREDS_PROVIDER) == null) {
            context.setAttribute(HttpClientContext.CREDS_PROVIDER, this.credentialsProvider);
        }
        if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.defaultConfig);
        }
    }

    @Override
    protected CloseableHttpResponse doExecute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(request, "HTTP request");
        HttpExecutionAware execAware = null;
        if (request instanceof HttpExecutionAware) {
            execAware = (HttpExecutionAware) request;
        }
        try {
            final HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request);
            final HttpClientContext localcontext = HttpClientContext.adapt(
                    context != null ? context : new BasicHttpContext());
            RequestConfig config = null;
            if (request instanceof Configurable) {
                config = ((Configurable) request).getConfig();
            }
            if (config == null) {
                final HttpParams params = request.getParams();
                if (params instanceof HttpParamsNames) {
                    if (!((HttpParamsNames) params).getNames().isEmpty()) {
                        config = HttpClientParamConfig.getRequestConfig(params);
                    }
                } else {
                    config = HttpClientParamConfig.getRequestConfig(params);
                }
            }
            if (config != null) {
                localcontext.setRequestConfig(config);
            }
            setupContext(localcontext);
            final HttpRoute route = determineRoute(target, wrapper, localcontext);
            return this.execChain.execute(route, wrapper, localcontext, execAware);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException);
        }
    }

    public void close() {
        this.connManager.shutdown();
        if (this.closeables != null) {
            for (final Closeable closeable: this.closeables) {
                try {
                    closeable.close();
                } catch (final IOException ex) {
                    this.log.error(ex.getMessage(), ex);
                }
            }
        }
    }

    public HttpParams getParams() {
        throw new UnsupportedOperationException();
    }

    public ClientConnectionManager getConnectionManager() {

        return new ClientConnectionManager() {

            public void shutdown() {
                connManager.shutdown();
            }

            public ClientConnectionRequest requestConnection(
                    final HttpRoute route, final Object state) {
                throw new UnsupportedOperationException();
            }

            public void releaseConnection(
                    final ManagedClientConnection conn,
                    final long validDuration, final TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }

            public SchemeRegistry getSchemeRegistry() {
                throw new UnsupportedOperationException();
            }

            public void closeIdleConnections(final long idletime, final TimeUnit tunit) {
                connManager.closeIdleConnections(idletime, tunit);
            }

            public void closeExpiredConnections() {
                connManager.closeExpiredConnections();
            }

        };

    }

}
