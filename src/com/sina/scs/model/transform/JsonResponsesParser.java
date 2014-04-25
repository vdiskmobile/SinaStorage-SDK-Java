package com.sina.scs.model.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sina.SCSClientException;
import com.sina.scs.Constants;
import com.sina.scs.ObjectExpirationResult;
import com.sina.scs.ServerSideEncryptionResult;
import com.sina.scs.ServiceUtils;
import com.sina.scs.model.AccessControlList;
import com.sina.scs.model.AmazonS3Exception;
import com.sina.scs.model.Bucket;
import com.sina.scs.model.CompleteMultipartUploadResult;
import com.sina.scs.model.InitiateMultipartUploadResult;
import com.sina.scs.model.ObjectListing;
import com.sina.scs.model.Owner;
import com.sina.scs.model.PartListing;

public class JsonResponsesParser {
    private static final Log log = LogFactory.getLog(JsonResponsesParser.class);
   
    /**
     * Parses an json document from an input stream using a document handler.
     *
     * @param handler
     *            the handler for the json document
     * @param inputStream
     *            an input stream containing the json document to parse
     *
     * @throws SCSClientException
     *             any parsing, IO or other exceptions are wrapped in an
     *             S3ServiceException.
     */
    protected <T> T parseJsonInputStream(TypeReference<T> tr, InputStream inputStream)
            throws SCSClientException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Parsing json response document with type: " + tr);
            }

            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                Constants.DEFAULT_ENCODING));
            
            ObjectMapper mapper = new ObjectMapper();
            T modelObject = mapper.readValue(breader,tr);
            return modelObject;
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after json parse failure", e);
                }
            }
            throw new SCSClientException("Failed to parse json document with handler "
                + tr, t);
        }
    }
    
    /**
     * Parses a ListBucket response json document from an input stream.
     *
     *	{
	 *	    "Delimiter": null,
	 *	    "Prefix": null,
	 *	    "CommonPrefixes": [],
	 *	    "Marker": null,
	 *	    "ContentsQuantity": 10,
	 *	    "CommonPrefixesQuantity": 0,
	 *	    "NextMarker": null,
	 *	    "IsTruncated": false,
	 *	    "Contents": [
	 *	        {
	 *	            "SHA1": "4a09518d3c402d0a444e2f6c964a1b5xxxxxx",
	 *	            "Name": "/aaa/file.txt",
	 *	            "Expiration-Time": null,
	 *	            "Last-Modified": "Mon, 31 Mar 2014 08:53:41 UTC",
	 *	            "Owner": "SINA000000100xxxxxx",
	 *	            "MD5": "49c60d1ef444d46939xxxxxxxxxx",
	 *	            "Content-Type": "text/plain",
	 *	            "Size": 48
	 *	        },
	 *	        ...
	 *	    ]
	 *	}
     *
     * @param inputStream
     *            json data input stream.
     * @return the json handler object populated with data parsed from the json
     *         stream.
     * @throws SCSClientException
     */
    public ObjectListing parseListBucketObjectsResponse(InputStream inputStream)
            throws SCSClientException {
    	try {
            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                Constants.DEFAULT_ENCODING));
            
//            StringBuffer buffer = new StringBuffer();
//            String line = "";
//            while ((line = breader.readLine()) != null){
//              buffer.append(line);
//            }
//            log.debug(buffer.toString());

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            @SuppressWarnings("unchecked")
			Map<String,Object> modelObject = mapper.readValue(breader,Map.class);
            
            return new ObjectListing(modelObject);
            
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after json parse failure", e);
                }
            }
            throw new SCSClientException("Failed to parse json document with handler ", t);
        }
    	
    }

    /**
     * Parses a ListAllMyBuckets response json document from an input stream.
     *
     *	{
     *       "Owner": {
     *           "DisplayName": "",
     *           "ID": "SINA0000001001NHT3M7"
     *       },
     *       "Buckets": [
     *           {
     *               "ConsumedBytes": 22536776,
     *               "CreationDate": "Fri, 28 Mar 2014 09:07:45 UTC",
     *               "Name": "test11"
     *           },
     *           {
     *               "ConsumedBytes": 0,
     *               "CreationDate": "Tue, 01 Apr 2014 03:28:32 UTC",
     *               "Name": "asdasdasdasd"
     *           }
     *       ]
     *   }
     *
     *
     * @param inputStream
     *            json data input stream.
     * @return the json handler object populated with data parsed from the json
     *         stream.
     * @throws SCSClientException
     */
    @SuppressWarnings("unchecked")
    public List<Bucket> parseListMyBucketsResponse(InputStream inputStream)
            throws SCSClientException {
    	try {
            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                Constants.DEFAULT_ENCODING));
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            List<Bucket> resultList = new ArrayList<Bucket>();
            
			Map<String,Object> modelObject = mapper.readValue(breader,Map.class);
			if (modelObject != null && modelObject.get("Buckets") != null){
				HashMap<String,String> ownerMap = (HashMap<String,String>) modelObject.get("Owner");
				Owner owner = new Owner((String)ownerMap.get("ID"), (String)ownerMap.get("DisplayName"));
				
				List<HashMap<String,Object>> bucketList = (List<HashMap<String,Object>>) modelObject.get("Buckets");
	        	for(HashMap<String,Object> bucketMap : bucketList)
	        		resultList.add(new Bucket(bucketMap,owner));
	        }
            
            return resultList;
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after json parse failure", e);
                }
            }
            throw new SCSClientException("Failed to parse json document with handler ", t);
        }
    	
    }

    /**
     * Parses an AccessControlListHandler response json document from an input
     * stream.
     *
     *   {
	 *       "Owner": "SINA0000001001NHT3M7",
	 *       "ACL": {
	 *           "SINA0000001001NHT3M7": [
	 *               "read",
	 *               "write",
	 *               "read_acp",
	 *               "write_acp"
	 *           ]
	 *       }
	 *   }
     *
     * @param inputStream
     *            json data input stream.
     * @return the json handler object populated with data parsed from the json
     *         stream.
     *
     * @throws SCSClientException
     */
    public AccessControlList parseAccessControlListResponse(InputStream inputStream)
        throws SCSClientException{
    	try {
            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                Constants.DEFAULT_ENCODING));
            
