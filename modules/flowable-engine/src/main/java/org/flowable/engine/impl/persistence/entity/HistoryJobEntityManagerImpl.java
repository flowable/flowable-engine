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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.HistoryJobQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.HistoryJobDataManager;
import org.flowable.engine.runtime.HistoryJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HistoryJobEntityManagerImpl extends AbstractEntityManager<HistoryJobEntity> implements HistoryJobEntityManager {

    private static final Logger logger = LoggerFactory.getLogger(HistoryJobEntityManagerImpl.class);

    protected HistoryJobDataManager historyJobDataManager;

    public HistoryJobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryJobDataManager historyJobDataManager) {
        super(processEngineConfiguration);
        this.historyJobDataManager = historyJobDataManager;
    }

    @Override
    protected DataManager<HistoryJobEntity> getDataManager() {
        return historyJobDataManager;
    }

    @Override
    public void insertHistoryJobEntity(HistoryJobEntity historyJobEntity) {
        insert(historyJobEntity, true);
    }

    public List<HistoryJobEntity> findHistoryJobsToExecute(Page page) {
        return historyJobDataManager.findHistoryJobsToExecute(page);
    }

    @Override
    public List<HistoryJobEntity> findHistoryJobsByExecutionId(String executionId) {
        return historyJobDataManager.findHistoryJobsByExecutionId(executionId);
    }

    @Override
    public List<HistoryJobEntity> findHistoryJobsByProcessInstanceId(String processInstanceId) {
        return historyJobDataManager.findHistoryJobsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<HistoryJobEntity> findExpiredHistoryJobs(Page page) {
        return historyJobDataManager.findExpiredHistoryJobs(page);
    }

    @Override
    public void resetExpiredHistoryJob(String jobId) {
        historyJobDataManager.resetExpiredHistoryJob(jobId);
    }

    @Override
    public List<HistoryJob> findHistoryJobsByQueryCriteria(HistoryJobQueryImpl jobQuery, Page page) {
        return historyJobDataManager.findHistoryJobsByQueryCriteria(jobQuery, page);
    }

    @Override
    public long findHistoryJobCountByQueryCriteria(HistoryJobQueryImpl jobQuery) {
        return historyJobDataManager.findHistoryJobCountByQueryCriteria(jobQuery);
    }

    @Override
    public void updateHistoryJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        historyJobDataManager.updateHistoryJobTenantIdForDeployment(deploymentId, newTenantId);
    }

    @Override
    public void delete(HistoryJobEntity jobEntity) {
        super.delete(jobEntity);

        deleteExceptionByteArrayRef(jobEntity);
        deleteAdvancedJobHandlerConfigurationByteArrayRef(jobEntity);

        // Send event
        if (getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, this));
        }
    }

    /**
     * Deletes a the byte array used to store the exception information. Subclasses may override to provide custom implementations.
     */
    protected void deleteExceptionByteArrayRef(HistoryJobEntity jobEntity) {
        ByteArrayRef exceptionByteArrayRef = jobEntity.getExceptionByteArrayRef();
        if (exceptionByteArrayRef != null) {
            exceptionByteArrayRef.delete();
        }
    }
    
    protected void deleteAdvancedJobHandlerConfigurationByteArrayRef(HistoryJobEntity jobEntity) {
        ByteArrayRef configurationByteArrayRef = jobEntity.getAdvancedJobHandlerConfigurationByteArrayRef();
        if (configurationByteArrayRef != null) {
            configurationByteArrayRef.delete();
        }
    }

    public HistoryJobDataManager getHistoryJobDataManager() {
        return historyJobDataManager;
    }

    public void setHistoryJobDataManager(HistoryJobDataManager historyJobDataManager) {
        this.historyJobDataManager = historyJobDataManager;
    }

}
