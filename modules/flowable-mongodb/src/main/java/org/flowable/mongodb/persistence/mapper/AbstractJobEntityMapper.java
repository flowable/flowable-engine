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
package org.flowable.mongodb.persistence.mapper;

import org.bson.Document;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;

import com.sun.webkit.dom.EntityImpl;

public abstract class AbstractJobEntityMapper<T extends Entity> extends AbstractEntityToDocumentMapper<T> {

    public void copyJobInfoFromDocument(Document document, AbstractRuntimeJobEntity jobEntity) {
        jobEntity.setId(document.getString("_id"));
        jobEntity.setCreateTime(document.getDate("createTime"));
        jobEntity.setDuedate(document.getDate("duedate"));
        jobEntity.setExceptionMessage(document.getString("exceptionMessage"));
        jobEntity.setExclusive(document.getBoolean("isExclusive"));
        jobEntity.setExecutionId(document.getString("executionId"));
        jobEntity.setJobHandlerConfiguration(document.getString("jobHandlerConfiguration"));
        jobEntity.setJobHandlerType(document.getString("jobHandlerType"));
        jobEntity.setJobType(document.getString("jobType"));
        jobEntity.setProcessDefinitionId(document.getString("processDefinitionId"));
        jobEntity.setProcessInstanceId(document.getString("processInstanceId"));
        jobEntity.setRepeat(document.getString("repeat"));
        jobEntity.setRetries(document.getInteger("retries"));
        jobEntity.setRevision(document.getInteger("revision"));
        jobEntity.setScopeDefinitionId(document.getString("scopeDefinitionId"));
        jobEntity.setScopeId(document.getString("scopeId"));
        jobEntity.setScopeType(document.getString("scopeType"));
        jobEntity.setSubScopeId(document.getString("subScopeId"));
        jobEntity.setTenantId(document.getString("tenantId"));
    }
    
    public Document copyJobInfoToDocument(AbstractRuntimeJobEntity jobEntity) {
        Document jobDocument = new Document();
        appendIfNotNull(jobDocument, "_id", jobEntity.getId());
        appendIfNotNull(jobDocument, "createTime", jobEntity.getCreateTime());
        appendIfNotNull(jobDocument, "duedate", jobEntity.getDuedate());
        appendIfNotNull(jobDocument, "exceptionMessage", jobEntity.getExceptionMessage());
        appendIfNotNull(jobDocument, "isExclusive", jobEntity.isExclusive());
        appendIfNotNull(jobDocument, "executionId", jobEntity.getExecutionId());
        appendIfNotNull(jobDocument, "jobHandlerConfiguration", jobEntity.getJobHandlerConfiguration());
        appendIfNotNull(jobDocument, "jobHandlerType", jobEntity.getJobHandlerType());
        appendIfNotNull(jobDocument, "jobType", jobEntity.getJobType());
        appendIfNotNull(jobDocument, "processDefinitionId", jobEntity.getProcessDefinitionId());
        appendIfNotNull(jobDocument, "processInstanceId", jobEntity.getProcessInstanceId());
        appendIfNotNull(jobDocument, "repeat", jobEntity.getRepeat());
        appendIfNotNull(jobDocument, "retries", jobEntity.getRetries());
        appendIfNotNull(jobDocument, "revision", jobEntity.getRevision());
        appendIfNotNull(jobDocument, "scopeDefinitionId", jobEntity.getScopeDefinitionId());
        appendIfNotNull(jobDocument, "scopeId", jobEntity.getScopeId());
        appendIfNotNull(jobDocument, "scopeType", jobEntity.getScopeType());
        appendIfNotNull(jobDocument, "subScopeId", jobEntity.getSubScopeId());
        appendIfNotNull(jobDocument, "tenantId", jobEntity.getTenantId());
        return jobDocument;
    }

}
