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
package org.flowable.job.service.impl;

import java.util.Date;
import java.util.Map;

import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;

/**
 * @author Filip Hrisafov
 */
public class AcquiredExternalWorkerJobImpl implements AcquiredExternalWorkerJob {

    protected final ExternalWorkerJobEntity job;
    protected final Map<String, Object> variables;

    public AcquiredExternalWorkerJobImpl(ExternalWorkerJobEntity job, Map<String, Object> variables) {
        this.job = job;
        this.variables = variables;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public Date getDuedate() {
        return job.getDuedate();
    }

    @Override
    public String getProcessInstanceId() {
        return job.getProcessInstanceId();
    }

    @Override
    public String getExecutionId() {
        return job.getExecutionId();
    }

    @Override
    public String getProcessDefinitionId() {
        return job.getProcessDefinitionId();
    }

    @Override
    public String getCategory() {
        return job.getCategory();
    }

    @Override
    public String getJobType() {
        return job.getJobType();
    }

    @Override
    public String getElementId() {
        return job.getElementId();
    }

    @Override
    public String getElementName() {
        return job.getElementName();
    }

    @Override
    public String getScopeId() {
        return job.getScopeId();
    }

    @Override
    public String getSubScopeId() {
        return job.getSubScopeId();
    }

    @Override
    public String getScopeType() {
        return job.getScopeType();
    }

    @Override
    public String getScopeDefinitionId() {
        return job.getScopeDefinitionId();
    }

    @Override
    public String getCorrelationId() {
        return job.getCorrelationId();
    }

    @Override
    public boolean isExclusive() {
        return job.isExclusive();
    }

    @Override
    public Date getCreateTime() {
        return job.getCreateTime();
    }

    @Override
    public String getId() {
        return job.getId();
    }

    @Override
    public int getRetries() {
        return job.getRetries();
    }

    @Override
    public String getExceptionMessage() {
        return job.getExceptionMessage();
    }

    @Override
    public String getTenantId() {
        return job.getTenantId();
    }

    @Override
    public String getJobHandlerType() {
        return job.getJobHandlerType();
    }

    @Override
    public String getJobHandlerConfiguration() {
        return job.getJobHandlerConfiguration();
    }

    @Override
    public String getCustomValues() {
        return job.getCustomValues();
    }

    @Override
    public String getLockOwner() {
        return job.getLockOwner();
    }

    @Override
    public Date getLockExpirationTime() {
        return job.getLockExpirationTime();
    }
}
