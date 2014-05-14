/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.sina;

//import static com.sina.SDKGlobalConfiguration.PROFILING_SYSTEM_PROPERTY;

import static com.sina.SDKGlobalConfiguration.PROFILING_SYSTEM_PROPERTY;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import javax.swing.plaf.synth.Region;

import com.sina.http.ExecutionContext;
import com.sina.http.HttpConnect;
import com.sina.http.HttpMethodName;
import com.sina.http.HttpRequest;


/**
 * Abstract base class for Amazon Web Service Java clients.
 * <p>
 * Responsible for basic client capabilities that are the same across all AWS
 * SDK Java clients (ex: setting the client endpoint).
 */
public abstract class SCSWebServiceClient {

    /** The service endpoint to which this client will send requests. */
    protected URI endpoint;

    /** The client configuration */
    protected ClientConfiguration clientConfiguration;

    /** Low level client for sending requests to AWS services. */
    protected HttpConnect client;

//    /** Optional request handlers for additional request processing. */
//    protected final List<RequestHandler2> requestHandler2s;
    
    /** Optional offset (in seconds) to use when signing requests */
    protected int timeOffset;

//    /** AWS signer for authenticating requests. */
//    private Signer signer;

    /**
     * Constructs a new SCSWebServiceClient object using the specified
     * configuration and request metric collector.
     * 
     * @param clientConfiguration
     *            The client configuration for this client.
     * @param requestMetricCollector
     *            optional request metric collector to be used at the http
     *            client level; can be null.
     */
    public SCSWebServiceClient(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
        client = new HttpConnect(clientConfiguration);
//        requestHandler2s = new CopyOnWriteArrayList<RequestHandler2>();
    }
    
//    protected Signer getSigner() { return signer; }

    /**
     * Overrides the default endpoint for this client. Callers can use this
     * method to control which AWS region they want to work with.
     * <p>
     * <b>This method is not threadsafe. Endpoints should be configured when the
     * client is created and before any service requests are made. Changing it
     * afterwards creates inevitable race conditions for any service requests in
     * transit.</b>
     * <p>
     * Callers can pass in just the endpoint (ex: "ec2.amazonaws.com") or a full
     * URL, including the protocol (ex: "https://ec2.amazonaws.com"). If the
     * protocol is not specified here, the default protocol from this client's
     * {@link ClientConfiguration} will be used, which by default is HTTPS.
     * <p>
     * For more information on using AWS regions with the AWS SDK for Java, and
     * a complete list of all available endpoints for all AWS services, see: 
     * <a href="http://developer.amazonwebservices.com/connect/entry.jspa?externalID=3912">
     * http://developer.amazonwebservices.com/connect/entry.jspa?externalID=3912</a>
     * 
     * @param endpoint
     *            The endpoint (ex: "ec2.amazonaws.com") or a full URL,
     *            including the protocol (ex: "https://ec2.amazonaws.com") of
     *            the region specific AWS endpoint this client will communicate
     *            with.
     * @throws IllegalArgumentException
     *             If any problems are detected with the specified endpoint.
     */
    public void setEndpoint(String endpoint) throws IllegalArgumentException {
        URI uri = configEndpoint(endpoint);
//        configSigner(uri);
    }