//            StringBuffer buffer = new StringBuffer();
//            String line = "";
//            while ((line = breader.readLine()) != null){
//              buffer.append(line);
//            }
//            
//            log.debug(buffer.toString());
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
			@SuppressWarnings("unchecked")
			Map<String,Object> jsonObject = mapper.readValue(breader,Map.class);
			if(jsonObject!=null)
				return new AccessControlList(jsonObject);
            
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after json parse failure", e);
                }
            }
            throw new SCSClientException("Failed to parse json document with handler ", t);
        }
    	
    	return null;
    	
    }

//    public DeleteObjectsHandler parseDeletedObjectsResult(InputStream inputStream) {
//        DeleteObjectsHandler handler = new DeleteObjectsHandler();
//        parseXmlInputStream(handler, inputStream);
//        return handler;
//    }

//    public CopyObjectResultHandler parseCopyObjectResponse(InputStream inputStream)
//        throws SCSClientException
//    {
////        CopyObjectResultHandler handler = new CopyObjectResultHandler();
////        parseXmlInputStream(handler, inputStream);
////        return handler;
//    	return return parseJsonInputStream(new TypeReference<AccessControlList>() {}, inputStream);
//    }

    public CompleteMultipartUploadHandler parseCompleteMultipartUploadResponse(InputStream inputStream)
        throws SCSClientException
    {
        CompleteMultipartUploadHandler handler = new CompleteMultipartUploadHandler();
//        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    /**
     * 
     *  {
	 *	    "Bucket": "<Your-Bucket-Name>",
	 *	    "Key": "<ObjectName>",
	 *	    "UploadId": "7517c1c49a3b4b86a5f08858290c5cf6"
	 *	}
     * @param inputStream
     * @return
     * @throws SCSClientException
     */
    public InitiateMultipartUploadResult parseInitiateMultipartUploadResponse(InputStream inputStream)
        throws SCSClientException
    {
//        InitiateMultipartUploadHandler handler = new InitiateMultipartUploadHandler();
//        parseXmlInputStream(handler, inputStream);
//        return handler;
    	
    	try {
            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                Constants.DEFAULT_ENCODING));
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
			@SuppressWarnings("unchecked")
			Map<String,String> jsonObject = mapper.readValue(breader,Map.class);
			if(jsonObject!=null)
				return new InitiateMultipartUploadResult(jsonObject);
            
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after json parse failure", e);
                }
            }
            throw new SCSClientException("Failed to parse json document with handler ", t);
        }
    	
    	return null;
    }

//    public MultipartUploadListing parseListMultipartUploadsResponse(InputStream inputStream)
//        throws SCSClientException
//    {
//    	try {
//            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
//                Constants.DEFAULT_ENCODING));
//            
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
//            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//            
//			@SuppressWarnings("unchecked")
//			Map<String,Object> jsonObject = mapper.readValue(breader,Map.class);
//			if(jsonObject!=null)
//				return new MultipartUploadListing(jsonObject);
//            
//        } catch (Throwable t) {
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                if (log.isErrorEnabled()) {
//                    log.error("Unable to close response InputStream up after json parse failure", e);
//                }
//            }
//            throw new SCSClientException("Failed to parse json document with handler ", t);
//        }
//    	
//    	return null;
//    }

    /**
     *   * {
	 *	    "Bucket": "<Your-Bucket-Name>",
	 *	
	 *	    "Key": "<ObjectName>",
	 *	
	 *	    "Initiator": {
	 *		
	 *	        "ID": "<ID>",
	 *	        "DisplayName": "<DisplayName>"
	 *	    },
	 *	
	 *	    "PartNumberMarker": null,
	 *	
	 *	    "NextPartNumberMarker": null,
	 *	
	 *	    "MaxParts": null,
	 *	
	 *	    "IsTruncated": false,
	 *	
	 *	    "Part": [
	 *	
	 *	        {
	 *	            "PartNumber": 1,
	 *	            "Last-Modified": "Wed, 20 Jun 2012 14:57:10 UTC",
	 *	            "ETag": "050fdc0e690bfae7b29392f152bcf301",
	 *	            "Size": 1024
	 *	        },
	 *		
	 *	        {
	 *	            "PartNumber": 2,
	 *	            "Last-Modified": "Wed, 20 Jun 2012 14:57:10 UTC",
	 *	            "ETag": "050fdc0e690bfae7b29392f152bcf302",
	 *	            "Size": 1024
	 *	        },
	 *		
	 *	        {
	 *	            "PartNumber": 3,
	 *	            "Last-Modified": "Wed, 20 Jun 2012 14:57:10 UTC",
	 *	            "ETag": "050fdc0e690bfae7b29392f152bcf303",
	 *	            "Size": 1024
	 *	        },
	 *		
	 *	        ...
	 *	    ]
	 *		
	 *	}
	 *
     * @param inputStream
     * @return
     * @throws SCSClientException
     */
    public PartListing parseListPartsResponse(InputStream inputStream)
        throws SCSClientException
    {
//        ListPartsHandler handler = new ListPartsHandler();
//        parseXmlInputStream(handler, inputStream);
//        return handler;
    	try {
            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                Constants.DEFAULT_ENCODING));
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
			@SuppressWarnings("unchecked")
			Map<String,Object> jsonObject = mapper.readValue(breader,Map.class);
			if(jsonObject!=null)
				return new PartListing(jsonObject);
            
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after json parse failure", e);
                }
            }
            throw new SCSClientException("Failed to parse json document with handler ", t);
        }
    	
    	return null;
    }
    
