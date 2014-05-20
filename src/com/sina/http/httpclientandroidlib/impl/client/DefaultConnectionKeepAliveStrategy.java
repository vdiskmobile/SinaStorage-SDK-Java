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

import com.sina.http.httpclientandroidlib.HeaderElement;
import com.sina.http.httpclientandroidlib.HeaderElementIterator;
import com.sina.http.httpclientandroidlib.HttpResponse;
import com.sina.http.httpclientandroidlib.annotation.Immutable;
import com.sina.http.httpclientandroidlib.conn.ConnectionKeepAliveStrategy;
import com.sina.http.httpclientandroidlib.message.BasicHeaderElementIterator;
import com.sina.http.httpclientandroidlib.protocol.HTTP;
import com.sina.http.httpclientandroidlib.protocol.HttpContext;
import com.sina.http.httpclientandroidlib.util.Args;

/**
 * Default implementation of a strategy deciding duration
 * that a connection can remain idle.
 *
 * The default implementation looks solely at the 'Keep-Alive'
 * header's timeout token.
 *
 * @since 4.0
 */
@Immutable
public class DefaultConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    public static final DefaultConnectionKeepAliveStrategy INSTANCE = new DefaultConnectionKeepAliveStrategy();

    public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
        Args.notNull(response, "HTTP response");
        final HeaderElementIterator it = new BasicHeaderElementIterator(
                response.headerIterator(HTTP.CONN_KEEP_ALIVE));
        while (it.hasNext()) {
            final HeaderElement he = it.nextElement();
            final String param = he.getName();
            final String value = he.getValue();
            if (value != null && param.equalsIgnoreCase("timeout")) {
                try {
                    return Long.parseLong(value) * 1000;
                } catch(final NumberFormatException ignore) {
                }
            }
        }
        return -1;
    }

}
