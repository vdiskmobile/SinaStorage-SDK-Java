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
package com.sina.http.httpclientandroidlib.impl.conn;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.sina.http.httpclientandroidlib.HttpHost;
import com.sina.http.httpclientandroidlib.androidextra.HttpClientAndroidLog;
import com.sina.http.httpclientandroidlib.annotation.Immutable;
import com.sina.http.httpclientandroidlib.client.protocol.HttpClientContext;
import com.sina.http.httpclientandroidlib.config.Lookup;
import com.sina.http.httpclientandroidlib.config.SocketConfig;
import com.sina.http.httpclientandroidlib.conn.ConnectTimeoutException;
import com.sina.http.httpclientandroidlib.conn.DnsResolver;
import com.sina.http.httpclientandroidlib.conn.HttpClientConnectionManager;
import com.sina.http.httpclientandroidlib.conn.HttpHostConnectException;
import com.sina.http.httpclientandroidlib.conn.ManagedHttpClientConnection;
import com.sina.http.httpclientandroidlib.conn.SchemePortResolver;
import com.sina.http.httpclientandroidlib.conn.UnsupportedSchemeException;
import com.sina.http.httpclientandroidlib.conn.socket.ConnectionSocketFactory;
import com.sina.http.httpclientandroidlib.conn.socket.LayeredConnectionSocketFactory;
import com.sina.http.httpclientandroidlib.protocol.HttpContext;
import com.sina.http.httpclientandroidlib.util.Args;

/* LogFactory removed by HttpClient for Android script. */

@Immutable
class HttpClientConnectionOperator {

    static final String SOCKET_FACTORY_REGISTRY = "http.socket-factory-registry";

    public HttpClientAndroidLog log = new HttpClientAndroidLog(HttpClientConnectionManager.class);

    private final Lookup<ConnectionSocketFactory> socketFactoryRegistry;
    private final SchemePortResolver schemePortResolver;
    private final DnsResolver dnsResolver;

    HttpClientConnectionOperator(
            final Lookup<ConnectionSocketFactory> socketFactoryRegistry,
            final SchemePortResolver schemePortResolver,
            final DnsResolver dnsResolver) {
        super();
        Args.notNull(socketFactoryRegistry, "Socket factory registry");
        this.socketFactoryRegistry = socketFactoryRegistry;
        this.schemePortResolver = schemePortResolver != null ? schemePortResolver :
            DefaultSchemePortResolver.INSTANCE;
        this.dnsResolver = dnsResolver != null ? dnsResolver :
            SystemDefaultDnsResolver.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private Lookup<ConnectionSocketFactory> getSocketFactoryRegistry(final HttpContext context) {
        Lookup<ConnectionSocketFactory> reg = (Lookup<ConnectionSocketFactory>) context.getAttribute(
                SOCKET_FACTORY_REGISTRY);
        if (reg == null) {
            reg = this.socketFactoryRegistry;
        }
        return reg;
    }

    public void connect(
            final ManagedHttpClientConnection conn,
            final HttpHost host,
            final InetSocketAddress localAddress,
            final int connectTimeout,
            final SocketConfig socketConfig,
            final HttpContext context) throws IOException {
        final Lookup<ConnectionSocketFactory> registry = getSocketFactoryRegistry(context);
        final ConnectionSocketFactory sf = registry.lookup(host.getSchemeName());
        if (sf == null) {
            throw new UnsupportedSchemeException(host.getSchemeName() +
                    " protocol is not supported");
        }
        final InetAddress[] addresses = this.dnsResolver.resolve(host.getHostName());
        final int port = this.schemePortResolver.resolve(host);
        for (int i = 0; i < addresses.length; i++) {
            final InetAddress address = addresses[i];
            final boolean last = i == addresses.length - 1;

            Socket sock = sf.createSocket(context);
            sock.setReuseAddress(socketConfig.isSoReuseAddress());
            conn.bind(sock);

            final InetSocketAddress remoteAddress = new InetSocketAddress(address, port);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Connecting to " + remoteAddress);
            }
            try {
                sock.setSoTimeout(socketConfig.getSoTimeout());
                sock = sf.connectSocket(
                        connectTimeout, sock, host, remoteAddress, localAddress, context);
                sock.setTcpNoDelay(socketConfig.isTcpNoDelay());
                sock.setKeepAlive(socketConfig.isSoKeepAlive());
                final int linger = socketConfig.getSoLinger();
                if (linger >= 0) {
                    sock.setSoLinger(linger > 0, linger);
                }
                conn.bind(sock);
                return;
            } catch (final SocketTimeoutException ex) {
                if (last) {
                    throw new ConnectTimeoutException(ex, host, addresses);
                }
            } catch (final ConnectException ex) {
                if (last) {
                    final String msg = ex.getMessage();
                    if ("Connection timed out".equals(msg)) {
                        throw new ConnectTimeoutException(ex, host, addresses);
                    } else {
                        throw new HttpHostConnectException(ex, host, addresses);
                    }
                }
            }
            if (this.log.isDebugEnabled()) {
                this.log.debug("Connect to " + remoteAddress + " timed out. " +
                        "Connection will be retried using another IP address");
            }
        }
    }

    public void upgrade(
            final ManagedHttpClientConnection conn,
            final HttpHost host,
            final HttpContext context) throws IOException {
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final Lookup<ConnectionSocketFactory> registry = getSocketFactoryRegistry(clientContext);
        final ConnectionSocketFactory sf = registry.lookup(host.getSchemeName());
        if (sf == null) {
            throw new UnsupportedSchemeException(host.getSchemeName() +
                    " protocol is not supported");
        }
        if (!(sf instanceof LayeredConnectionSocketFactory)) {
            throw new UnsupportedSchemeException(host.getSchemeName() +
                    " protocol does not support connection upgrade");
        }
        final LayeredConnectionSocketFactory lsf = (LayeredConnectionSocketFactory) sf;
        Socket sock = conn.getSocket();
        final int port = this.schemePortResolver.resolve(host);
        sock = lsf.createLayeredSocket(sock, host.getHostName(), port, context);
        conn.bind(sock);
    }

}
