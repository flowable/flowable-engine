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
package org.flowable.engine.impl.jobexecutor;

import java.util.Arrays;
import java.util.Date;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationValidationJobHandler implements JobHandler {

    public static final String TYPE = "migration-validation";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {

        ProcessMigrationBatchEntityManager processMigrationBatchEntityManager = CommandContextUtil.getProcessMigrationBatchEntityManager(commandContext);
        ProcessInstanceMigrationManager processInstanceMigrationManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationManager();

        String batchId = getBatchIdFromHandlerCfg(configuration);
        ProcessMigrationBatchEntity batchEntity = processMigrationBatchEntityManager.findById(batchId);

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(batchEntity.getMigrationDocumentJson());
        ProcessInstanceMigrationValidationResult validationResult = processInstanceMigrationManager.validateMigrateProcessInstance(batchEntity.getProcessInstanceId(), migrationDocument, commandContext);

        Date currentTime = CommandContextUtil.getProcessEngineConfiguration(commandContext).getClock().getCurrentTime();
        if (validationResult.hasErrors()) {
            String collatedValidationResult = Arrays.toString(validationResult.getValidationMessages().toArray(new String[0]));
            batchEntity.completeWithResult(currentTime, collatedValidationResult);
        } else {
            batchEntity.complete(currentTime);
        }
    }

    //TODO WIP - Review how to encode the processMigration batch id in the handlerCfg - JSON?
    public static String getBatchIdFromHandlerCfg(String handlerCfg) {
        if (handlerCfg != null) {
            String[] split = handlerCfg.split(":");
            if (split.length >= 2) {
                return split[1];
            }
        }
        return null;
    }

    public static String getHandlerCfgForBatchId(String batchId) {
        return "BatchId:" + batchId;
    }

}

