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
package org.flowable.cmmn.engine.impl.job;

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.batch.service.impl.persistence.entity.BatchEntity;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

public class CaseInstanceMigrationStatusJobHandler extends AbstractCaseInstanceMigrationJobHandler {

    public static final String TYPE = "case-migration-status";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();

        String batchId = getBatchIdFromHandlerCfg(configuration);
        Batch batch = batchService.getBatch(batchId);

        List<BatchPart> batchParts = batchService.findBatchPartsByBatchId(batchId);
        int completedBatchParts = 0;
        int failedBatchParts = 0;
        for (BatchPart batchPart : batchParts) {
            if (batchPart.getCompleteTime() != null) {
                completedBatchParts++;

                if (CaseInstanceBatchMigrationResult.RESULT_FAIL.equals(batchPart.getStatus())) {
                    failedBatchParts++;
                }
            }
        }
        if (completedBatchParts == batchParts.size()) {
            batchService.completeBatch(batch.getId(), CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
            job.setRepeat(null);

        } else {
            if (batchParts.size() == 0) {
                updateBatchStatus(batch, batchService);
                job.setRepeat(null);

            } else {
                updateBatchStatus(batch, batchService);
            }
        }
    }

    protected void updateBatchStatus(Batch batch, BatchService batchService) {
        ((BatchEntity) batch).setStatus(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
        batchService.updateBatch(batch);
    }
}
