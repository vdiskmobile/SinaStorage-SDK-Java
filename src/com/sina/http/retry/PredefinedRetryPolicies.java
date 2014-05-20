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
package com.sina.http.retry;

import java.util.Random;

import com.sina.ClientConfiguration;
import com.sina.SCSClientException;
import com.sina.SCSServiceException;
import com.sina.SCSWebServiceRequest;
import com.sina.http.httpclientandroidlib.client.HttpRequestRetryHandler;

/**
 * This class includes a set of pre-defined retry policies, including default
 * policies used by SDK.
 */
public class PredefinedRetryPolicies {
    
    /* SDK default */
    
    /** SDK default max retry count **/
    public static int DEFAULT_MAX_ERROR_RETRY = 3;
    
    /** SDK default retry policy **/
    public static final HttpRequestRetryHandler DEFAULT;
    
    /**
     * The SDK default back-off strategy, which increases exponentially up to a
     * max amount of delay. It also applies a larger scale factor upon service
     * throttling exception.
     */
    public static final SDKDefaultBackoffStrategy DEFAULT_BACKOFF_STRATEGY = new SDKDefaultBackoffStrategy();
    
    static {
        DEFAULT = getDefaultRetryPolicy();
    }
    
    /**
     * Returns the SDK default retry policy. This policy will honor the
     * maxErrorRetry set in ClientConfiguration.
     * 
     * @see ClientConfiguration#setMaxErrorRetry(int)
     */
    public static HttpRequestRetryHandler getDefaultRetryPolicy() {
        return new RetryHandler(DEFAULT_MAX_ERROR_RETRY);
    }
    
    
    /**
     * Returns the SDK default retry policy with the specified max retry count.
     */
    public static HttpRequestRetryHandler getDefaultRetryPolicyWithCustomMaxRetries(int maxErrorRetry) {
        return new RetryHandler(maxErrorRetry);
    }
    
    /** A private class that implements the default back-off strategy. **/
    private static class SDKDefaultBackoffStrategy  {
        
        /** Base sleep time (milliseconds) for general exceptions. **/
        private static final int SCALE_FACTOR = 300;
        
        /** Base sleep time (milliseconds) for throttling exceptions. **/
        private static final int THROTTLING_SCALE_FACTOR = 500;
        
        private static final int THROTTLING_SCALE_FACTOR_RANDOM_RANGE = THROTTLING_SCALE_FACTOR / 4;
        
        /** Maximum exponential back-off time before retrying a request */
        private static final int MAX_BACKOFF_IN_MILLISECONDS = 20 * 1000;
        
        /** For generating a random scale factor **/
        private final Random random = new Random();
        
        /** {@inheritDoc} */
        public final long delayBeforeNextRetry(SCSWebServiceRequest originalRequest,
                                               SCSClientException exception,
                                               int retries) {
            if (retries <= 0) return 0;
            
            int scaleFactor;
            if (exception instanceof SCSServiceException
                    && RetryUtils.isThrottlingException((SCSServiceException)exception)) {
                scaleFactor = THROTTLING_SCALE_FACTOR + random.nextInt(THROTTLING_SCALE_FACTOR_RANDOM_RANGE);
            } else {
                scaleFactor = SCALE_FACTOR;
            }
            
            long delay = (1 << retries) * scaleFactor;
            delay = Math.min(delay, MAX_BACKOFF_IN_MILLISECONDS);
            
            return delay;
        }
    }
}
