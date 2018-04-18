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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDecisionTableQuery;
import org.flowable.dmn.api.DmnRepositoryService;

/**
 * @author Tijs Rademakers
 */
public class GetDecisionTablesForCaseDefinitionCmd implements Command<List<DmnDecisionTable>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String caseDefinitionId;
    protected DmnRepositoryService dmnRepositoryService;

    public GetDecisionTablesForCaseDefinitionCmd(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @Override
    public List<DmnDecisionTable> execute(CommandContext commandContext) {
        CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseDefinitionId);
        
        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("Cannot find case definition for id: " + caseDefinitionId, CaseDefinition.class);
        }
        
        Case caseModel = CaseDefinitionUtil.getCase(caseDefinitionId);

        if (caseModel == null) {
            throw new FlowableObjectNotFoundException("Cannot find case definition for id: " + caseDefinitionId, Case.class);
        }

        dmnRepositoryService = CommandContextUtil.getDmnEngineConfiguration(commandContext).getDmnRepositoryService();
        if (dmnRepositoryService == null) {
            throw new FlowableException("DMN repository service is not available");
        }

        List<DmnDecisionTable> decisionTables = getDecisionTablesFromModel(caseModel, caseDefinition);

        return decisionTables;
    }

    protected List<DmnDecisionTable> getDecisionTablesFromModel(Case caseModel, CaseDefinition caseDefinition) {
        Set<String> decisionTableKeys = new HashSet<>();
        List<DmnDecisionTable> decisionTables = new ArrayList<>();
        List<DecisionTask> decisionTasks = caseModel.getPlanModel().findPlanItemDefinitionsOfType(DecisionTask.class, true);

        for (DecisionTask decisionTask : decisionTasks) {
            if (decisionTask.getFieldExtensions() != null && decisionTask.getFieldExtensions().size() > 0) {
                for (FieldExtension fieldExtension : decisionTask.getFieldExtensions()) {
                    if ("decisionTableReferenceKey".equals(fieldExtension.getFieldName())) {
                        String decisionTableReferenceKey = fieldExtension.getStringValue();
                        if (!decisionTableKeys.contains(decisionTableReferenceKey)) {
                            addDecisionTableToCollection(decisionTables, decisionTableReferenceKey, caseDefinition);
                            decisionTableKeys.add(decisionTableReferenceKey);
                        }
                        break;
                    }
                }
            }
        }

        return decisionTables;
    }

    protected void addDecisionTableToCollection(List<DmnDecisionTable> decisionTables, String decisionTableKey, CaseDefinition caseDefinition) {
        DmnDecisionTableQuery decisionTableQuery = dmnRepositoryService.createDecisionTableQuery();
        DmnDecisionTable decisionTable = decisionTableQuery.decisionTableKey(decisionTableKey).parentDeploymentId(caseDefinition.getDeploymentId()).singleResult();

        if (decisionTable != null) {
            decisionTables.add(decisionTable);
        }
    }
}
