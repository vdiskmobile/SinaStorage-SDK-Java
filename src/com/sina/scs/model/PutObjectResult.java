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
package com.sina.scs.model;

import java.util.Date;

import com.sina.scs.SCS;
import com.sina.scs.ObjectExpirationResult;
import com.sina.scs.ServerSideEncryptionResult;

/**
 * Contains the data returned by Amazon S3 from the <code>putObject</code>
 * operation.
 * Use this request to access information about the new object created from the
 * <code>putObject</code> request, such as its ETag and optional version ID.
 *
 * @see SCS#putObject(String, String, java.io.File)
 * @see SCS#putObject(String, String, java.io.InputStream, S3ObjectMetadata)
 * @see SCS#putObject(PutObjectRequest)
 */
public class PutObjectResult implements ServerSideEncryptionResult, ObjectExpirationResult {

    /**
     * The version ID of the new, uploaded object. This field will only be
     * present if object versioning has been enabled for the bucket to which the
     * object was uploaded.
     */
    private String versionId;

    /** The ETag value of the new object */
    private String eTag;

    /** The server side encryption algorithm of the new object */
    private String serverSideEncryption;

    /** The time this object expires, or null if it has no expiration */
    private Date expirationTime;

    /** The expiration rule for this object */
    private String expirationTimeRuleId;

    /** The content MD5 */
    private String contentMd5;
    
    /** for miaopai file download url*/
    private String serviceSideKey;

    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("[PutObjectResult]");
//    	sb.append("\nversionId:"+versionId);
    	sb.append("\neTag:"+eTag);
//    	sb.append("\nserverSideEncryption:"+serverSideEncryption);
    	sb.append("\nexpirationTime:"+expirationTime);
    	sb.append("\nexpirationTimeRuleId:"+expirationTimeRuleId);
    	sb.append("\ncontentMd5:"+contentMd5);
    	sb.append("\nserviceSideKey:"+serviceSideKey);
    	
    	return sb.toString();
    }
    
    
    
    /**
     * Gets the optional version ID of the newly uploaded object. This field will
     * be set only if object versioning is enabled for the bucket the
     * object was uploaded to.
     *
     * @return The optional version ID of the newly uploaded object.
     *
     * @see PutObjectResult#setVersionId(String)
     */
    public String getVersionId() {
        return versionId;
    }


    /**
     * Sets the optional version ID of the newly uploaded object.
     *
     * @param versionId
     *            The optional version ID of the newly uploaded object.
     *
     * @see PutObjectResult#getVersionId()
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Gets the ETag value for the newly created object.
     *
     * @return The ETag value for the new object.
     *
     * @see PutObjectResult#setETag(String)
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the ETag value for the new object that was created from the
     * associated <code>putObject</code> request.
     *
     * @param eTag
     *            The ETag value for the new object.
     *
     * @see PutObjectResult#getETag()
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Returns the server-side encryption algorithm for the newly created
     * object, or null if none was used.
     */
    public String getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * Sets the server-side encryption algorithm for the newly created object.
     *
     * @param serverSideEncryption
     *            The server-side encryption algorithm for the new object.
     */
    public void setServerSideEncryption(String serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }

    /**
     * Returns the expiration time for this object, or null if it doesn't expire.
     */
    public Date getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets the expiration time for the object.
     *
     * @param expirationTime
     *            The expiration time for the object.
     */
    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Returns the {@link BucketLifecycleConfiguration} rule ID for this
     * object's expiration, or null if it doesn't expire.
     */
    public String getExpirationTimeRuleId() {
        return expirationTimeRuleId;
    }

    /**
     * Sets the {@link BucketLifecycleConfiguration} rule ID for this object's
     * expiration
     *
     * @param expirationTimeRuleId
     *            The rule ID for this object's expiration
     */
    public void setExpirationTimeRuleId(String expirationTimeRuleId) {
        this.expirationTimeRuleId = expirationTimeRuleId;
    }

    /**
     * Sets the content MD5.
     *
     * @param contentMd5
     *            The content MD5
     */
    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    /**
     * Returns the content MD5.
     */
    public String getContentMd5() {
        return contentMd5;
    }
    
    public String getServiceSideKey() {
    	return serviceSideKey;
    }
    
    public void setServiceSideKey(String serviceSideKey) {
    	this.serviceSideKey = serviceSideKey;
    }
}