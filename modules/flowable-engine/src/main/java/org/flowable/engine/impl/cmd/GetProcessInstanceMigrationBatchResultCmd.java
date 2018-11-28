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

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessMigrationBatch;

/**
 * @author Dennis Federico
 */
public class GetProcessInstanceMigrationBatchResultCmd implements Command<List<String>> {

    protected String batchId;

    public GetProcessInstanceMigrationBatchResultCmd(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public List<String> execute(CommandContext commandContext) {

        ProcessMigrationBatchEntityManager batchManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessMigrationBatchEntityManager();
        ProcessMigrationBatchEntity batch = batchManager.findById(batchId);

        List<String> jsonResults = new ArrayList<>();

        if (batch.isCompleted()) {
            if (batch.getResult() != null) {
                jsonResults.add(batch.getResult());
            }

            if (batch.getBatchChildren() != null) {
                batch.getBatchChildren()
                    .stream()
                    .filter(child -> child.getResult() != null)
                    .map(ProcessMigrationBatch::getResult)
                    .forEach(jsonResults::add);
            }
            return jsonResults;
        }
        return null;
    }

}
