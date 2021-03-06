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
package com.sina.scs.transfer.internal;

import com.sina.scs.transfer.TransferProgress;

public class TransferProgressImpl extends TransferProgress {
    
    public synchronized void updateProgress(long bytes) {
        this.bytesTransferred += bytes;
    }

    /**
     * @deprecated Replaced by {@link #setBytesTransferred()}
     */
    @Deprecated
    public void setBytesTransfered(long bytesTransferred) {
        setBytesTransferred(bytesTransferred);
    }

    public void setBytesTransferred(long bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }

    public void setTotalBytesToTransfer(long totalBytesToTransfer) {
        this.totalBytesToTransfer = totalBytesToTransfer;
    }
}
