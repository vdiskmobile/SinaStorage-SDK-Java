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

package com.sina.http.httpclientandroidlib.impl.execchain;

import java.io.IOException;

import com.sina.http.httpclientandroidlib.Header;
import com.sina.http.httpclientandroidlib.HttpException;
import com.sina.http.httpclientandroidlib.androidextra.HttpClientAndroidLog;
import com.sina.http.httpclientandroidlib.annotation.Immutable;
import com.sina.http.httpclientandroidlib.client.HttpRequestRetryHandler;
import com.sina.http.httpclientandroidlib.client.NonRepeatableRequestException;
import com.sina.http.httpclientandroidlib.client.methods.CloseableHttpResponse;
import com.sina.http.httpclientandroidlib.client.methods.HttpExecutionAware;
import com.sina.http.httpclientandroidlib.client.methods.HttpRequestWrapper;
import com.sina.http.httpclientandroidlib.client.protocol.HttpClientContext;
import com.sina.http.httpclientandroidlib.conn.routing.HttpRoute;
import com.sina.http.httpclientandroidlib.util.Args;

/* LogFactory removed by HttpClient for Android script. */

/**
 * Request executor in the request execution chain that is responsible
 * for making a decision whether a request failed due to an I/O error
 * should be re-executed.
 * <p/>
 * Further responsibilities such as communication with the opposite
 * endpoint is delegated to the next executor in the request execution
 * chain.
 *
 * @since 4.3
 */
@Immutable
public class RetryExec implements ClientExecChain {

    public HttpClientAndroidLog log = new HttpClientAndroidLog(getClass());

    private final ClientExecChain requestExecutor;
    private final HttpRequestRetryHandler retryHandler;

    public RetryExec(
            final ClientExecChain requestExecutor,
            final HttpRequestRetryHandler retryHandler) {
        Args.notNull(requestExecutor, "HTTP request executor");
        Args.notNull(retryHandler, "HTTP request retry handler");
        this.requestExecutor = requestExecutor;
        this.retryHandler = retryHandler;
    }

    public CloseableHttpResponse execute(
            final HttpRoute route,
            final HttpRequestWrapper request,
            final HttpClientContext context,
            final HttpExecutionAware execAware) throws IOException, HttpException {
        Args.notNull(route, "HTTP route");
        Args.notNull(request, "HTTP request");
        Args.notNull(context, "HTTP context");
        final Header[] origheaders = request.getAllHeaders();
        for (int execCount = 1;; execCount++) {
            try {
                return this.requestExecutor.execute(route, request, context, execAware);
            } catch (final IOException ex) {
                if (execAware != null && execAware.isAborted()) {
                    this.log.debug("Request has been aborted");
                    throw ex;
                }
                if (retryHandler.retryRequest(ex, execCount, context)) {
                    if (this.log.isInfoEnabled()) {
                        this.log.info("I/O exception ("+ ex.getClass().getName() +
                                ") caught when processing request: "
                                + ex.getMessage());
                    }
                    if (this.log.isDebugEnabled()) {
                        this.log.debug(ex.getMessage(), ex);
                    }
                    if (!Proxies.isRepeatable(request)) {
                        this.log.debug("Cannot retry non-repeatable request");
                        throw new NonRepeatableRequestException("Cannot retry request " +
                                "with a non-repeatable request entity", ex);
                    }
                    request.setHeaders(origheaders);
                    this.log.info("Retrying request");
                } else {
                    throw ex;
                }
            }
        }
    }

}
