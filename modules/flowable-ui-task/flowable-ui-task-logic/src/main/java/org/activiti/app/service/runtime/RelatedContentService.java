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
package org.activiti.app.service.runtime;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.app.domain.runtime.RelatedContent;
import org.activiti.app.repository.runtime.RelatedContentRepository;
import org.activiti.content.storage.api.ContentMetaDataKeys;
import org.activiti.content.storage.api.ContentObject;
import org.activiti.content.storage.api.ContentStorage;
import org.activiti.engine.runtime.Clock;
import org.activiti.idm.api.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Frederik Heremans
 */
@Service
public class RelatedContentService {
    
    @Autowired
    protected RelatedContentRepository contentRepository;

    @Autowired
    protected ContentStorage contentStorage;

    @Autowired
    protected Clock clock;
    
    public RelatedContent get(String id) {
      return contentRepository.get(id);
    }

    public List<RelatedContent> getRelatedContent(String source, String sourceId) {
        return contentRepository.findBySourceAndSourceId(source, sourceId);
    }

    public List<RelatedContent> getRelatedContentForTask(String taskId) {
        return contentRepository.findAllRelatedByTaskId(taskId);
    }

    public List<RelatedContent> getRelatedContentForProcessInstance(String processInstanceId) {
        return contentRepository.findAllRelatedByProcessInstanceId(processInstanceId);
    }
    
    public List<RelatedContent> getFieldContentForProcessInstance(String processInstanceId, String field) {
        return contentRepository.findAllByProcessInstanceIdAndField(processInstanceId, field);
    }
    
    public List<RelatedContent> getFieldContentForTask(String taskId) {
        return contentRepository.findAllFieldBasedContentByTaskId(taskId);
    }
    
    public List<RelatedContent> getAllFieldContentForProcessInstance(String processInstanceId) {
        return contentRepository.findAllFieldBasedContentByProcessInstanceId(processInstanceId);
    }
    
    public List<RelatedContent> getAllFieldContentForTask(String taskId, String field) {
        return contentRepository.findAllByTaskIdAndField(taskId, field);
    }

    @Transactional
    public RelatedContent createRelatedContent(User user, String name, String source, String sourceId, String taskId,
            String processId, String field, String mimeType, InputStream data, Long lengthHint) {
        
        return createRelatedContent(user, name, source, sourceId, taskId, processId, 
                mimeType, data, lengthHint, false, false, field);
    }
    
    @Transactional
    public RelatedContent createRelatedContent(User user, String name, String source, String sourceId, String taskId,
            String processId, String mimeType, InputStream data, Long lengthHint, boolean relatedContent, boolean link) {
    
        return createRelatedContent(user, name, source, sourceId, taskId, processId, mimeType, data, lengthHint, relatedContent, link, null);
    }
    
    protected RelatedContent createRelatedContent(User user, String name, String source, String sourceId, String taskId,
            String processId, String mimeType, InputStream data, Long lengthHint, boolean relatedContent, boolean link, String field) {
        
        Date timestamp = clock.getCurrentTime();
    	final RelatedContent newContent = new RelatedContent();
        newContent.setName(name);
        newContent.setSource(source);
        newContent.setSourceId(sourceId);
        newContent.setTaskId(taskId);
        newContent.setProcessInstanceId(processId);
        newContent.setCreatedBy(user.getId());
        newContent.setCreated(timestamp);
        newContent.setLastModifiedBy(user.getId());
        newContent.setLastModified(timestamp);
        newContent.setMimeType(mimeType);
        newContent.setRelatedContent(relatedContent);
        newContent.setLink(link);
        newContent.setField(field);
        
        if (data != null) {

            // Stream given, write to store and save a reference to the content object
            Map<String, Object> metaData = new HashMap<String, Object>();
            if (taskId != null) {
              metaData.put(ContentMetaDataKeys.TASK_ID, taskId);
            } else {
              if (processId != null) {
                metaData.put(ContentMetaDataKeys.PROCESS_INSTANCE_ID, processId);
              }
            }
            ContentObject createContentObject = contentStorage.createContentObject(data, lengthHint, metaData);
            newContent.setContentStoreId(createContentObject.getId());
            newContent.setContentAvailable(true);

            // After storing the stream, store the length to be accessible without having to consult the
            // underlying content storage to get file size
            newContent.setContentSize(createContentObject.getContentLength());
            
        } else {
            
            if (link) {
                // Mark content as available, since it will never be fetched and copied
                newContent.setContentAvailable(true);
            } else {
                // Content not (yet) available
                newContent.setContentAvailable(false);
            }
        }
        
        contentRepository.save(newContent);
        
        return newContent;
    }

