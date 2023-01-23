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

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentImpl;
import org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CaseInstanceMigrationJobHandler extends AbstractCaseInstanceMigrationJobHandler {

    public static final String TYPE = "case-migration";

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
        CaseInstanceMigrationDocument migrationDocument = CaseInstanceMigrationDocumentImpl.fromJson(
                batch.getBatchDocumentJson(engineConfiguration.getEngineCfgKey()));

        String exceptionMessage = null;
        try {
            migrationManager.migrateCaseInstance(batchPart.getScopeId(), migrationDocument, commandContext);
        } catch (FlowableException e) {
            exceptionMessage = e.getMessage();
        }

        String resultAsJsonString = prepareResultAsJsonString(exceptionMessage);

        if (exceptionMessage != null) {
            batchService.completeBatchPart(batchPartId, CaseInstanceBatchMigrationResult.RESULT_FAIL, resultAsJsonString);
        } else {
            batchService.completeBatchPart(batchPartId, CaseInstanceBatchMigrationResult.RESULT_SUCCESS, resultAsJsonString);
        }
    }

    protected static String prepareResultAsJsonString(String exceptionMessage) {
        ObjectNode objectNode = getObjectMapper().createObjectNode();
        if (exceptionMessage == null) {
            objectNode.put(BATCH_RESULT_STATUS_LABEL, CaseInstanceBatchMigrationResult.RESULT_SUCCESS);
        } else {
            objectNode.put(BATCH_RESULT_STATUS_LABEL, CaseInstanceBatchMigrationResult.RESULT_FAIL);
            objectNode.put(BATCH_RESULT_MESSAGE_LABEL, exceptionMessage);
        }
        return objectNode.toString();
    }

}