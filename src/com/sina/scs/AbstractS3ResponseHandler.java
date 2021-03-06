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
package com.sina.scs;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sina.Headers;
import com.sina.ResponseMetadata;
import com.sina.SCSWebServiceResponse;
import com.sina.http.HttpResponse;
import com.sina.http.HttpResponseHandler;
import com.sina.scs.model.ObjectMetadata;
import com.sina.util.DateUtils;


/**
 * Abstract HTTP response handler for Amazon S3 responses. Provides common
 * utilities that other specialized S3 response handlers need to share such as
 * pulling common response metadata (ex: request IDs) out of headers.
 *
 * @param <T>
 *            The output type resulting from handling a response.
 */
public abstract class AbstractS3ResponseHandler<T>
        implements HttpResponseHandler<SCSWebServiceResponse<T>> {

    /** Shared logger */
    private static final Log log = LogFactory.getLog(S3MetadataResponseHandler.class);

    /** The set of response headers that aren't part of the object's metadata */
    private static final Set<String> ignoredHeaders;

    static {
        ignoredHeaders = new HashSet<String>();
        ignoredHeaders.add(Headers.DATE);
        ignoredHeaders.add(Headers.SERVER);
        ignoredHeaders.add(Headers.REQUEST_ID);
        ignoredHeaders.add(Headers.EXTENDED_REQUEST_ID);
    }

    /**
     * The majority of S3 response handlers read the complete response while
     * handling it, and don't need to manually manage the underlying HTTP
     * connection.
     *
     * @see com.amazonaws.http.HttpResponseHandler#needsConnectionLeftOpen()
     */
    public boolean needsConnectionLeftOpen() {
        return false;
    }

    /**
     * Parses the S3 response metadata (ex: AWS request ID) from the specified
     * response, and returns a AmazonWebServiceResponse<T> object ready for the
     * result to be plugged in.
     *
     * @param response
     *            The response containing the response metadata to pull out.
     *
     * @return A new, populated AmazonWebServiceResponse<T> object, ready for
     *         the result to be plugged in.
     */
    protected SCSWebServiceResponse<T> parseResponseMetadata(HttpResponse response) {
    	SCSWebServiceResponse<T> awsResponse = new SCSWebServiceResponse<T>();
        String awsRequestId = response.getHeaders().get(Headers.REQUEST_ID);
        String hostId = response.getHeaders().get(Headers.EXTENDED_REQUEST_ID);

        Map<String, String> metadataMap = new HashMap<String, String>();
        metadataMap.put(ResponseMetadata.AWS_REQUEST_ID, awsRequestId);
        metadataMap.put(S3ResponseMetadata.HOST_ID, hostId);
        awsResponse.setResponseMetadata(new S3ResponseMetadata(metadataMap));

        return awsResponse;
    }

    /**
     * Populates the specified S3ObjectMetadata object with all object metadata
     * pulled from the headers in the specified response.
     *
     * @param response
     *            The HTTP response containing the object metadata within the
     *            headers.
     * @param metadata
     *            The metadata object to populate from the response's headers.
     */
    protected void populateObjectMetadata(HttpResponse response, ObjectMetadata metadata) {
        for (Entry<String, String> header : response.getHeaders().entrySet()) {
            String key = header.getKey();
            if (key.startsWith(Headers.S3_USER_METADATA_PREFIX)) {
                key = key.substring(Headers.S3_USER_METADATA_PREFIX.length());
                metadata.addUserMetadata(key, header.getValue());
            } else if (ignoredHeaders.contains(key)) {
                // ignore...
            } else if (key.equals(Headers.LAST_MODIFIED)) {
                try {
                    metadata.setHeader(key, ServiceUtils.parseRfc822Date(header.getValue()));
                } catch (ParseException pe) {
                    log.warn("Unable to parse last modified date: " + header.getValue(), pe);
                }
            } else if (key.equals(Headers.CONTENT_LENGTH)) {
                try {
                    metadata.setHeader(key, Long.parseLong(header.getValue()));
                } catch (NumberFormatException nfe) {
                    log.warn("Unable to parse content length: " + header.getValue(), nfe);
                }
            } else if (key.equals(Headers.ETAG)) {
                metadata.setHeader(key, ServiceUtils.removeQuotes(header.getValue()));
            } else if (key.equals(Headers.EXPIRES)) {
                try {
                    metadata.setHttpExpiresDate(new DateUtils().parseRfc822Date(header.getValue()));
                } catch (ParseException pe) {
                    log.warn("Unable to parse http expiration date: " + header.getValue(), pe);
                }
            } else if (key.equals(Headers.EXPIRATION)) {
                new ObjectExpirationHeaderHandler<ObjectMetadata>().handle(metadata, response);
//            } else if (key.equals(Headers.RESTORE)) {
//                new ObjectRestoreHeaderHandler<ObjectRestoreResult>().handle(metadata, response);
            } else {
                metadata.setHeader(key, header.getValue());
            }
        }
    }

}
