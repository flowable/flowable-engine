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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormRepositoryService;

/**
 * @author Tijs Rademakers
 */
public class GetFormDefinitionsForCaseDefinitionCmd implements Command<List<FormDefinition>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String caseDefinitionId;
    protected FormRepositoryService formRepositoryService;

    public GetFormDefinitionsForCaseDefinitionCmd(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @Override
    public List<FormDefinition> execute(CommandContext commandContext) {
        CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseDefinitionId);
        
        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("Cannot find case definition for id: " + caseDefinitionId, CaseDefinition.class);
        }
        
        Case caseModel = CaseDefinitionUtil.getCase(caseDefinitionId);

        if (caseModel == null) {
            throw new FlowableObjectNotFoundException("Cannot find case definition for id: " + caseDefinitionId, Case.class);
        }

        formRepositoryService = CommandContextUtil.getFormEngineConfiguration(commandContext).getFormRepositoryService();

        if (formRepositoryService == null) {
            throw new FlowableException("Form repository service is not available");
        }

        List<FormDefinition> formDefinitions = getFormDefinitionsFromModel(caseModel, caseDefinition);

        return formDefinitions;
    }

    protected List<FormDefinition> getFormDefinitionsFromModel(Case caseModel, CaseDefinition caseDefinition) {
        Set<String> formKeys = new HashSet<>();
        List<FormDefinition> formDefinitions = new ArrayList<>();

        // for all user tasks
        List<HumanTask> humanTasks = caseModel.getPlanModel().findPlanItemDefinitionsOfType(HumanTask.class, true);
        
        for (HumanTask humanTask : humanTasks) {
            if (StringUtils.isNotEmpty(humanTask.getFormKey())) {
                formKeys.add(humanTask.getFormKey());
            }
        }

        for (String formKey : formKeys) {
            addFormDefinitionToCollection(formDefinitions, formKey, caseDefinition);
        }

        return formDefinitions;
    }

    protected void addFormDefinitionToCollection(List<FormDefinition> formDefinitions, String formKey, CaseDefinition caseDefinition) {
        FormDefinitionQuery formDefinitionQuery = formRepositoryService.createFormDefinitionQuery();
        FormDefinition formDefinition = formDefinitionQuery.formDefinitionKey(formKey).parentDeploymentId(caseDefinition.getDeploymentId()).singleResult();

        if (formDefinition != null) {
            formDefinitions.add(formDefinition);
        }
    }
}
