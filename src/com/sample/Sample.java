package com.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.sina.SCSClientException;
import com.sina.SCSServiceException;
import com.sina.auth.AWSCredentials;
import com.sina.auth.BasicAWSCredentials;
import com.sina.event.ProgressEvent;
import com.sina.event.ProgressListener;
import com.sina.scs.SCS;
import com.sina.scs.SCSClient;
import com.sina.scs.model.AccessControlList;
import com.sina.scs.model.AccessKeyIdGrantee;
import com.sina.scs.model.Bucket;
import com.sina.scs.model.CompleteMultipartUploadRequest;
import com.sina.scs.model.InitiateMultipartUploadResult;
import com.sina.scs.model.ListPartsRequest;
import com.sina.scs.model.ObjectListing;
import com.sina.scs.model.ObjectMetadata;
import com.sina.scs.model.PartETag;
import com.sina.scs.model.PartListing;
import com.sina.scs.model.Permission;
import com.sina.scs.model.PutObjectRequest;
import com.sina.scs.model.PutObjectResult;
import com.sina.scs.model.S3Object;
import com.sina.scs.model.UploadPartRequest;
import com.sina.scs.transfer.TransferManager;
import com.sina.scs.transfer.Upload;
import com.sina.scs.transfer.internal.UploadPartRequestFactory;

public class Sample {

	String accessKey = "accessKey";
	String secretKey = "secretKey";
	
	AWSCredentials credentials = new BasicAWSCredentials(accessKey,secretKey);
	SCS conn = new SCSClient(credentials);
	
	/* Service操作 */
	
	/**
	 * 获取所有bucket
	 */
	public void getAllBuckets(){
		List<Bucket> list = conn.listBuckets();
		System.out.println("====getAllBuckets===="+list);
	}
	
	/* Bucket操作 */
	
	/**
	 * 创建bucket
	 */
	public void createBucket(){
		Bucket bucket = conn.createBucket("create-a-bucket11");
		
		System.out.println(bucket);
	}
	
	/**
	 * 删除bucket
	 */
	public void deleteBucket(){
		conn.deleteBucket("create-a-bucket1212121");
	}
	
	/**
	 * 获取bucket ACL
	 */
	public void getBucketAcl(){
		AccessControlList acl = conn.getBucketAcl("create-a-bucket11");
		System.out.println(acl);
	}
	
	/**
	 * 设置bucket acl
	 */
	public void putBucketAcl(){
		AccessControlList acl = new AccessControlList();
		acl.grantPermissions(AccessKeyIdGrantee.CANONICAL, Permission.Read,Permission.ReadAcp);
		acl.grantPermissions(AccessKeyIdGrantee.ANONYMOUSE,Permission.ReadAcp,Permission.Write,Permission.WriteAcp);
		acl.grantPermissions(new AccessKeyIdGrantee("SINA000000"+accessKey), Permission.Read,Permission.ReadAcp,Permission.Write,Permission.WriteAcp);
		
		conn.setBucketAcl("create-a-bucket11", acl);
	}
	
	/**
	 * 列bucket中所有文件
	 */
	public void listObjects(){
		ObjectListing objectListing = conn.listObjects("test11");
		System.out.println(objectListing);
	}
	
	/* Object操作 */
	/**
	 * 获取object metadata
	 */
	public void getObjectMetadata(){
		ObjectMetadata metadata = conn.getObjectMetadata("test11", "/aaa/bbb.txt");
		System.out.println(metadata.getUserMetadata());
		System.out.println(metadata.getETag());
		System.out.println(metadata.getLastModified());
		System.out.println(metadata.getRawMetadata());
	}
	
