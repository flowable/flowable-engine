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
package org.flowable.cmmn.engine.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchQuery;
import org.flowable.cmmn.api.CmmnHistoryCleaningManager;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;

public class DefaultCmmnHistoryCleaningManager implements CmmnHistoryCleaningManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public DefaultCmmnHistoryCleaningManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public HistoricCaseInstanceQuery createHistoricCaseInstanceCleaningQuery() {
        HistoricCaseInstanceQueryImpl historicCaseInstanceQuery = new HistoricCaseInstanceQueryImpl(
                cmmnEngineConfiguration.getCommandExecutor(), cmmnEngineConfiguration);
        historicCaseInstanceQuery.finishedBefore(getEndedBefore());
        return historicCaseInstanceQuery;
    }

    @Override
    public BatchQuery createBatchCleaningQuery() {
        return cmmnEngineConfiguration.getCmmnManagementService().createBatchQuery()
                .completeTimeLowerThan(getEndedBefore())
                .batchType(Batch.HISTORIC_CASE_DELETE_TYPE);
    }

    protected Date getEndedBefore() {
        Duration endedAfterDuration = cmmnEngineConfiguration.getCleanInstancesEndedAfter();
        Instant endedBefore = Instant.now().minus(endedAfterDuration);
        return Date.from(endedBefore);
    }
}
