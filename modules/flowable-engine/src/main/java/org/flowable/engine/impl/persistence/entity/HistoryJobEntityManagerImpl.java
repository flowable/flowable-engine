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
public class HistoryJobEntityManagerImpl extends JobInfoEntityManagerImpl<HistoryJobEntity> implements HistoryJobEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryJobEntityManagerImpl.class);

    protected HistoryJobDataManager historyJobDataManager;

    public HistoryJobEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryJobDataManager historyJobDataManager) {
        super(processEngineConfiguration, historyJobDataManager);
        this.historyJobDataManager = historyJobDataManager;
    }

    @Override
    protected DataManager<HistoryJobEntity> getDataManager() {
        return historyJobDataManager;
    }
    
    @Override
    public List<HistoryJob> findHistoryJobsByQueryCriteria(HistoryJobQueryImpl jobQuery) {
        return historyJobDataManager.findHistoryJobsByQueryCriteria(jobQuery);
    }

    @Override
    public long findHistoryJobCountByQueryCriteria(HistoryJobQueryImpl jobQuery) {
        return historyJobDataManager.findHistoryJobCountByQueryCriteria(jobQuery);
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
    
    @Override
    public void deleteNoCascade(HistoryJobEntity historyJobEntity) {
        super.delete(historyJobEntity);
        if (getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, this));
        }
    }

    public HistoryJobDataManager getHistoryJobDataManager() {
        return historyJobDataManager;
    }

    public void setHistoryJobDataManager(HistoryJobDataManager historyJobDataManager) {
        this.historyJobDataManager = historyJobDataManager;
    }

}
