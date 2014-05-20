/*
 * Copyright 2011-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.sina.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.sina.ClientConfiguration;
import com.sina.SCSClientException;
import com.sina.http.httpclientandroidlib.Header;
import com.sina.http.httpclientandroidlib.HttpHost;
import com.sina.http.httpclientandroidlib.HttpRequest;
import com.sina.http.httpclientandroidlib.HttpResponse;
import com.sina.http.httpclientandroidlib.HttpStatus;
import com.sina.http.httpclientandroidlib.ProtocolException;
import com.sina.http.httpclientandroidlib.auth.AuthScope;
import com.sina.http.httpclientandroidlib.auth.NTCredentials;
import com.sina.http.httpclientandroidlib.client.HttpClient;
import com.sina.http.httpclientandroidlib.conn.ConnectTimeoutException;
import com.sina.http.httpclientandroidlib.conn.params.ConnRoutePNames;
import com.sina.http.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import com.sina.http.httpclientandroidlib.conn.scheme.Scheme;
import com.sina.http.httpclientandroidlib.conn.scheme.SchemeLayeredSocketFactory;
import com.sina.http.httpclientandroidlib.conn.scheme.SchemeRegistry;
import com.sina.http.httpclientandroidlib.conn.scheme.SchemeSocketFactory;
import com.sina.http.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import com.sina.http.httpclientandroidlib.impl.client.DefaultRedirectStrategy;
import com.sina.http.httpclientandroidlib.impl.conn.PoolingClientConnectionManager;
import com.sina.http.httpclientandroidlib.params.BasicHttpParams;
import com.sina.http.httpclientandroidlib.params.HttpConnectionParams;
import com.sina.http.httpclientandroidlib.params.HttpParams;
import com.sina.http.httpclientandroidlib.protocol.HttpContext;
import com.sina.http.impl.client.SdkHttpClient;

/** Responsible for creating and configuring instances of Apache HttpClient4. */
class HttpClientFactory {


    /**
     * Creates a new HttpClient object using the specified AWS
     * ClientConfiguration to configure the client.
     *
     * @param config
     *            Client configuration options (ex: proxy settings, connection
     *            limits, etc).
     *
     * @return The new, configured HttpClient.
     */
	@SuppressWarnings("deprecation")
    public HttpClient createHttpClient(ClientConfiguration config) {
        /* Set HTTP client parameters */
		HttpParams httpClientParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpClientParams, config.getConnectionTimeout());
        HttpConnectionParams.setSoTimeout(httpClientParams, config.getSocketTimeout());
        HttpConnectionParams.setStaleCheckingEnabled(httpClientParams, true);
        HttpConnectionParams.setTcpNoDelay(httpClientParams, true);

        int socketSendBufferSizeHint = config.getSocketBufferSizeHints()[0];
        int socketReceiveBufferSizeHint = config.getSocketBufferSizeHints()[1];
        if (socketSendBufferSizeHint > 0 || socketReceiveBufferSizeHint > 0) {
            HttpConnectionParams.setSocketBufferSize(httpClientParams,
                    Math.max(socketSendBufferSizeHint, socketReceiveBufferSizeHint));
        }

        PoolingClientConnectionManager connectionManager = ConnectionManagerFactory
                .createPoolingClientConnManager(config, httpClientParams);
        SdkHttpClient httpClient = new SdkHttpClient(connectionManager, httpClientParams);
        if(config.getMaxErrorRetry() > 0)
        	httpClient.setHttpRequestRetryHandler(config.getRetryPolicy());
//        httpClient.setRedirectStrategy(new LocationHeaderNotRequiredRedirectStrategy());

