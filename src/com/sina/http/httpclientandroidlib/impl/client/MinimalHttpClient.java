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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.sina.http.httpclientandroidlib.HttpException;
import com.sina.http.httpclientandroidlib.HttpHost;
import com.sina.http.httpclientandroidlib.HttpRequest;
import com.sina.http.httpclientandroidlib.annotation.ThreadSafe;
import com.sina.http.httpclientandroidlib.client.ClientProtocolException;
import com.sina.http.httpclientandroidlib.client.config.RequestConfig;
import com.sina.http.httpclientandroidlib.client.methods.CloseableHttpResponse;
import com.sina.http.httpclientandroidlib.client.methods.Configurable;
import com.sina.http.httpclientandroidlib.client.methods.HttpExecutionAware;
import com.sina.http.httpclientandroidlib.client.methods.HttpRequestWrapper;
import com.sina.http.httpclientandroidlib.client.protocol.HttpClientContext;
import com.sina.http.httpclientandroidlib.conn.ClientConnectionManager;
import com.sina.http.httpclientandroidlib.conn.ClientConnectionRequest;
import com.sina.http.httpclientandroidlib.conn.HttpClientConnectionManager;
import com.sina.http.httpclientandroidlib.conn.ManagedClientConnection;
import com.sina.http.httpclientandroidlib.conn.routing.HttpRoute;
import com.sina.http.httpclientandroidlib.conn.scheme.SchemeRegistry;
import com.sina.http.httpclientandroidlib.impl.DefaultConnectionReuseStrategy;
import com.sina.http.httpclientandroidlib.impl.execchain.MinimalClientExec;
import com.sina.http.httpclientandroidlib.params.BasicHttpParams;
import com.sina.http.httpclientandroidlib.params.HttpParams;
import com.sina.http.httpclientandroidlib.protocol.BasicHttpContext;
import com.sina.http.httpclientandroidlib.protocol.HttpContext;
import com.sina.http.httpclientandroidlib.protocol.HttpRequestExecutor;
import com.sina.http.httpclientandroidlib.util.Args;

/**
 * Internal class.
 *
 * @since 4.3
 */
@ThreadSafe
@SuppressWarnings("deprecation")
class MinimalHttpClient extends CloseableHttpClient {

    private final HttpClientConnectionManager connManager;
    private final MinimalClientExec requestExecutor;
    private final HttpParams params;

    public MinimalHttpClient(
            final HttpClientConnectionManager connManager) {
        super();
        this.connManager = Args.notNull(connManager, "HTTP connection manager");
        this.requestExecutor = new MinimalClientExec(
                new HttpRequestExecutor(),
                connManager,
                DefaultConnectionReuseStrategy.INSTANCE,
                DefaultConnectionKeepAliveStrategy.INSTANCE);
        this.params = new BasicHttpParams();
    }

    @Override
    protected CloseableHttpResponse doExecute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(target, "Target host");
        Args.notNull(request, "HTTP request");
        HttpExecutionAware execAware = null;
        if (request instanceof HttpExecutionAware) {
            execAware = (HttpExecutionAware) request;
        }
        try {
            final HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request);
            final HttpClientContext localcontext = HttpClientContext.adapt(
                context != null ? context : new BasicHttpContext());
            final HttpRoute route = new HttpRoute(target);
            RequestConfig config = null;
            if (request instanceof Configurable) {
                config = ((Configurable) request).getConfig();
            }
            if (config != null) {
                localcontext.setRequestConfig(config);
            }
            return this.requestExecutor.execute(route, wrapper, localcontext, execAware);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException);
        }
    }

    public HttpParams getParams() {
        return this.params;
    }

    public void close() {
        this.connManager.shutdown();
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