//    // ////////////
//    // Handlers //
//    // ////////////
//
//    /**
//     * Handler for ListBucket response json documents. The document is parsed
//     * into {@link S3Object}s available via the {@link #getObjects()} method.
//     */
//    public class ListBucketHandler {
//        private S3ObjectSummary currentObject = null;
//        private Owner currentOwner = null;
//        private StringBuilder currText = null;
//        private boolean insideCommonPrefixes = false;
//
//        private ObjectListing objectListing = new ObjectListing();
//        private List<String> commonPrefixes = new ArrayList<String>();
//
//        // Listing properties.
//        private String bucketName = null;
//        private String requestPrefix = null;
//        private String requestMarker = null;
//        private int requestMaxKeys = 0;
//        private String requestDelimiter = null;
//        private boolean listingTruncated = false;
//        private String lastKey = null;
//        private String nextMarker = null;
//
//        public ListBucketHandler() {
//            super();
//            this.currText = new StringBuilder();
//        }
//
//        public ObjectListing getObjectListing() {
//            objectListing.setBucketName(bucketName);
//            objectListing.setCommonPrefixes(commonPrefixes);
//            objectListing.setDelimiter(requestDelimiter);
//            objectListing.setMarker(requestMarker);
//            objectListing.setMaxKeys(requestMaxKeys);
//            objectListing.setPrefix(requestPrefix);
//            objectListing.setTruncated(listingTruncated);
//
//            /*
//             * S3 only includes the NextMarker json element if the request
//             * specified a delimiter, but for consistency we'd like to always
//             * give easy access to the next marker if we're returning a list of
//             * results that's truncated.
//             */
//            if (nextMarker != null) {
//                objectListing.setNextMarker(nextMarker);
//            } else if (listingTruncated) {
//                String nextMarker = null;
//                if (objectListing.getObjectSummaries().isEmpty() == false) {
//                    nextMarker = objectListing.getObjectSummaries().get(objectListing.getObjectSummaries().size() - 1).getKey();
//                } else if (objectListing.getCommonPrefixes().isEmpty() == false) {
//                    nextMarker = objectListing.getCommonPrefixes().get(objectListing.getCommonPrefixes().size() - 1);
//                } else {
//                    log.error("S3 response indicates truncated results, but contains no object summaries or common prefixes.");
//                }
//
//                objectListing.setNextMarker(nextMarker);
//            }
//
//            return objectListing;
//        }
//
//        /**
//         * If the listing is truncated this method will return the marker that
//         * should be used in subsequent bucket list calls to complete the
//         * listing.
//         *
//         * @return null if the listing is not truncated, otherwise the next
//         *         marker if it's available or the last object key seen if the
//         *         next marker isn't available.
//         */
//        public String getMarkerForNextListing() {
//            if (listingTruncated) {
//                if (nextMarker != null) {
//                    return nextMarker;
//                } else if (lastKey != null) {
//                    return lastKey;
//                } else {
//                    if (log.isWarnEnabled()) {
//                        log.warn("Unable to find Next Marker or Last Key for truncated listing");
//                    }
//                    return null;
//                }
//            } else {
//                return null;
//            }
//        }
//
//        /**
//         * @return true if the listing document was truncated, and therefore
//         *         only contained a subset of the available S3 objects.
//         */
//        public boolean isListingTruncated() {
//            return listingTruncated;
//        }
//
//        /**
//         * @return the S3 objects contained in the listing.
//         */
//        public List<S3ObjectSummary> getObjects() {
//            return objectListing.getObjectSummaries();
//        }
//
//        public String[] getCommonPrefixes() {
//            return commonPrefixes.toArray(new String[commonPrefixes.size()]);
//        }
//
//        public String getRequestPrefix() {
//            return requestPrefix;
//        }
//
//        public String getRequestMarker() {
//            return requestMarker;
//        }
//
//        public String getNextMarker() {
//            return nextMarker;
//        }
//
//        public long getRequestMaxKeys() {
//            return requestMaxKeys;
//        }
//
//    }
//
//    /**
//     * Handler for ListAllMyBuckets response json documents. The document is
//     * parsed into {@link Bucket}s available via the {@link #getBuckets()}
//     * method.
//     */
//    public class ListAllMyBucketsHandler extends DefaultHandler {
//        private Owner bucketsOwner = null;
//        private Bucket currentBucket = null;
//        private StringBuilder currText = null;
//
//        private List<Bucket> buckets = null;
//
//        public ListAllMyBucketsHandler() {
//            super();
//            buckets = new ArrayList<Bucket>();
//            this.currText = new StringBuilder();
//        }
//
//        /**
//         * @return the buckets listed in the document.
//         */
//        public List<Bucket> getBuckets() {
//            return buckets;
//        }
//
//        /**
//         * @return the owner of the buckets.
//         */
//        public Owner getOwner() {
//            return bucketsOwner;
//        }
//
//        @Override
//        public void startDocument() {
//        }
//
//        @Override
//        public void endDocument() {
//        }
//
//        @Override
//        public void startElement(String uri, String name, String qName, Attributes attrs) {
//            if (name.equals("Bucket")) {
//                currentBucket = new Bucket();
//            } else if (name.equals("Owner")) {
//                bucketsOwner = new Owner();
//            }
//        }
//
//        @Override
//        public void endElement(String uri, String name, String qName) {
//            String elementText = this.currText.toString();
//            // Listing details.
//            if (name.equals("ID")) {
//                bucketsOwner.setId(elementText);
//            } else if (name.equals("DisplayName")) {
//                bucketsOwner.setDisplayName(elementText);
//            }
//            // Bucket item details.
//            else if (name.equals("Bucket")) {
//                currentBucket.setOwner(bucketsOwner);
//                buckets.add(currentBucket);
//            } else if (name.equals("Name")) {
//                currentBucket.setName(elementText);
//            } else if (name.equals("CreationDate")) {
//                elementText += ".000Z";
//                try {
//                    currentBucket.setCreationDate(ServiceUtils.parseIso8601Date(elementText));
//                } catch (ParseException e) {
//                    throw new RuntimeException(
//                        "Non-ISO8601 date for CreationDate in list buckets output: "
//                        + elementText, e);
//                }
//            }
//            this.currText = new StringBuilder();
//        }
//
//        @Override
//        public void characters(char ch[], int start, int length) {
//            this.currText.append(ch, start, length);
//        }
//    }
//
//    /**
//     * Handler for AccessControlList response json documents. The document is
//     * parsed into an {@link AccessControlList} object available via the
//     * {@link #getAccessControlList()} method.
//     */
//    public class AccessControlListHandler extends DefaultHandler {
//        private AccessControlList accessControlList = null;
//
//        private Owner owner = null;
//        private Grantee currentGrantee = null;
//        private Permission currentPermission = null;
//        private StringBuilder currText = null;
//
//        private boolean insideACL = false;
//
//        public AccessControlListHandler() {
//            super();
//            this.currText = new StringBuilder();
//        }
//
//        /**
//         * @return an object representing the ACL document.
//         */
//        public AccessControlList getAccessControlList() {
//            return accessControlList;
//        }
//
//        @Override
//        public void startDocument() {
//        }
//
//        @Override
//        public void endDocument() {
//        }
//
//        @Override
//        public void startElement(String uri, String name, String qName, Attributes attrs) {
//            if (name.equals("Owner")) {
//                owner = new Owner();
//            } else if (name.equals("AccessControlList")) {
//                accessControlList = new AccessControlList();
//                accessControlList.setOwner(owner);
//                insideACL = true;
//            } else if (name.equals("Grantee")) {
//                String type = XmlResponsesSaxParser.findAttributeValue( "xsi:type", attrs );
//                if ("AmazonCustomerByEmail".equals(type)) {
//                    currentGrantee = new EmailAddressGrantee(null);
//                } else if ("CanonicalUser".equals(type)) {
//                    currentGrantee = new CanonicalGrantee(null);
//                } else if ("Group".equals(type)) {
//                    /*
//                     * Nothing to do for GroupGrantees here since we
//                     * can't construct an empty enum value early.
//                     */
//                }
//            }
//        }
//
//        @Override
//        public void endElement(String uri, String name, String qName) {
//            String elementText = this.currText.toString();
//
//            // Owner details.
//            if (name.equals("ID") && !insideACL) {
//                owner.setId(elementText);
//            } else if (name.equals("DisplayName") && !insideACL) {
//                owner.setDisplayName(elementText);
//            }
//            // ACL details.
//            else if (name.equals("ID")) {
//                currentGrantee.setIdentifier(elementText);
//            } else if (name.equals("EmailAddress")) {
//                currentGrantee.setIdentifier(elementText);
//            } else if (name.equals("URI")) {
//                /*
//                 * Only GroupGrantees contain an URI element in them, and we
//                 * can't construct currentGrantee during startElement for a
//                 * GroupGrantee since it's an enum.
//                 */
//                currentGrantee = GroupGrantee.parseGroupGrantee(elementText);
//            } else if (name.equals("DisplayName")) {
//                ((CanonicalGrantee) currentGrantee).setDisplayName(elementText);
//            } else if (name.equals("Permission")) {
//                currentPermission = Permission.parsePermission(elementText);
//            } else if (name.equals("Grant")) {
//                accessControlList.grantPermission(currentGrantee, currentPermission);
//                currentGrantee = null;
//                currentPermission = null;
//            } else if (name.equals("AccessControlList")) {
//                insideACL = false;
//            }
//            this.currText = new StringBuilder();
//        }
//
//        @Override
//        public void characters(char ch[], int start, int length) {
//            this.currText.append(ch, start, length);
//        }
//    }
//
//
//    public class CopyObjectResultHandler extends DefaultHandler implements ServerSideEncryptionResult, ObjectExpirationResult {
//
//        // Data items for successful copy
//        private String etag = null;
//        private Date lastModified = null;
//        private String versionId = null;
//        private String serverSideEncryption;
//        private Date expirationTime;
//        private String expirationTimeRuleId;
//
//        // Data items for failed copy
//        private String errorCode = null;
//        private String errorMessage = null;
//        private String errorRequestId = null;
//        private String errorHostId = null;
//        private boolean receivedErrorResponse = false;
//
//        private StringBuilder currText = null;
//
//        public CopyObjectResultHandler() {
//            super();
//            this.currText = new StringBuilder();
//        }
//
//        public Date getLastModified() {
//            return lastModified;
//        }
//
//        public String getVersionId() {
//            return versionId;
//        }
//
//        public void setVersionId(String versionId) {
//            this.versionId = versionId;
//        }
//
//        @Override
//        public String getServerSideEncryption() {
//            return serverSideEncryption;
//        }
//
//        @Override
//        public void setServerSideEncryption(String serverSideEncryption) {
//            this.serverSideEncryption = serverSideEncryption;
//        }
//
//        @Override
//        public Date getExpirationTime() {
//            return expirationTime;
//        }
//
//        @Override
//        public void setExpirationTime(Date expirationTime) {
//            this.expirationTime = expirationTime;
//        }
//
//        @Override
//        public String getExpirationTimeRuleId() {
//            return expirationTimeRuleId;
//        }
//
//        @Override
//        public void setExpirationTimeRuleId(String expirationTimeRuleId) {
//            this.expirationTimeRuleId = expirationTimeRuleId;
//        }
//
//        public String getETag() {
//            return etag;
//        }
//
//        public String getErrorCode() {
//            return errorCode;
//        }
//
//        public String getErrorHostId() {
//            return errorHostId;
//        }
//
//        public String getErrorMessage() {
//            return errorMessage;
//        }
//
//        public String getErrorRequestId() {
//            return errorRequestId;
//        }
//
//        public boolean isErrorResponse() {
//            return receivedErrorResponse;
//        }
//
//
//        @Override
//        public void startDocument() {
//        }
//
//        @Override
//        public void endDocument() {
//        }
//
//        @Override
//        public void startElement(String uri, String name, String qName, Attributes attrs) {
//            if (name.equals("CopyObjectResult")) {
//                receivedErrorResponse = false;
//            } else if (name.equals("Error")) {
//                receivedErrorResponse = true;
//            }
//        }
//
//        @Override
//        public void endElement(String uri, String name, String qName) {
//            String elementText = this.currText.toString();
//
//            if (name.equals("LastModified")) {
//                try {
//                    lastModified = ServiceUtils.parseIso8601Date(elementText);
//                } catch (ParseException e) {
//                    throw new RuntimeException(
//                        "Non-ISO8601 date for LastModified in copy object output: "
//                        + elementText, e);
//                }
//            } else if (name.equals("ETag")) {
//                etag = ServiceUtils.removeQuotes(elementText);
//            } else if (name.equals("Code")) {
//                errorCode = elementText;
//            } else if (name.equals("Message")) {
//                errorMessage = elementText;
//            } else if (name.equals("RequestId")) {
//                errorRequestId = elementText;
//            } else if (name.equals("HostId")) {
//                errorHostId = elementText;
//            }
//
//            this.currText = new StringBuilder();
//        }
//
//        @Override
//        public void characters(char ch[], int start, int length) {
//            this.currText.append(ch, start, length);
//        }
//    }
//
    /*
     * <?xml version="1.0" encoding="UTF-8"?>
     * <CompleteMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
     *     <Location>http://Example-Bucket.s3.amazonaws.com/Example-Object</Location>
     *     <Bucket>Example-Bucket</Bucket>
     *     <Key>Example-Object</Key>
     *     <ETag>"3858f62230ac3c915f300c664312c11f-9"</ETag>
     * </CompleteMultipartUploadResult>
     *
     * Or if an error occurred while completing:
     *
     * <?xml version="1.0" encoding="UTF-8"?>
     * <Error>
     *     <Code>InternalError</Code>
     *     <Message>We encountered an internal error. Please try again.</Message>
     *     <RequestId>656c76696e6727732072657175657374</RequestId>
     *     <HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>
     * </Error>
     */
    public class CompleteMultipartUploadHandler extends DefaultHandler implements ServerSideEncryptionResult,
            ObjectExpirationResult {

        private StringBuilder text;

        // Successful completion
        private CompleteMultipartUploadResult result;

        @Override
        public String getServerSideEncryption() {
            if ( result != null )
                return result.getServerSideEncryption();
            else
                return null;
        }

        @Override
        public void setServerSideEncryption(String serverSideEncryption) {
            if ( result != null )
                result.setServerSideEncryption(serverSideEncryption);
        }

        /**
         * @see com.amazonaws.services.s3.model.CompleteMultipartUploadResult#getExpirationTime()
         */
        @Override
        public Date getExpirationTime() {
            if ( result != null )
                return result.getExpirationTime();
            return null;
        }

        /**
         * @see com.amazonaws.services.s3.model.CompleteMultipartUploadResult#setExpirationTime(java.util.Date)
         */
        @Override
        public void setExpirationTime(Date expirationTime) {
            if ( result != null )
                result.setExpirationTime(expirationTime);
        }

        /**
         * @see com.amazonaws.services.s3.model.CompleteMultipartUploadResult#getExpirationTimeRuleId()
         */
        @Override
        public String getExpirationTimeRuleId() {
            if ( result != null )
                return result.getExpirationTimeRuleId();
            return null;
        }

        /**
         * @see com.amazonaws.services.s3.model.CompleteMultipartUploadResult#setExpirationTimeRuleId(java.lang.String)
         */
        @Override
        public void setExpirationTimeRuleId(String expirationTimeRuleId) {
            if ( result != null )
                result.setExpirationTimeRuleId(expirationTimeRuleId);
        }

        // Error during completion
        private AmazonS3Exception ase;
        private String hostId;
        private String requestId;
        private String errorCode;

        public CompleteMultipartUploadResult getCompleteMultipartUploadResult() {
            return result;
        }

        public AmazonS3Exception getAmazonS3Exception() {
            return ase;
        }

        @Override
        public void startDocument() {
            text = new StringBuilder();
        }

        @Override
        public void startElement(String uri, String name, String qName, Attributes attrs) {
            // Success response json elements
            if (name.equals("CompleteMultipartUploadResult")) {
                result = new CompleteMultipartUploadResult();
            } else if (name.equals("Location")) {
            } else if (name.equals("Bucket")) {
            } else if (name.equals("Key")) {
            } else if (name.equals("ETag")) {
            }

            // Error response json elements
            if (name.equals("Error")) {
            } else if (name.equals("Code")) {
            } else if (name.equals("Message")) {
            } else if (name.equals("RequestId")) {
            } else if (name.equals("HostId")) {
            }
            text.setLength(0);
        }

        @Override
        public void endElement(String uri, String name, String qName) throws SAXException {
            if (result != null) {
                // Success response json elements
                if (name.equals("CompleteMultipartUploadResult")) {
                } else if (name.equals("Location")) {
                    result.setLocation(text.toString());
                } else if (name.equals("Bucket")) {
                    result.setBucketName(text.toString());
                } else if (name.equals("Key")) {
                    result.setKey(text.toString());
                } else if (name.equals("ETag")) {
                    result.setETag(ServiceUtils.removeQuotes(text.toString()));
                }
            } else {
                // Error response json elements
                if (name.equals("Error")) {
                    ase.setErrorCode(errorCode);
                    ase.setRequestId(requestId);
                    ase.setExtendedRequestId(hostId);
                } else if (name.equals("Code")) {
                    errorCode = text.toString();
                } else if (name.equals("Message")) {
                    ase = new AmazonS3Exception(text.toString());
                } else if (name.equals("RequestId")) {
                    requestId = text.toString();
                } else if (name.equals("HostId")) {
                    hostId = text.toString();
                }
            }
        }

        @Override
        public void characters(char ch[], int start, int length) {
            this.text.append(ch, start, length);
        }
    }
