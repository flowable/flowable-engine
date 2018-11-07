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

package org.flowable.job.service.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.HistoryJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.data.HistoryJobDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HistoryJobEntityManagerImpl extends JobInfoEntityManagerImpl<HistoryJobEntity> implements HistoryJobEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryJobEntityManagerImpl.class);

    protected HistoryJobDataManager historyJobDataManager;

    public HistoryJobEntityManagerImpl(JobServiceConfiguration jobServiceConfiguration, HistoryJobDataManager historyJobDataManager) {
        super(jobServiceConfiguration, historyJobDataManager);
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

        deleteByteArrayRef(jobEntity.getExceptionByteArrayRef());
        deleteByteArrayRef(jobEntity.getAdvancedJobHandlerConfigurationByteArrayRef());
        deleteByteArrayRef(jobEntity.getCustomValuesByteArrayRef());

        // Send event
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, this));
        }
    }

    @Override
    public void deleteNoCascade(HistoryJobEntity historyJobEntity) {
        super.delete(historyJobEntity);
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, this));
        }
    }

    public HistoryJobDataManager getHistoryJobDataManager() {
        return historyJobDataManager;
    }

    public void setHistoryJobDataManager(HistoryJobDataManager historyJobDataManager) {
        this.historyJobDataManager = historyJobDataManager;
    }

}
