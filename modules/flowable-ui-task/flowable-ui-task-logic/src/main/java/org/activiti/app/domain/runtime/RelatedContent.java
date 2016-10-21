/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.app.domain.runtime;

import java.util.Date;


/**
 * @author Frederik Heremans
 */
public class RelatedContent {

    protected String id;
    protected String name;
    protected String mimeType;
    protected Date created;
    private String createdBy;
    private String taskId;
    private String processInstanceId;
    private String source;
    private String sourceId;
    private boolean contentAvailable;
    private boolean locked;
    private Date lockDate;
    private Date lockExpirationDate;
    private String lockOwner;
    private boolean checkedOut;
    private boolean checkedOutToLocal;
    private Date checkoutDate;
    private String contentStoreId;
    private String checkoutOwner;
    protected Date lastModified;
    private String lastModifiedBy;
    private String field;
    private boolean relatedContent = false;
    private boolean link = false;
    private String linkUrl;
    private Long contentSize;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    // new stuff
    
    public Date getLastModified() {
    	return this.lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
    
    public boolean isLocked() {
    	return locked;
    }
    
    public Date getLockDate() {
    	return lockDate;
    }
    
    public Date getLockExpiration() {
    	return lockExpirationDate;
    }
    
    public String getLockOwner() {
    	return lockOwner;
    }
    
    public boolean isCheckedOut() {
    	return checkedOut;
    }
    
    public boolean isCheckedOutToLocal() {
    	return checkedOutToLocal;
    }
    
    public Date getCheckoutDate() {
    	return checkoutDate;
    }
    
    public String getCheckoutOwner() {
    	return checkoutOwner;
    }
    
    public String getVersionLabel() {
    	return "1.0";
    }
    
    public void setLockDate(Date lockDate) {
        this.lockDate = lockDate;
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
    public void setLockExpirationDate(Date lockExpirationDate) {
        this.lockExpirationDate = lockExpirationDate;
    }
    
    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }
    
    public void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }
    
    public void setCheckedOutToLocal(boolean checkedOutToLocal) {
        this.checkedOutToLocal = checkedOutToLocal;
    }
    
    public void setCheckoutDate(Date checkoutDate) {
        this.checkoutDate = checkoutDate;
    }
    
    public void setCheckoutOwner(String checkoutOwner) {
        this.checkoutOwner = checkoutOwner;
    }
    
    public void setContentAvailable(boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }
    
    public boolean isContentAvailable() {
        return contentAvailable;
    }
    
    public String getContentStoreId() {
        return contentStoreId;
    }
    
    public void setContentStoreId(String contentStoreId) {
        this.contentStoreId = contentStoreId;
    }

    public void setField(String field) {
        this.field = field;
    }
    
    public String getField() {
        return field;
    }
    
    public void setRelatedContent(boolean relatedContent) {
        this.relatedContent = relatedContent;
    }
    
    public boolean isRelatedContent() {
        return relatedContent;
    }
    
    public boolean isLink() {
        return link;
    }
    
    public void setLink(boolean link) {
        this.link = link;
    }
    
    public String getLinkUrl() {
        return linkUrl;
    }
    
    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }
    
    public Long getContentSize() {
        return contentSize;
    }
    
    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }
}
