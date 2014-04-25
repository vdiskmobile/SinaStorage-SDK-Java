package com.sina.scs.model.transform;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.sina.SCSClientException;
import com.sina.scs.model.AccessControlList;
import com.sina.scs.model.Grant;
import com.sina.util.json.Jackson;

public class AclJsonFactory {

	/**
	 * 转换成json格式
	 * {  
	 *		'SINA0000000000000001' :  [ "read", "read_acp" , "write", "write_acp" ],
	 *		'GRPS000000ANONYMOUSE' :  [ "read", "read_acp" , "write", "write_acp" ],
	 *		'GRPS0000000CANONICAL' :  [ "read", "read_acp" , "write", "write_acp" ],
	 *	}
	 * @param acl
	 * @return
	 * @throws SCSClientException
	 */
    public byte[] convertToJsonByteArray(AccessControlList acl) throws SCSClientException{
    	if (acl == null) {
            throw new SCSClientException("Invalid AccessControlList: acl is null");
        }
    	
    	HashMap<String,String[]> jsonMap = new HashMap<String,String[]>();
    	for (Grant grant : acl.getGrants()) {
    		jsonMap.put(grant.getGrantee().getIdentifier(), grant.getPermissionsForJsonArray());
        }
    	
    	String jsonString = Jackson.toJsonString(jsonMap);
    	
    	try {
			return jsonString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SCSClientException(e.getMessage());
		}
    	
    }
}