	/**
	 * 下载object 
	 * more detail:http://docs.aws.amazon.com/AmazonS3/latest/dev/RetrievingObjectUsingJava.html
  	 *	//		断点续传
	 *	//		GetObjectRequest rangeObjectRequest = new GetObjectRequest("test11", "/aaa/bbb.txt");
	 *	//		rangeObjectRequest.setRange(0, 10); // retrieve 1st 10 bytes.
	 *	//		S3Object objectPortion = conn.getObject(rangeObjectRequest);
	 *	//		
	 *	//		InputStream objectData = objectPortion.getObjectContent();
	 *	//		//Process the objectData stream.
	 *	//		objectData.close();
	 */
	public void getObject(){
		S3Object s3Obj = conn.getObject("test11", "/aaa/bbb.txt");
		InputStream in = s3Obj.getObjectContent();
		byte[] buf = new byte[1024];
		OutputStream out = null;
		try {
			out = new FileOutputStream(new File("dage1.txt"));
			int count;
			while( (count = in.read(buf)) != -1)
			{
			   if( Thread.interrupted() )
			   {
			       throw new InterruptedException();
			   }
			   out.write(buf, 0, count);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally{
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 上传文件
	 */
	public void putObject(){
		PutObjectResult putObjectResult = conn.putObject("asdasdasdasd", "asdf第三方地方阿333==斯蒂---芬这种111.pdf", new File("dage1.txt"));
		System.out.println(putObjectResult);
	}
	
	/**
	 * 拷贝object
	 */
	public void copyObject(){
		conn.copyObject("asdasdasdasd", "awsdas阿斯顿.txt", "asdasdasdasd", "aa按时发生地方2111.txt");
	}
	
	/**
	 * 秒传
	 */
	public void putObjectRelax(){
		conn.putObjectRelax("asdasdasdasd","magnet.txt","4322fec3dd44787585f818a2d7bfa85ae0b664ab",12526362624l);
	}
	
	/**
	 * 获取object metadata
	 */
	public void getObjectMeta(){
		ObjectMetadata objectMetadata = conn.getObjectMetadata("asdasdasdasd", "aaa111a.txt");
		System.out.println(objectMetadata.getUserMetadata());
		System.out.println(objectMetadata.getContentLength());
		System.out.println(objectMetadata.getRawMetadata());
		System.out.println(objectMetadata.getETag());
	}
	
	/**
	 * 设置object metadata
	 */
	@SuppressWarnings("serial")
	public void putObjectMeta(){
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setUserMetadata(new HashMap<String,String>(){{
					put("aaa","1111");
					put("bbb","222");
					put("ccc","3333");
					put("asdfdsaf","vvvvvv");
		}});
		conn.setObjectMetadata("asdasdasdasd", "aaa111a.txt", objectMetadata);
	}
	
	/**
	 * 删除Object
	 */
	public void deleteObject(){
		conn.deleteObject("asdasdasdasd", "aaa撒旦法第三方a.txt");
	}
	
	/**
	 * 获取object acl
	 */
	public void getObjectAcl(){
		AccessControlList acl = conn.getObjectAcl("asdasdasdasd", "awsdas阿斯顿.txt");
		System.out.println(acl);
	}
	
	/**
	 * 设置object acl
	 */
	public void putObjectAcl(){
		AccessControlList acl = new AccessControlList();
		acl.grantPermissions(AccessKeyIdGrantee.CANONICAL, Permission.Read,Permission.ReadAcp);
		acl.grantPermissions(AccessKeyIdGrantee.ANONYMOUSE,Permission.ReadAcp,Permission.Write,Permission.WriteAcp);
		acl.grantPermissions(new AccessKeyIdGrantee("SINA000000"+accessKey), Permission.Read,Permission.ReadAcp,Permission.Write,Permission.WriteAcp);
		
		conn.setObjectAcl("asdasdasdasd", "awsdas阿斯顿.txt", acl);
	}
	
	/* 分片上传文件 */
	
	/* TransferManager */
	public void putObjectByTransferManager(){
		TransferManager tf = new TransferManager(conn);
		Upload myUpload = tf.upload("asdasdasdasd", "从市场上菜市场市场上菜市场上厕所方阿333==斯蒂---芬这种111.pdf", new File("/Users/hanchao/Desktop/归档.zip"));
		
		// You can poll your transfer's status to check its progress
		if (myUpload.isDone() == false) {
			System.out.println("Transfer: " + myUpload.getDescription());
			System.out.println("  - State: " + myUpload.getState());
			System.out.println("  - Progress: "
					+ myUpload.getProgress().getBytesTransferred());
		}

		// Transfers also allow you to set a <code>ProgressListener</code> to
		// receive
		// asynchronous notifications about your transfer's progress.
		myUpload.addProgressListener(new ProgressListener(){
			@Override
			public void progressChanged(ProgressEvent progressEvent) {
				System.out.println(progressEvent);
			}
		});

		// Or you can block the current thread and wait for your transfer to
		// to complete. If the transfer fails, this method will throw an
		// SCSClientException or SCSServiceException detailing the reason.
		try {
			myUpload.waitForCompletion();
		} catch (SCSServiceException e) {
			e.printStackTrace();
		} catch (SCSClientException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 分片上传文件
	 * @throws Exception
	 */
	public void multipartsUpload() throws Exception{
		//初始化上传任务
		InitiateMultipartUploadResult initiateMultipartUploadResult = conn.initiateMultipartUpload("asdasdasdasd", "ababtest11111.txt");
		
		if(initiateMultipartUploadResult!=null){
			//分片上传
			List<PartETag> partETags = null;
			PutObjectRequest putObjectRequest = new PutObjectRequest(initiateMultipartUploadResult.getBucketName(),
					initiateMultipartUploadResult.getKey(), new File("/Users/hanchao/Desktop/归档.zip"));
			 try {
				long optimalPartSize = 5 * 1024 * 1024; //5M
	            UploadPartRequestFactory requestFactory = new UploadPartRequestFactory(putObjectRequest, initiateMultipartUploadResult.getUploadId()
	            		, optimalPartSize);
	
	            partETags = uploadPartsInSeries(requestFactory);
	        } catch (Exception e) {
	            throw e;
	        } finally {
	            if (putObjectRequest.getInputStream() != null) {
					try {
						putObjectRequest.getInputStream().close();
					} catch (Exception e) {
						throw e;
					}
	            }
	        }
		
			 //分片列表
			PartListing partList = conn.listParts(new ListPartsRequest(initiateMultipartUploadResult.getBucketName(),
											initiateMultipartUploadResult.getKey(),
											initiateMultipartUploadResult.getUploadId()));
			System.out.println("已上传的文件分片列表:\n"+partList);
			
			//分片合并，完成上传
			ObjectMetadata objectMetadata = conn.completeMultipartUpload(new CompleteMultipartUploadRequest(putObjectRequest.getBucketName(),
					putObjectRequest.getKey(), initiateMultipartUploadResult.getUploadId(), partETags));
			
			System.out.println("合并文件结果:\n");
			System.out.println(objectMetadata.getUserMetadata());
			System.out.println(objectMetadata.getContentLength());
			System.out.println(objectMetadata.getRawMetadata());
			System.out.println(objectMetadata.getETag());
		}
		
	}
	
	/**
     * Uploads all parts in the request in serial in this thread, then completes
     * the upload and returns the result.
     */
    private List<PartETag> uploadPartsInSeries(UploadPartRequestFactory requestFactory) {

        final List<PartETag> partETags = new ArrayList<PartETag>();

        while (requestFactory.hasMoreRequests()) {
            UploadPartRequest uploadPartRequest = requestFactory.getNextUploadPartRequest();
            // Mark the stream in case we need to reset it
            InputStream inputStream = uploadPartRequest.getInputStream();
            if (inputStream != null && inputStream.markSupported()) {
                if (uploadPartRequest.getPartSize() >= Integer.MAX_VALUE) {
                    inputStream.mark(Integer.MAX_VALUE);
                } else {
                    inputStream.mark((int)uploadPartRequest.getPartSize());
                }
            }
            partETags.add(conn.uploadPart(uploadPartRequest).getPartETag());
        }

        return partETags;
    }
	
	/* 生成url */
	public void generateUrl(){
		Date expiration = new Date();
        long epochMillis = expiration.getTime();
        epochMillis += 60*5*1000;
        expiration = new Date(epochMillis);   
        
		URL presignedUrl = conn.generatePresignedUrl("asdasdasdasd", "awsdas阿斯顿.txt", expiration);
		System.out.println(presignedUrl);
	}
	
	public static void main(String[] args){
		java.util.logging.Logger.getLogger("com.sina").setLevel(java.util.logging.Level.FINEST);

		Sample sample = new Sample();
		/* Service操作 */
//		sample.getAllBuckets();
		/* Bucket操作 */
//		sample.createBucket();
		sample.deleteBucket();
//		sample.getBucketAcl();
//		sample.putBucketAcl();
//		sample.listObjects();
		/* Object操作 */
//		sample.getObjectMetadata();
//		sample.getObject();
//		sample.putObject();
//		sample.copyObject();
//		sample.putObjectRelax();
//		sample.getObjectMeta();
//		sample.putObjectMeta();
//		sample.deleteObject();
//		sample.getObjectAcl();
//		sample.putObjectAcl();
		/* 生成url */
//		sample.generateUrl();
		
		
//		try {
//			sample.multipartsUpload();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		/* TransferManager */
//		sample.putObjectByTransferManager();
	}
	
}
