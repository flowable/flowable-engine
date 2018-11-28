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

package org.flowable.engine.impl.cmd;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessMigrationBatch;

/**
 * @author Dennis Federico
 */
public class GetProcessInstanceMigrationValidationBatchCmd implements Command<ProcessMigrationBatch> {

    protected String batchId;
    protected boolean fetchResources;

    public GetProcessInstanceMigrationValidationBatchCmd(String batchId, boolean fetchResources) {
        this.batchId = batchId;
        this.fetchResources = fetchResources;
    }

    @Override
    public ProcessMigrationBatch execute(CommandContext commandContext) {

        ProcessMigrationBatchEntityManager batchManager = CommandContextUtil.getProcessMigrationBatchEntityManager(commandContext);
        ProcessMigrationBatchEntity batch = batchManager.findById(batchId);
        if (fetchResources) {
            batch.getMigrationDocumentJson();
            batch.getResult();
            if (batch.getBatchChildren() != null) {
                batch.getBatchChildren().forEach(child -> child.getResult());
            }
        }
        return batch;
    }

}