        try {
            Scheme http = new Scheme("http", PlainSocketFactory.getSocketFactory(), 80);
            SSLSocketFactory sf = new SSLSocketFactory(SSLContext.getDefault(),
                    SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
            
            Scheme https = new Scheme("https", sf, 443);
            SchemeRegistry sr = connectionManager.getSchemeRegistry();
            sr.register(http);
            sr.register(https);
        } catch (NoSuchAlgorithmException e) {
            throw new SCSClientException("Unable to access default SSL context", e);
        }

//        /* TODO:SSL cert
//         * If SSL cert checking for endpoints has been explicitly disabled,
//         * register a new scheme for HTTPS that won't cause self-signed certs to
//         * error out.
//         */
//        if (System.getProperty(DISABLE_CERT_CHECKING_SYSTEM_PROPERTY) != null) {
            Scheme sch = new Scheme("https", 443, new TrustingSocketFactory());
            httpClient.getConnectionManager().getSchemeRegistry().register(sch);
//        }

        /* Set proxy if configured */
        String proxyHost = config.getProxyHost();
        int proxyPort = config.getProxyPort();
        if (proxyHost != null && proxyPort > 0) {
//            AmazonHttpClient.log.info("Configuring Proxy. Proxy Host: " + proxyHost + " " + "Proxy Port: " + proxyPort);
            HttpHost proxyHttpHost = new HttpHost(proxyHost, proxyPort);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHttpHost);

            String proxyUsername    = config.getProxyUsername();
            String proxyPassword    = config.getProxyPassword();
            String proxyDomain      = config.getProxyDomain();
            String proxyWorkstation = config.getProxyWorkstation();

            if (proxyUsername != null && proxyPassword != null) {
                httpClient.getCredentialsProvider().setCredentials(
                        new AuthScope(proxyHost, proxyPort),
                        new NTCredentials(proxyUsername, proxyPassword, proxyWorkstation, proxyDomain));
            }
        }

        return httpClient;
    }

    /**
     * Customization of the default redirect strategy provided by HttpClient to be a little
     * less strict about the Location header to account for S3 not sending the Location
     * header with 301 responses.
     */
    private static final class LocationHeaderNotRequiredRedirectStrategy
            extends DefaultRedirectStrategy {

        @Override
        public boolean isRedirected(HttpRequest request,
                HttpResponse response, HttpContext context) throws ProtocolException {
            int statusCode = response.getStatusLine().getStatusCode();
            Header locationHeader = response.getFirstHeader("location");

            // Instead of throwing a ProtocolException in this case, just
            // return false to indicate that this is not redirected
            if (locationHeader == null &&
                statusCode == HttpStatus.SC_MOVED_PERMANENTLY) return false;

            return super.isRedirected(request, response, context);
        }
    }

    /**
     * Simple implementation of SchemeSocketFactory (and
     * LayeredSchemeSocketFactory) that bypasses SSL certificate checks. This
     * class is only intended to be used for testing purposes.
     */
    private static class TrustingSocketFactory implements SchemeSocketFactory, SchemeLayeredSocketFactory {

        private SSLContext sslcontext = null;

        private static SSLContext createSSLContext() throws IOException {
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[] { new TrustingX509TrustManager() }, null);
                return context;
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        private SSLContext getSSLContext() throws IOException {
            if (this.sslcontext == null) this.sslcontext = createSSLContext();
            return this.sslcontext;
        }

        public Socket createSocket(HttpParams params) throws IOException {
            return getSSLContext().getSocketFactory().createSocket();
        }

        public Socket connectSocket(Socket sock,
                InetSocketAddress remoteAddress,
                InetSocketAddress localAddress, HttpParams params)
                throws IOException, UnknownHostException,
                ConnectTimeoutException {
            int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
            int soTimeout = HttpConnectionParams.getSoTimeout(params);

            SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket(params));
            if (localAddress != null) sslsock.bind(localAddress);

            sslsock.connect(remoteAddress, connTimeout);
            sslsock.setSoTimeout(soTimeout);
            return sslsock;
        }

        public boolean isSecure(Socket sock) throws IllegalArgumentException {
            return true;
        }

        public Socket createLayeredSocket(Socket arg0, String arg1, int arg2, HttpParams arg3)
                throws IOException, UnknownHostException {
            return getSSLContext().getSocketFactory().createSocket(arg0, arg1, arg2, true);
        }
    }

    /**
     * Simple implementation of X509TrustManager that trusts all certificates.
     * This class is only intended to be used for testing purposes.
     */
    private static class TrustingX509TrustManager implements X509TrustManager {
        private static final X509Certificate[] X509_CERTIFICATES = new X509Certificate[0];

        public X509Certificate[] getAcceptedIssuers() {
            return X509_CERTIFICATES;
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // No-op, to trust all certs
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // No-op, to trust all certs
        }
    };
}