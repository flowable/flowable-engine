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

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationResult;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessInstanceMigrationJobHandler extends AbstractProcessInstanceMigrationJobHandler {

    public static final String TYPE = "process-migration";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        BatchService batchService = processEngineConfiguration.getBatchServiceConfiguration().getBatchService();
        ProcessInstanceMigrationManager processInstanceMigrationManager = processEngineConfiguration.getProcessInstanceMigrationManager();

        String batchPartId = getBatchPartIdFromHandlerCfg(configuration);
        BatchPart batchPart = batchService.getBatchPart(batchPartId);
        Batch batch = batchService.getBatch(batchPart.getBatchId());
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromJson(batch.getBatchDocumentJson(processEngineConfiguration.getEngineCfgKey()));

        String exceptionMessage = null;
        try {
            processInstanceMigrationManager.migrateProcessInstance(batchPart.getScopeId(), migrationDocument, commandContext);
        } catch (FlowableException e) {
            exceptionMessage = e.getMessage();
        }

        String resultAsJsonString = prepareResultAsJsonString(exceptionMessage);
        
        if (exceptionMessage != null) {
            batchService.completeBatchPart(batchPartId, ProcessInstanceBatchMigrationResult.RESULT_FAIL, resultAsJsonString);
        } else {
            batchService.completeBatchPart(batchPartId, ProcessInstanceBatchMigrationResult.RESULT_SUCCESS, resultAsJsonString);
        }
    }

    protected static String prepareResultAsJsonString(String exceptionMessage) {
        ObjectNode objectNode = getObjectMapper().createObjectNode();
        if (exceptionMessage == null) {
            objectNode.put(BATCH_RESULT_STATUS_LABEL, ProcessInstanceBatchMigrationResult.RESULT_SUCCESS);
        } else {
            objectNode.put(BATCH_RESULT_STATUS_LABEL, ProcessInstanceBatchMigrationResult.RESULT_FAIL);
            objectNode.put(BATCH_RESULT_MESSAGE_LABEL, exceptionMessage);
        }
        return objectNode.toString();
    }

}