//
//    /*
//     * <?xml version="1.0" encoding="UTF-8"?>
//     * <InitiateMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
//     *     <Bucket>example-bucket</Bucket>
//     *     <Key>example-object</Key>
//     *     <UploadId>VXBsb2FkIElEIGZvciA2aWWpbmcncyBteS1tb3ZpZS5tMnRzIHVwbG9hZA</UploadId>
//     * </InitiateMultipartUploadResult>
//     */
//    public class InitiateMultipartUploadHandler extends DefaultHandler {
//        private StringBuilder text;
//
//        private InitiateMultipartUploadResult result;
//
//        public InitiateMultipartUploadResult getInitiateMultipartUploadResult() {
//            return result;
//        }
//
//        @Override
//        public void startDocument() {
//            text = new StringBuilder();
//        }
//
//        @Override
//        public void startElement(String uri, String name, String qName, Attributes attrs) {
//            if (name.equals("InitiateMultipartUploadResult")) {
//                result = new InitiateMultipartUploadResult();
//            } else if (name.equals("Bucket")) {
//            } else if (name.equals("Key")) {
//            } else if (name.equals("UploadId")) {
//            }
//            text.setLength(0);
//        }
//
//        @Override
//        public void endElement(String uri, String name, String qName) throws SAXException {
//            if (name.equals("InitiateMultipartUploadResult")) {
//            } else if (name.equals("Bucket")) {
//                result.setBucketName(text.toString());
//            } else if (name.equals("Key")) {
//                result.setKey(text.toString());
//            } else if (name.equals("UploadId")) {
//                result.setUploadId(text.toString());
//            }
//        }
//
//        @Override
//        public void characters(char ch[], int start, int length) {
//            this.text.append(ch, start, length);
//        }
//    }
//
//    /*
//     * HTTP/1.1 200 OK
//     * x-amz-id-2: Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==
//     * x-amz-request-id: 656c76696e6727732072657175657374
//     * Date: Tue, 16 Feb 2010 20:34:56 GMT
//     * Content-Length: 1330
//     * Connection: keep-alive
//     * Server: AmazonS3
//     *
//     * <?xml version="1.0" encoding="UTF-8"?>
//     * <ListMultipartUploadsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
//     *     <Bucket>bucket</Bucket>
//     *     <KeyMarker></KeyMarker>
//     *     <Delimiter>/</Delimiter>
//     *     <Prefix/>
//     *     <UploadIdMarker></UploadIdMarker>
//     *     <NextKeyMarker>my-movie.m2ts</NextKeyMarker>
//     *     <NextUploadIdMarker>YW55IGlkZWEgd2h5IGVsdmluZydzIHVwbG9hZCBmYWlsZWQ</NextUploadIdMarker>
//     *     <MaxUploads>3</MaxUploads>
//     *     <IsTruncated>true</IsTruncated>
//     *     <Upload>
//     *         <Key>my-divisor</Key>
//     *         <UploadId>XMgbGlrZSBlbHZpbmcncyBub3QgaGF2aW5nIG11Y2ggbHVjaw</UploadId>
//     *         <Owner>
//     *             <ID>b1d16700c70b0b05597d7acd6a3f92be</ID>
//     *             <DisplayName>delving</DisplayName>
//     *         </Owner>
//     *         <StorageClass>STANDARD</StorageClass>
//     *         <Initiated>Tue, 26 Jan 2010 19:42:19 GMT</Initiated>
//     *     </Upload>
//     *     <Upload>
//     *         <Key>my-movie.m2ts</Key>
//     *         <UploadId>VXBsb2FkIElEIGZvciBlbHZpbmcncyBteS1tb3ZpZS5tMnRzIHVwbG9hZA</UploadId>
//     *         <Owner>
//     *             <ID>b1d16700c70b0b05597d7acd6a3f92be</ID>
//     *             <DisplayName>delving</DisplayName>
//     *         </Owner>
//     *         <StorageClass>STANDARD</StorageClass>
//     *         <Initiated>Tue, 16 Feb 2010 20:34:56 GMT</Initiated>
//     *     </Upload>
//     *     <Upload>
//     *         <Key>my-movie.m2ts</Key>
//     *         <UploadId>YW55IGlkZWEgd2h5IGVsdmluZydzIHVwbG9hZCBmYWlsZWQ</UploadId>
//     *         <Owner>
//     *             <ID>b1d16700c70b0b05597d7acd6a3f92be</ID>
//     *             <DisplayName>delving</DisplayName>
//     *         </Owner>
//     *         <StorageClass>STANDARD</StorageClass>
//     *         <Initiated>Wed, 27 Jan 2010 03:02:01 GMT</Initiated>
//     *     </Upload>
//     *    <CommonPrefixes>
//     *        <Prefix>photos/</Prefix>
//     *    </CommonPrefixes>
//     *    <CommonPrefixes>
//     *        <Prefix>videos/</Prefix>
//     *    </CommonPrefixes>
//     * </ListMultipartUploadsResult>
//     */
//    public class ListMultipartUploadsHandler extends DefaultHandler {
//        private StringBuilder text;
//
//        private MultipartUploadListing result;
//
//        private MultipartUpload currentMultipartUpload;
//        private Owner currentOwner;
//        private Owner currentInitiator;
//
//        boolean inCommonPrefixes = false;
//
//        public MultipartUploadListing getListMultipartUploadsResult() {
//            return result;
//        }
//
//        @Override
//        public void startDocument() {
//            text = new StringBuilder();
//        }
//
//        @Override
//        public void startElement(String uri, String name, String qName, Attributes attrs) {
//            if (name.equals("ListMultipartUploadsResult")) {
//                result = new MultipartUploadListing();
//            } else if (name.equals("Bucket")) {
//            } else if (name.equals("KeyMarker")) {
//            } else if (name.equals("Delimiter")) {
//            } else if (name.equals("UploadIdMarker")) {
//            } else if (name.equals("NextKeyMarker")) {
//            } else if (name.equals("NextUploadIdMarker")) {
//            } else if (name.equals("MaxUploads")) {
//            } else if (name.equals("IsTruncated")) {
//            } else if (name.equals("Upload")) {
//                currentMultipartUpload = new MultipartUpload();
//            } else if (name.equals("Key")) {
//            } else if (name.equals("UploadId")) {
//            } else if (name.equals("Owner")) {
//                currentOwner = new Owner();
//            } else if (name.equals("Initiator")) {
//                currentInitiator = new Owner();
//            } else if (name.equals("ID")) {
//            } else if (name.equals("DisplayName")) {
//            } else if (name.equals("StorageClass")) {
//            } else if (name.equals("Initiated")) {
//            } else if (name.equals("CommonPrefixes")) {
//                inCommonPrefixes = true;
//            }
//            text.setLength(0);
//        }
//
//        @Override
//        public void endElement(String uri, String name, String qName) throws SAXException {
//            if (name.equals("ListMultipartUploadsResult")) {
//            } else if (name.equals("Bucket")) {
//                result.setBucketName(text.toString());
//            } else if (name.equals("KeyMarker")) {
//                result.setKeyMarker(checkForEmptyString(text.toString()));
//            } else if (name.equals("Delimiter")) {
//                result.setDelimiter(checkForEmptyString(text.toString()));
//            } else if (name.equals("Prefix") && inCommonPrefixes == false) {
//                result.setPrefix(checkForEmptyString(text.toString()));
//            } else if (name.equals("Prefix") && inCommonPrefixes == true) {
//                result.getCommonPrefixes().add(text.toString());
//            } else if (name.equals("UploadIdMarker")) {
//                result.setUploadIdMarker(checkForEmptyString(text.toString()));
//            } else if (name.equals("NextKeyMarker")) {
//                result.setNextKeyMarker(checkForEmptyString(text.toString()));
//            } else if (name.equals("NextUploadIdMarker")) {
//                result.setNextUploadIdMarker(checkForEmptyString(text.toString()));
//            } else if (name.equals("MaxUploads")) {
//                result.setMaxUploads(Integer.parseInt(text.toString()));
//            } else if (name.equals("IsTruncated")) {
//                result.setTruncated(Boolean.parseBoolean(text.toString()));
//            } else if (name.equals("Upload")) {
//                result.getMultipartUploads().add(currentMultipartUpload);
//            } else if (name.equals("Key")) {
//                currentMultipartUpload.setKey(text.toString());
//            } else if (name.equals("UploadId")) {
//                currentMultipartUpload.setUploadId(text.toString());
//            } else if (name.equals("Owner")) {
//                currentMultipartUpload.setOwner(currentOwner);
//                currentOwner = null;
//            } else if (name.equals("Initiator")) {
//                currentMultipartUpload.setInitiator(currentInitiator);
//                currentInitiator = null;
//            } else if (name.equals("ID") && currentOwner != null) {
//                currentOwner.setId(checkForEmptyString(text.toString()));
//            } else if (name.equals("DisplayName") && currentOwner != null) {
//                currentOwner.setDisplayName(checkForEmptyString(text.toString()));
//            } else if (name.equals("ID") && currentInitiator != null) {
//                currentInitiator.setId(checkForEmptyString(text.toString()));
//            } else if (name.equals("DisplayName") && currentInitiator != null) {
//                currentInitiator.setDisplayName(checkForEmptyString(text.toString()));
//            } else if (name.equals("StorageClass")) {
//                currentMultipartUpload.setStorageClass(text.toString());
//            } else if (name.equals("Initiated")) {
//                try {
//                    currentMultipartUpload.setInitiated(ServiceUtils.parseIso8601Date(text.toString()));
//                } catch (ParseException e) {
//                    throw new SAXException(
//                            "Non-ISO8601 date for Initiated in initiate multipart upload result: "
//                            + text.toString(), e);
//                }
//            } else if (name.equals("CommonPrefixes")) {
//                inCommonPrefixes = false;
//            }
//        }
//
//        @Override
//        public void characters(char ch[], int start, int length) {
//            this.text.append(ch, start, length);
//        }
//    }
//
//    /*
//     * HTTP/1.1 200 OK
//     * x-amz-id-2: Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==
//     * x-amz-request-id: 656c76696e6727732072657175657374
//     * Date: Tue, 16 Feb 2010 20:34:56 GMT
//     * Content-Length: 985
//     * Connection: keep-alive
//     * Server: AmazonS3
//     *
//     * <?xml version="1.0" encoding="UTF-8"?>
//     * <ListPartsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
//     *     <Bucket>example-bucket</Bucket>
//     *     <Key>example-object</Key>
//     *     <UploadId>XXBsb2FkIElEIGZvciBlbHZpbmcncyVcdS1tb3ZpZS5tMnRzEEEwbG9hZA</UploadId>
//     *     <Owner>
//     *         <ID>x1x16700c70b0b05597d7ecd6a3f92be</ID>
//     *         <DisplayName>username</DisplayName>
//     *     </Owner>
//     *     <Initiator>
//     *         <ID>x1x16700c70b0b05597d7ecd6a3f92be</ID>
//     *         <DisplayName>username</DisplayName>
//     *     </Initiator>
//     *     <StorageClass>STANDARD</StorageClass>
//     *     <PartNumberMarker>1</PartNumberMarker>
//     *     <NextPartNumberMarker>3</NextPartNumberMarker>
//     *     <MaxParts>2</MaxParts>
//     *     <IsTruncated>true</IsTruncated>
//     *     <Part>
//     *         <PartNumber>2</PartNumber>
//     *         <LastModified>Wed, 27 Jan 2010 03:02:03 GMT</LastModified>
//     *         <ETag>"7778aef83f66abc1fa1e8477f296d394"</ETag>
//     *         <Size>10485760</Size>
//     *     </Part>
//     *     <Part>
//     *        <PartNumber>3</PartNumber>
//     *        <LastModified>Wed, 27 Jan 2010 03:02:02 GMT</LastModified>
//     *        <ETag>"aaaa18db4cc2f85cedef654fccc4a4x8"</ETag>
//     *        <Size>10485760</Size>
//     *     </Part>
//     * </ListPartsResult>
//     */
//    public class ListPartsHandler extends DefaultHandler {
//        private StringBuilder text;
//
//        private PartListing result;
//        private Owner currentOwner;
//        private Owner currentInitiator;
//        private PartSummary currentPart;
//
//        public PartListing getListPartsResult() {
//            return result;
//        }
//
//        @Override
//        public void startDocument() {
//            text = new StringBuilder();
//        }
//
//        @Override
//        public void startElement(String uri, String name, String qName, Attributes attrs) {
//            if (name.equals("ListPartsResult")) {
//                result = new PartListing();
//            } else if (name.equals("Bucket")) {
//            } else if (name.equals("Key")) {
//            } else if (name.equals("UploadId")) {
//            } else if (name.equals("Owner")) {
//                currentOwner = new Owner();
//            } else if (name.equals("Initiator")) {
//                currentInitiator = new Owner();
//            } else if (name.equals("ID")) {
//            } else if (name.equals("DisplayName")) {
//            } else if (name.equals("StorageClass")) {
//            } else if (name.equals("PartNumberMarker")) {
//            } else if (name.equals("NextPartNumberMarker")) {
//            } else if (name.equals("MaxParts")) {
//            } else if (name.equals("IsTruncated")) {
//            } else if (name.equals("Part")) {
//                currentPart = new PartSummary();
//            } else if (name.equals("PartNumber")) {
//            } else if (name.equals("LastModified")) {
//            } else if (name.equals("ETag")) {
//            } else if (name.equals("Size")) {
//            }
//            text.setLength(0);
//        }
//
//        private Integer parseInteger(String text) {
//            text = checkForEmptyString(text.toString());
//            if (text == null) return null;
//            return Integer.parseInt(text);
//        }
//
//        @Override
//        public void endElement(String uri, String name, String qName) throws SAXException {
//            if (name.equals("ListPartsResult")) {
//            } else if (name.equals("Bucket")) {
//                result.setBucketName(text.toString());
//            } else if (name.equals("Key")) {
//                result.setKey(text.toString());
//            } else if (name.equals("UploadId")) {
//                result.setUploadId(text.toString());
//            } else if (name.equals("Owner")) {
//                result.setOwner(currentOwner);
//                currentOwner = null;
//            } else if (name.equals("Initiator")) {
//                result.setInitiator(currentInitiator);
//                currentInitiator = null;
//            } else if (name.equals("ID") && currentOwner != null) {
//                currentOwner.setId(checkForEmptyString(text.toString()));
//            } else if (name.equals("DisplayName") && currentOwner != null) {
//                currentOwner.setDisplayName(checkForEmptyString(text.toString()));
//            } else if (name.equals("ID") && currentInitiator != null) {
//                currentInitiator.setId(checkForEmptyString(text.toString()));
//            } else if (name.equals("DisplayName") && currentInitiator != null) {
//                currentInitiator.setDisplayName(checkForEmptyString(text.toString()));
//            } else if (name.equals("StorageClass")) {
//                result.setStorageClass(text.toString());
//            } else if (name.equals("PartNumberMarker")) {
//                result.setPartNumberMarker(parseInteger(text.toString()));
//            } else if (name.equals("NextPartNumberMarker")) {
//                result.setNextPartNumberMarker(parseInteger(text.toString()));
//            } else if (name.equals("MaxParts")) {
//                result.setMaxParts(parseInteger(text.toString()));
//            } else if (name.equals("IsTruncated")) {
//                result.setTruncated(Boolean.parseBoolean(text.toString()));
//            } else if (name.equals("Part")) {
//                result.getParts().add(currentPart);
//            } else if (name.equals("PartNumber")) {
//                currentPart.setPartNumber(Integer.parseInt(text.toString()));
//            } else if (name.equals("LastModified")) {
//                try {
//                    currentPart.setLastModified(ServiceUtils.parseIso8601Date(text.toString()));
//                } catch (ParseException e) {
//                    throw new SAXException(
//                            "Non-ISO8601 date for LastModified in list parts result: "
//                            + text.toString(), e);
//                }
//            } else if (name.equals("ETag")) {
//                currentPart.setETag(ServiceUtils.removeQuotes(text.toString()));
//            } else if (name.equals("Size")) {
//                currentPart.setSize(Long.parseLong(text.toString()));
//            }
//        }
//
//        @Override
//        public void characters(char ch[], int start, int length) {
//            this.text.append(ch, start, length);
//        }
//    }

