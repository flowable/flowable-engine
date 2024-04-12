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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.FlowableBatchPartMigrationException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
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

        try {
            processInstanceMigrationManager.migrateProcessInstance(batchPart.getScopeId(), migrationDocument, commandContext);
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            
            processEngineConfiguration.getCommandExecutor().execute(new Command<>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    CommandConfig commandConfig = processEngineConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                    return processEngineConfiguration.getCommandExecutor().execute(commandConfig, new Command<>() {
                        @Override
                        public Void execute(CommandContext commandContext2) {
                            String resultAsJsonString = prepareResultAsJsonString(exceptionMessage, e);
                            batchService.completeBatchPart(batchPartId, ProcessInstanceBatchMigrationResult.RESULT_FAIL, resultAsJsonString);

                            return null;
                        }
                    });
                }
            });
            
            FlowableBatchPartMigrationException wrappedException = new FlowableBatchPartMigrationException(e.getMessage(), e);
            wrappedException.setIgnoreFailedJob(true);
            throw wrappedException;
        }
        
        String resultAsJsonString = prepareResultAsJsonString();
        batchService.completeBatchPart(batchPartId, ProcessInstanceBatchMigrationResult.RESULT_SUCCESS, resultAsJsonString);
    }

    protected String prepareResultAsJsonString(String exceptionMessage, Exception e) {
        ObjectNode objectNode = getObjectMapper().createObjectNode();
        objectNode.put(BATCH_RESULT_STATUS_LABEL, ProcessInstanceBatchMigrationResult.RESULT_FAIL);
        objectNode.put(BATCH_RESULT_MESSAGE_LABEL, exceptionMessage);
        objectNode.put(BATCH_RESULT_STACKTRACE_LABEL, getExceptionStacktrace(e));
        return objectNode.toString();
    }
    
    protected String prepareResultAsJsonString() {
        ObjectNode objectNode = getObjectMapper().createObjectNode();
        objectNode.put(BATCH_RESULT_STATUS_LABEL, ProcessInstanceBatchMigrationResult.RESULT_SUCCESS);
        return objectNode.toString();
    }

    protected String getExceptionStacktrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}