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
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;

public abstract class AbstractJobEntityMapper {

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
        jobDocument.append("_id", jobEntity.getId());
        jobDocument.append("createTime", jobEntity.getCreateTime());
        jobDocument.append("duedate", jobEntity.getDuedate());
        jobDocument.append("exceptionMessage", jobEntity.getExceptionMessage());
        jobDocument.append("isExclusive", jobEntity.isExclusive());
        jobDocument.append("executionId", jobEntity.getExecutionId());
        jobDocument.append("jobHandlerConfiguration", jobEntity.getJobHandlerConfiguration());
        jobDocument.append("jobHandlerType", jobEntity.getJobHandlerType());
        jobDocument.append("jobType", jobEntity.getJobType());
        jobDocument.append("processDefinitionId", jobEntity.getProcessDefinitionId());
        jobDocument.append("processInstanceId", jobEntity.getProcessInstanceId());
        jobDocument.append("repeat", jobEntity.getRepeat());
        jobDocument.append("retries", jobEntity.getRetries());
        jobDocument.append("revision", jobEntity.getRevision());
        jobDocument.append("scopeDefinitionId", jobEntity.getScopeDefinitionId());
        jobDocument.append("scopeId", jobEntity.getScopeId());
        jobDocument.append("scopeType", jobEntity.getScopeType());
        jobDocument.append("subScopeId", jobEntity.getSubScopeId());
        jobDocument.append("tenantId", jobEntity.getTenantId());
        
        return jobDocument;
    }

}
