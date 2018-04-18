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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class StartCaseInstanceWithFormCmd implements Command<CaseInstance>, Serializable {
    
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(StartCaseInstanceWithFormCmd.class);

    protected CaseInstanceBuilder caseInstanceBuilder;

    public StartCaseInstanceWithFormCmd(CaseInstanceBuilder caseInstanceBuilder) {
        this.caseInstanceBuilder = caseInstanceBuilder;
    }

    @Override
    public CaseInstance execute(CommandContext commandContext) {
        if (caseInstanceBuilder == null) {
            throw new FlowableIllegalArgumentException("Cannot start case instance: no case instance builder provided");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CmmnDeploymentManager deploymentCache = cmmnEngineConfiguration.getDeploymentManager();

        // Find the process definition
        CaseDefinition caseDefinition = deploymentCache.findDeployedCaseDefinitionById(caseInstanceBuilder.getCaseDefinitionId());
        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("No case definition found for id = '" + caseInstanceBuilder.getCaseDefinitionId() + "'", CaseDefinition.class);
        }

        FormInfo formInfo = null;
        Map<String, Object> formVariables = null;
        FormService formService = CommandContextUtil.getFormService();

        if (caseInstanceBuilder.getVariables() != null || caseInstanceBuilder.getOutcome() != null) {
            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());
            Case caze = cmmnModel.getCaseById(caseDefinition.getKey());
            Stage planModel = caze.getPlanModel();
            if (planModel != null && StringUtils.isNotEmpty(planModel.getFormKey())) {
                FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
                if (formRepositoryService != null) {
                    formInfo = formRepositoryService.getFormModelByKey(planModel.getFormKey());
                    if (formInfo != null) {
                        formVariables = formService.getVariablesFromFormSubmission(formInfo, caseInstanceBuilder.getVariables(),
                                caseInstanceBuilder.getOutcome());
                    }
                } else {
                    LOGGER.warn("Requesting form model {} without configured formRepositoryService", planModel.getFormKey());
                }
            }
        }


        CaseInstance caseInstance = cmmnEngineConfiguration.getCaseInstanceHelper().startCaseInstance(caseInstanceBuilder);

        if (formInfo != null) {
            formService.createFormInstanceWithScopeId(formVariables, formInfo, null, caseInstance.getId(),
                    ScopeTypes.CMMN, caseInstance.getCaseDefinitionId());
            FormFieldHandler formFieldHandler = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getFormFieldHandler();
            formFieldHandler.handleFormFieldsOnSubmit(formInfo, null, null, caseInstance.getId(), ScopeTypes.CMMN, caseInstanceBuilder.getVariables());
        }

        return caseInstance;
    }

}