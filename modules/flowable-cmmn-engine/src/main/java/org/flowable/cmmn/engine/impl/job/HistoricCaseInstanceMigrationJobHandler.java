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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationManager;
import org.flowable.cmmn.engine.impl.migration.HistoricCaseInstanceMigrationDocumentImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableBatchPartMigrationException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class HistoricCaseInstanceMigrationJobHandler extends AbstractCaseInstanceMigrationJobHandler {

    public static final String TYPE = "historic-case-migration";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();
        CaseInstanceMigrationManager migrationManager = engineConfiguration.getCaseInstanceMigrationManager();

        String batchPartId = getBatchPartIdFromHandlerCfg(configuration);
        BatchPart batchPart = batchService.getBatchPart(batchPartId);
        Batch batch = batchService.getBatch(batchPart.getBatchId());
        HistoricCaseInstanceMigrationDocument migrationDocument = HistoricCaseInstanceMigrationDocumentImpl.fromJson(
                batch.getBatchDocumentJson(engineConfiguration.getEngineCfgKey()));

        try {
            migrationManager.migrateHistoricCaseInstance(batchPart.getScopeId(), migrationDocument, commandContext);
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            
            engineConfiguration.getCommandExecutor().execute(new Command<>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    CommandConfig commandConfig = engineConfiguration.getCommandExecutor().getDefaultConfig().transactionRequiresNew();
                    return engineConfiguration.getCommandExecutor().execute(commandConfig, new Command<>() {
                        @Override
                        public Void execute(CommandContext commandContext2) {
                            String resultAsJsonString = prepareResultAsJsonString(exceptionMessage, e);
                            batchService.completeBatchPart(batchPartId, CaseInstanceBatchMigrationResult.RESULT_FAIL, resultAsJsonString);

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
        batchService.completeBatchPart(batchPartId, CaseInstanceBatchMigrationResult.RESULT_SUCCESS, resultAsJsonString);
    }

    protected String prepareResultAsJsonString(String exceptionMessage, Exception e) {
        ObjectNode objectNode = getObjectMapper().createObjectNode();
        objectNode.put(BATCH_RESULT_STATUS_LABEL, CaseInstanceBatchMigrationResult.RESULT_FAIL);
        objectNode.put(BATCH_RESULT_MESSAGE_LABEL, exceptionMessage);
        objectNode.put(BATCH_RESULT_STACKTRACE_LABEL, getExceptionStacktrace(e));
        return objectNode.toString();
    }
    
    protected String prepareResultAsJsonString() {
        ObjectNode objectNode = getObjectMapper().createObjectNode();
        objectNode.put(BATCH_RESULT_STATUS_LABEL, CaseInstanceBatchMigrationResult.RESULT_SUCCESS);
        return objectNode.toString();
    }

    protected String getExceptionStacktrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}