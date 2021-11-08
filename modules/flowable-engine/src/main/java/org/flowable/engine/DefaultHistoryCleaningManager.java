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
package org.flowable.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchQuery;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

public class DefaultHistoryCleaningManager implements HistoryCleaningManager {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    public DefaultHistoryCleaningManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public HistoricProcessInstanceQuery createHistoricProcessInstanceCleaningQuery() {
        return processEngineConfiguration.getHistoryService()
                .createHistoricProcessInstanceQuery()
                .finishedBefore(getEndedBefore());
    }

    @Override
    public BatchQuery createBatchCleaningQuery() {
        return processEngineConfiguration.getManagementService().createBatchQuery()
                .completeTimeLowerThan(getEndedBefore())
                .batchType(Batch.HISTORIC_PROCESS_DELETE_TYPE);
    }

    protected Date getEndedBefore() {
        Duration endedAfterDuration = processEngineConfiguration.getCleanInstancesEndedAfter();
        Instant endedBefore = Instant.now().minus(endedAfterDuration);
        return Date.from(endedBefore);
    }
}