    public RelatedContent getRelatedContent(String id, boolean includeOwner) {
        RelatedContent content = contentRepository.get(id);
        
        if (content != null && includeOwner) {
            // Touch related entities
            content.getCheckoutOwner();
            content.getLockOwner();
        }

        return content;
    }
    
    @Transactional
    public void deleteRelatedContent(RelatedContent content) {
        if (content.getContentStoreId() != null) {
            final String storeId = content.getContentStoreId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    contentStorage.deleteContentObject(storeId);
                }
            });
        }
        
        contentRepository.delete(content);
    }

    @Transactional
    public boolean lockContent(RelatedContent content, int timeOut, User user) {
        content.setLockDate(clock.getCurrentTime());
        content.setLocked(true);
        content.setLockOwner(user.getId());

        // Set expiration date based on timeout
        Calendar expiration = Calendar.getInstance();
        expiration.setTime(content.getLockDate());
        expiration.add(Calendar.SECOND, timeOut);

        content.setLockExpirationDate(expiration.getTime());

        contentRepository.save(content);
        return true;
    }

    @Transactional
    public boolean checkout(RelatedContent content, User user, boolean toLocal) {
        content.setCheckoutDate(clock.getCurrentTime());
        content.setCheckedOut(true);
        content.setCheckedOutToLocal(toLocal);
        content.setCheckoutOwner(user.getId());

        contentRepository.save(content);
        return true;
    }

    @Transactional
    public boolean unlock(RelatedContent content) {
        content.setLockDate(null);
        content.setLockExpirationDate(null);
        content.setLockOwner(null);
        content.setLocked(false);

        contentRepository.save(content);
        return true;
    }

    @Transactional
    public boolean uncheckout(RelatedContent content) {
        content.setCheckoutDate(null);
        content.setCheckedOut(false);
        content.setCheckedOutToLocal(false);
        content.setCheckoutOwner(null);

        contentRepository.save(content);
        return true;
    }

    @Transactional
    public boolean checkin(RelatedContent content, String comment, boolean keepCheckedOut) {
        if (!keepCheckedOut) {
            content.setCheckoutDate(null);
            content.setCheckedOut(false);
            content.setCheckoutOwner(null);

            // TODO: store comment
            contentRepository.save(content);
            return true;
        }
        return false;
    }

    @Transactional
    public void updateRelatedContentData(String relatedContentId, String contentStoreId, InputStream contentStream, Long lengthHint, User user) {
        Date timestamp = clock.getCurrentTime();
        
        ContentObject updatedContent = contentStorage.updateContentObject(contentStoreId, contentStream, lengthHint, null);
        
        RelatedContent relatedContent = contentRepository.get(relatedContentId);
        relatedContent.setLastModifiedBy(user.getId());
        relatedContent.setLastModified(timestamp);
        relatedContent.setContentSize(lengthHint);
        
        contentRepository.save(relatedContent);
    }

    @Transactional
    public void updateName(String relatedContentId, String newName) {
        RelatedContent relatedContent = contentRepository.get(relatedContentId);
        relatedContent.setName(newName);
        contentRepository.save(relatedContent);
    }
    
    /**
     * Marks a piece of content as permanent and flags it being used as selected content in the given field,
     * for the given process instance id and (optional) task id.
     */
    @Transactional
    public void setContentField(String relatedContentId, String field, String processInstanceId, String taskId) {
        final RelatedContent relatedContent = contentRepository.get(relatedContentId);
        if (relatedContent != null) {
          relatedContent.setProcessInstanceId(processInstanceId);
          relatedContent.setTaskId(taskId);
          relatedContent.setRelatedContent(false);
          relatedContent.setField(field);
          contentRepository.save(relatedContent);
        }
    }
    
    @Transactional
    public void storeRelatedContent(RelatedContent relatedContent) {
        contentRepository.save(relatedContent);
    }
    
    public ContentStorage getContentStorage() {
        return contentStorage;
    }

    /**
     * Deletes all content related to the given process instance. This includes all field content for a process instance, all
     * field content on tasks and all related content on tasks. The raw content data will also be removed from content storage
     * as well as all renditions and rendition data.
     */
    @Transactional
    public void deleteContentForProcessInstance(String processInstanceId) {
        List<RelatedContent> contentList = contentRepository.findAllContentByProcessInstanceId(processInstanceId);
        final Set<String> storageIds = new HashSet<String>();
        
        for (RelatedContent relatedContent : contentList) {
            if (relatedContent.getContentStoreId() != null) {
              storageIds.add(relatedContent.getContentStoreId());
          }
        }
        
        // Delete raw content AFTER transaction has been committed to prevent missing content on rollback
        if(!storageIds.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                   for(String id : storageIds) {
                       contentStorage.deleteContentObject(id);
                   }
                }
            });
        }
        
        // Batch delete all RelatedContent entities
        contentRepository.deleteAllContentByProcessInstanceId(processInstanceId);
    }
}