    /** Sets and returns the endpoint as a URI. */
    private URI configEndpoint(String endpoint) throws IllegalArgumentException {
        /*
         * If the endpoint doesn't explicitly specify a protocol to use, then
         * we'll defer to the default protocol specified in the client
         * configuration.
         */
        if (endpoint.contains("://") == false) {
            endpoint = clientConfiguration.getProtocol().toString() + "://" + endpoint;
        }

        try {
            return this.endpoint = new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Overrides the default endpoint for this client ("http://dynamodb.us-east-1.amazonaws.com/") and explicitly provides
     * an AWS region ID and AWS service name to use when the client calculates a signature
     * for requests.  In almost all cases, this region ID and service name
     * are automatically determined from the endpoint, and callers should use the simpler
     * one-argument form of setEndpoint instead of this method.
     * <p>
     * <b>This method is not threadsafe. Endpoints should be configured when the
     * client is created and before any service requests are made. Changing it
     * afterwards creates inevitable race conditions for any service requests in
     * transit.</b>
     * <p>
     * Callers can pass in just the endpoint (ex: "dynamodb.us-east-1.amazonaws.com/") or a full
     * URL, including the protocol (ex: "http://dynamodb.us-east-1.amazonaws.com/"). If the
     * protocol is not specified here, the default protocol from this client's
     * {@link ClientConfiguration} will be used, which by default is HTTPS.
     * <p>
     * For more information on using AWS regions with the AWS SDK for Java, and
     * a complete list of all available endpoints for all AWS services, see:
     * <a href="http://developer.amazonwebservices.com/connect/entry.jspa?externalID=3912">
     * http://developer.amazonwebservices.com/connect/entry.jspa?externalID=3912</a>
     *
     * @param endpoint
     *            The endpoint (ex: "dynamodb.us-east-1.amazonaws.com/") or a full URL,
     *            including the protocol (ex: "http://dynamodb.us-east-1.amazonaws.com/") of
     *            the region specific AWS endpoint this client will communicate
     *            with.
     * @param serviceName
     *            The name of the AWS service to use when signing requests.
     *
     * @throws IllegalArgumentException
     *             If any problems are detected with the specified endpoint.
     * @see AmazonDynamoDB#setRegion(Region)
     */
    public void setEndpoint(String endpoint, String serviceName) {
        configEndpoint(endpoint);
//        configSigner(serviceName);
    }

//    /**
//     * Configures the signer by the given URI.
//     */
//    protected void configSigner(URI uri) {
////        String service = getServiceNameIntern();
////        signer = SignerFactory.getSigner(service, region);
//    	signer = new S3Signer(httpVerb, resourcePath)
//    }

//    /**
//     * Configures the signer by the given service and region.
//     */
//    protected void configSigner(String serviceName) {
//        signer = SignerFactory.getSigner(serviceName);
//    }

    public void setConfiguration(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
        client = new HttpConnect(clientConfiguration);
    }

    /**
     * Shuts down this client object, releasing any resources that might be held
     * open. This is an optional method, and callers are not expected to call
     * it, but can if they want to explicitly release any open resources. Once a
     * client has been shutdown, it should not be used to make any more
     * requests.
     */
    public void shutdown() {
        client.shutdown();
    }

    /**
     * Converts a Request<T> object into an HttpRequest object. Copies all the
     * headers, parameters, etc. from the Request into the new HttpRequest.
     *
     * @param request
     *            The request to convert.
     * @param methodName
     *            The HTTP method (GET, PUT, DELETE, HEAD) to use in the
     *            converted HttpRequest object.
     *
     * @return A new HttpRequest object created from the details of the
     *         specified Request<T> object.
     */
    @Deprecated
    protected <T> HttpRequest convertToHttpRequest(Request<T> request, HttpMethodName methodName) {
        HttpRequest httpRequest = new HttpRequest(methodName);
        for (Entry<String, String> parameter : request.getParameters().entrySet()) {
            httpRequest.addParameter(parameter.getKey(), parameter.getValue());
        }

        for (Entry<String, String> parameter : request.getHeaders().entrySet()) {
            httpRequest.addHeader(parameter.getKey(), parameter.getValue());
        }

        httpRequest.setServiceName(request.getServiceName());
        httpRequest.setEndpoint(request.getEndpoint());
        httpRequest.setResourcePath(request.getResourcePath());
        httpRequest.setOriginalRequest(request.getOriginalRequest());

        return httpRequest;
    }

//    /**
//     * @deprecated by {@link #addRequestHandler(RequestHandler2)}.
//     * 
//     * Appends a request handler to the list of registered handlers that are run
//     * as part of a request's lifecycle.
//     *
//     * @param requestHandler
//     *            The new handler to add to the current list of request
//     *            handlers.
//     */
//    @Deprecated
//    public void addRequestHandler(RequestHandler requestHandler) {
//    	requestHandler2s.add(RequestHandler2.adapt(requestHandler));
//    }

//    /**
//     * Appends a request handler to the list of registered handlers that are run
//     * as part of a request's lifecycle.
//     *
//     * @param requestHandler2
//     *            The new handler to add to the current list of request
//     *            handlers.
//     */
//    public void addRequestHandler(RequestHandler2 requestHandler2) {
//        requestHandler2s.add(requestHandler2);
//    }

//    /**
//     * Removes a request handler from the list of registered handlers that are run
//     * as part of a request's lifecycle.
//     *
//     * @param requestHandler
//     *            The handler to remove from the current list of request
//     *            handlers.
//     */
//    public void removeRequestHandler(RequestHandler requestHandler) {
//        requestHandler2s.remove(RequestHandler2.adapt(requestHandler));
//    }
//
//    public void removeRequestHandler(RequestHandler2 requestHandler2) {
//        requestHandler2s.remove(requestHandler2);
//    }

//    protected final ExecutionContext createExecutionContext(SCSWebServiceRequest req) {
////        boolean isMetricsEnabled = isRequestMetricsEnabled(req) || isProfilingEnabled();
//        return new ExecutionContext();
//    }

    protected final ExecutionContext createExecutionContext(Request<?> req) {
//        return createExecutionContext(req.getOriginalRequest());
    	return createExecutionContext();
    }

    /**
     *             This method exists only for backward compatiblity reason, so
     *             that clients compiled against the older version of this class
     *             won't get {@link NoSuchMethodError} at runtime. However,
     *             calling this methods would effectively ignore and disable the
     *             request metric collector, if any, specified at the request
     *             level. Request metric collector specified at the service
     *             client or AWS SDK level will still be honored.
     */
    protected final ExecutionContext createExecutionContext() {
//        boolean isMetricsEnabled = isRMCEnabledAtClientOrSdkLevel() || isProfilingEnabled();
//        return new ExecutionContext(requestHandler2s, isMetricsEnabled);
    	
    	return new ExecutionContext();
    }

    /* Check the profiling system property and return true if set */
    private static boolean isProfilingEnabled() {
        return System.getProperty(PROFILING_SYSTEM_PROPERTY) != null;
    }
    
//    /**
//     * Returns true if request metric collection is applicable to the given
//     * request; false otherwise.
//     */
//    private boolean isRequestMetricsEnabled(SCSWebServiceRequest req) {
//        RequestMetricCollector c = req.getRequestMetricCollector(); // request level collector
//        if (c != null && c.isEnabled()) {
//            return true;
//        }
//        return isRMCEnabledAtClientOrSdkLevel();
//    }

//    /**
//     * Returns true if request metric collection is enabled at the service
//     * client or AWS SDK level request; false otherwise.
//     */
//    private boolean isRMCEnabledAtClientOrSdkLevel() {
//        RequestMetricCollector c = requestMetricCollector();
//        return c != null && c.isEnabled();
//    }
    
    /**
     * Sets the optional value for time offset for this client.  This
     * value will be applied to all requests processed through this client.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @param timeOffset
     *            The optional value for time offset (in seconds) for this client.
     */
    public void setTimeOffset(int timeOffset) {
        this.timeOffset = timeOffset;
    }
    
    /**
     * Sets the optional value for time offset for this client.  This
     * value will be applied to all requests processed through this client.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @param timeOffset
     *            The optional value for time offset (in seconds) for this client.
     * 
     * @return the updated web service client
     */
    public SCSWebServiceClient withTimeOffset(int timeOffset) {
        setTimeOffset(timeOffset);
        return this;
    }
    
    /**
     * Returns the optional value for time offset for this client.  This
     * value will be applied to all requests processed through this client.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @return The optional value for time offset (in seconds) for this client.
     */
    public int getTimeOffset() {
        return timeOffset;
    }

//    /**
//     * Returns the client specific {@link RequestMetricCollector}; or null if
//     * there is none.
//     */
//    public RequestMetricCollector getRequestMetricsCollector() {
//        return client.getRequestMetricCollector();
//    }

//    /**
//     * Returns the client specific request metric collector if there is one; or
//     * the one at the AWS SDK level otherwise.
//     */
//    protected RequestMetricCollector requestMetricCollector() {
//        RequestMetricCollector mc = client.getRequestMetricCollector();
//        return mc == null ? AwsSdkMetrics.getRequestMetricCollector() : mc;
//    }

//    /**
//     * Returns the most specific request metric collector, starting from the
//     * request level, then client level, then finally the AWS SDK level.
//     */
//    protected final RequestMetricCollector findRequestMetricCollector(Request<?> req) {
//        AmazonWebServiceRequest origReq = req.getOriginalRequest();
//        RequestMetricCollector mc = origReq.getRequestMetricCollector();
//        if (mc != null) {
//            return mc;
//        }
//        mc = getRequestMetricsCollector();
//        return mc == null ? AwsSdkMetrics.getRequestMetricCollector() : mc;
//    }

//    /**
//     * Common routine to end a client AWS request/response execution and collect
//     * the request metrics.  Caller of this routine is responsible for starting
//     * the event for {@link Field#ClientExecuteTime} and call this method
//     * in a try-finally block. 
//     */
//    protected final void endClientExecution(AWSRequestMetrics awsRequestMetrics,
//            Request<?> request, Response<?> response) {
//        if (request != null) {
//            awsRequestMetrics.endEvent(Field.ClientExecuteTime);
//            awsRequestMetrics.getTimingInfo().endTiming();
//            RequestMetricCollector c = findRequestMetricCollector(request);
//            c.collectMetrics(request, response);
//        }
//    }

//    /**
//     * @deprecated by {@link #getServiceName()}.
//     */
//    @Deprecated
//    protected String getServiceAbbreviation() {
//        return getServiceNameIntern();
//    }

//    /**
//     * Returns the service abbreviation for this service, used for identifying
//     * service endpoints by region, identifying the necessary signer, etc.
//     * Used to be call "getServiceAbbreviation".
//     */
//    public String getServiceName() {
//        return getServiceNameIntern();
//    }

//    /**
//     * Internal method for implementing {@link #getServiceName()}.
//     */
//    private String getServiceNameIntern() {
//        if (serviceName == null) {
//            synchronized(this) {
//                if (serviceName == null) {
//                    return serviceName = computeServiceName();
//                }
//            }
//        }
//        return serviceName;
//    }

//    /**
//     * Returns the service name of this AWS http client by first looking it up
//     * from the SDK internal configuration, and if not found, derive it from the
//     * class name of the immediate subclass of {@link SCSWebServiceClient}.
//     * No configuration is necessary if the simple class name of the http client
//     * follows the convention of <code>(Amazon|AWS).*(JavaClient|Client)</code>.
//     */
//    private String computeServiceName() {
//        Class<?> httpClientClass = Classes.childClassOf(
//                SCSWebServiceClient.class, this);
//        final String httpClientName = httpClientClass.getSimpleName();
//        String service = ServiceNameFactory.getServiceName(httpClientName);
//        if (service != null) {
//            return service; // only if it is so explicitly configured
//        }
//        // Otherwise, make use of convention over configuration
//        int j = httpClientName.indexOf("JavaClient");
//        if (j == -1) {
//            j = httpClientName.indexOf("Client");
//            if (j == -1) {
//                throw new IllegalStateException(
//                        "Unrecognized suffix for the AWS http client class name "
//                                + httpClientName);
//            }
//        }
//        int i = httpClientName.indexOf(AMAZON);
//        int len;
//        if (i == -1) {
//            i = httpClientName.indexOf(AWS);
//            if (i == -1) {
//                throw new IllegalStateException(
//                        "Unrecognized prefix for the AWS http client class name "
//                                + httpClientName);
//            }
//            len = AWS.length();
//        } else {
//            len = AMAZON.length();
//        }
//        if (i >= j) {
//            throw new IllegalStateException(
//                    "Unrecognized AWS http client class name " + httpClientName);
//        }
//        String serviceName = httpClientName.substring(i + len, j);
//        return serviceName.toLowerCase();
//    }
}