//    /*
//        HTTP/1.1 200 OK
//        x-amz-id-2: Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==
//        x-amz-request-id: 656c76696e6727732072657175657374
//        Date: Tue, 20 Sep 2012 20:34:56 GMT
//        Content-Type: application/xml
//        Transfer-Encoding: chunked
//        Connection: keep-alive
//        Server: AmazonS3
//
//        <?xml version="1.0" encoding="UTF-8"?>
//        <DeleteResult>
//            <Deleted>
//               <Key>Key</Key>
//               <VersionId>Version</VersionId>
//            </Deleted>
//            <Error>
//               <Key>Key</Key>
//               <VersionId>Version</VersionId>
//               <Code>Code</Code>
//               <Message>Message</Message>
//            </Error>
//            <Deleted>
//               <Key>Key</Key>
//               <DeleteMarker>true</DeleteMarker>
//               <DeleteMarkerVersionId>Version</DeleteMarkerVersionId>
//            </Deleted>
//        </DeleteResult>
//     */
//    public class DeleteObjectsHandler extends DefaultHandler {
//        private StringBuilder text;
//
//        private DeletedObject deletedObject = null;
//        private DeleteError error = null;
//        private List<DeletedObject> deletedObjects = new LinkedList<DeleteObjectsResult.DeletedObject>();
//        private List<DeleteError> deleteErrors = new LinkedList<MultiObjectDeleteException.DeleteError>();
//
//        public DeleteObjectsResponse getDeleteObjectResult() {
//            return new DeleteObjectsResponse(deletedObjects, deleteErrors);
//        }
//
//        @Override
//        public void startDocument() {
//            text = new StringBuilder();
//        }
//
//        @Override
//        public void startElement(String uri, String name, String qName, Attributes attrs) {
//            if ( name.equals("Deleted") ) {
//                deletedObject = new DeletedObject();
//            } else if ( name.equals("Error") ) {
//                error = new DeleteError();
//            } else if ( name.equals("Key") ) {
//            } else if ( name.equals("VersionId") ) {
//            } else if ( name.equals("Code") ) {
//            } else if ( name.equals("Message") ) {
//            } else if ( name.equals("DeleteMarker") ) {
//            } else if ( name.equals("DeleteMarkerVersionId") ) {
//            } else if ( name.equals("DeleteResult") ) {
//            } else {
//                log.warn("Unexpected tag: " + name);
//            }
//            text.setLength(0);
//        }
//
//        @Override
//        public void endElement(String uri, String name, String qName) throws SAXException {
//            if ( name.equals("Deleted") ) {
//                deletedObjects.add(deletedObject);
//                deletedObject = null;
//            } else if ( name.equals("Error") ) {
//                deleteErrors.add(error);
//                error = null;
//            } else if ( name.equals("Key") ) {
//                if ( deletedObject != null ) {
//                    deletedObject.setKey(text.toString());
//                } else if ( error != null ) {
//                    error.setKey(text.toString());
//                }
//            } else if ( name.equals("VersionId") ) {
//                if ( deletedObject != null ) {
//                    deletedObject.setVersionId(text.toString());
//                } else if ( error != null ) {
//                    error.setVersionId(text.toString());
//                }
//            } else if ( name.equals("Code") ) {
//                if ( error != null ) {
//                    error.setCode(text.toString());
//                }
//            } else if ( name.equals("Message") ) {
//                if ( error != null ) {
//                    error.setMessage(text.toString());
//                }
//            } else if ( name.equals("DeleteMarker") ) {
//                if ( deletedObject != null ) {
//                    deletedObject.setDeleteMarker(text.toString().equals("true"));
//                }
//            } else if ( name.equals("DeleteMarkerVersionId") ) {
//                if ( deletedObject != null ) {
//                    deletedObject.setDeleteMarkerVersionId(text.toString());
//                }
//            }
//        }
//
//        @Override
//        public void characters(char ch[], int start, int length) {
//            this.text.append(ch, start, length);
//        }
//    }

}
