/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Portions copyright 2006-2009 James Murty. Please see LICENSE.txt
 * for applicable license terms and NOTICE.txt for applicable notices.
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

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import com.sina.util.DateUtils;

/**
 * <p>
 * Represents an Amazon S3 bucket.
 * </p>
 * <p>
 * Every object stored in Amazon S3 is contained within a bucket. Buckets
 * partition the namespace of objects stored in Amazon S3 at the top level.
 * Within a bucket, any name can be used for objects. However, bucket names
 * must be unique across all of Amazon S3.
 * </p>
 * <p>
 * Bucket ownership is similar to the ownership of Internet domain names. 
 * Within Amazon S3, only a single user owns each bucket. 
 * Once a uniquely named bucket is created in Amazon S3, 
 * organize and name the objects within the bucket in any way.
 * Ownership of the bucket is retained as long as the owner has an Amazon S3 account.
 * </p>
 * <p>
 * To conform with DNS requirements, the following constraints apply:
 *  <ul>
 *      <li>Bucket names should not contain underscores</li>
 *      <li>Bucket names should be between 3 and 63 characters long</li>
 *      <li>Bucket names should not end with a dash</li>
 *      <li>Bucket names cannot contain adjacent periods</li>
 *      <li>Bucket names cannot contain dashes next to periods (e.g.,
 *      "my-.bucket.com" and "my.-bucket" are invalid)</li>
 *      <li>Bucket names cannot contain uppercase characters</li>
 *  </ul>
 * </p>
 * <p>
 * There are no limits to the number of objects that can be stored in a bucket.
 * Performance does not vary based on the number of buckets used. Store
 * all objects within a single bucket or organize them across several buckets.
 * </p>
 */
public class Bucket {

    /** The name of this S3 bucket */
    private String name = null;

    /** The details on the owner of this bucket */
    private Owner owner = null;

    /** The date this bucket was created */
    private Date creationDate = null;
    
    /** bucket空间用量 */
    private long consumedBytes = 0;

    /**
     * Constructs a bucket without any name specified.
     * 
     * @see Bucket#Bucket(String)
     */
    public Bucket() {}

    /**
     * Creates a bucket with a name. 
     * All buckets in Amazon S3 share a single namespace;
     * ensure the bucket is given a unique name.
     *
     * @param name
     *            The name for the bucket.
     *            
     * @see Bucket#Bucket()        
     */
    public Bucket(String name) {
        this.name = name;
    }
    
    /**
     * Create a bucket from jsonMap
     * @param jsonMap
     */
    public Bucket(Map<String,Object> bucketJsonMap,Owner owner){
    	
    	this.name = (String) bucketJsonMap.get("Name");
    	this.owner = owner;
		//创建时间
		String creationDateStr = (String) bucketJsonMap.get("CreationDate");
		if(creationDateStr!=null && !"".equals(creationDateStr)){
			DateUtils du = new DateUtils();
			try {
				this.setCreationDate(du.parseRfc822Date(creationDateStr));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		//用量
		this.setConsumedBytes((Long) bucketJsonMap.get("ConsumedBytes"));
    	
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "S3Bucket [name=" + getName()
                + ", creationDate=" + getCreationDate()
                + ", owner=" + getOwner() 
                + ", consumedBytes=" + getConsumedBytes()
                + "]";
    }

    /**
     * Gets the bucket's owner.  Returns <code>null</code>
     * if the bucket's owner is unknown.
     * 
     * @return 
     *  The bucket's owner, or <code>null</code> if it is unknown.
     *  
     *  @see Bucket#setOwner(Owner)
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * For internal use only.
     * Sets the bucket's owner in Amazon S3. This should only be used internally by
     * the AWS Java client methods that retrieve information directly from Amazon S3.
     *
     * @param owner
     *          The bucket's owner.
     * 
     * @see @link Bucket#getOwner()
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Gets the bucket's creation date. Returns <code>null</code>
     * if the creation date is not known.
     *
     * @return The bucket's creation date, or <code>null</code> 
     * if not known.
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * For internal use only.
     * Sets the bucket's creation date in S3. This should only be used
     * internally by AWS Java client methods that retrieve information directly
     * from Amazon S3.
     *
     * @param creationDate
     *          The bucket's creation date.
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Gets the name of the bucket.
     *
     * @return The name of this bucket.
     * 
     * @see Bucket#setName(String)
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the bucket. 
     * All buckets in Amazon S3 share a single namespace;
     * ensure the bucket is given a unique name.
     *
     * @param name
     *            The name for the bucket.
     */
    public void setName(String name) {
        this.name = name;
    }

	public long getConsumedBytes() {
		return consumedBytes;
	}

	public void setConsumedBytes(long consumedBytes) {
		this.consumedBytes = consumedBytes;
	}

}
