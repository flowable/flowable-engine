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

import java.util.Date;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationDocumentImpl;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationJobHandler extends AbstractProcessInstanceMigrationJobHandler {

    public static final String TYPE = "process-migration";
    public static final String RESULT_LABEL_PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String RESULT_LABEL_MIGRATION_PROCESS = "migrationProcess";
    public static final String RESULT_LABEL_CAUSE = "cause";
    public static final String RESULT_VALUE_SUCCESSFUL = "successful";
    public static final String RESULT_VALUE_FAILED = "failed";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {

        ProcessMigrationBatchEntityManager processMigrationBatchEntityManager = CommandContextUtil.getProcessMigrationBatchEntityManager(commandContext);
        ProcessInstanceMigrationManager processInstanceMigrationManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationManager();
        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();

        String batchId = getBatchIdFromHandlerCfg(configuration);
        ProcessMigrationBatchEntity batchEntity = processMigrationBatchEntityManager.findById(batchId);

        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentImpl.fromProcessInstanceMigrationDocumentJson(batchEntity.getMigrationDocumentJson());

        String exceptionMessage = null;
        try {
            processInstanceMigrationManager.migrateProcessInstance(batchEntity.getProcessInstanceId(), migrationDocument, commandContext);
        } catch (FlowableException e) {
            exceptionMessage = e.getMessage();
        }

        String resultAsJsonString = getResultAsJsonString(batchEntity.getProcessInstanceId(), exceptionMessage, objectMapper);
        Date currentTime = CommandContextUtil.getProcessEngineConfiguration(commandContext).getClock().getCurrentTime();
        batchEntity.completeWithResult(currentTime, resultAsJsonString);
    }

    protected static String getResultAsJsonString(String processInstanceId, String exceptionMessage, ObjectMapper objectMapper) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put(RESULT_LABEL_PROCESS_INSTANCE_ID, processInstanceId);
        if (exceptionMessage == null) {
            objectNode.put(RESULT_LABEL_MIGRATION_PROCESS, RESULT_VALUE_SUCCESSFUL);
        } else {
            objectNode.put(RESULT_LABEL_MIGRATION_PROCESS, RESULT_VALUE_FAILED);
            objectNode.put(RESULT_LABEL_CAUSE, exceptionMessage);
        }
        return objectNode.toString();
    }

}

