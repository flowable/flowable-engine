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
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.FormModel;
import org.flowable.variable.api.type.VariableScopeType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

/**
 * @author Joram Barrez
 */
public class StartCaseInstanceWithFormCmd implements Command<CaseInstance>, Serializable {

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

        FormModel formModel = null;
        Map<String, Object> formVariables = null;
        FormService formService = CommandContextUtil.getFormService();

        if (caseInstanceBuilder.getVariables() != null || caseInstanceBuilder.getOutcome() != null) {
            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());
            Case caze = cmmnModel.getCaseById(caseDefinition.getKey());
            Stage planModel = caze.getPlanModel();
            if (planModel != null && StringUtils.isNotEmpty(planModel.getFormKey())) {
                FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
                if (formRepositoryService != null) {
                    formModel = formRepositoryService.getFormModelByKey(planModel.getFormKey());
                    if (formModel != null) {
                        formVariables = formService.getVariablesFromFormSubmission(formModel, caseInstanceBuilder.getVariables(),
                                caseInstanceBuilder.getOutcome());
                    }
                } else {
                    LOGGER.warn("Requesting form model {} without configured formRepositoryService", planModel.getFormKey());
                }
            }
        }


        CaseInstance caseInstance = cmmnEngineConfiguration.getCaseInstanceHelper().startCaseInstance(caseInstanceBuilder);

        if (formModel != null) {
            formService.createFormInstanceWithScopeId(formVariables, formModel, null, caseInstance.getId(),
                    VariableScopeType.CMMN, caseInstance.getCaseDefinitionId());
            processUploadFieldsIfNeeded(formModel, caseInstance.getId());
        }

        return caseInstance;
    }

    /**
     * When content is uploaded for a field, it is uploaded as a 'temporary related content'. Now that the task is completed, we need to associate the field/taskId/processInstanceId with the related
     * content so we can retrieve it later.
     */
    protected void processUploadFieldsIfNeeded(FormModel formModel, String caseInstanceId) {
        ContentService contentService = CommandContextUtil.getContentService();
        if (contentService == null) {
            return;
        }

        Map<String, Object> variables = this.caseInstanceBuilder.getVariables();
        if (variables != null && formModel != null && formModel.getFields() != null) {
            for (FormField formField : formModel.getFields()) {
                if (FormFieldTypes.UPLOAD.equals(formField.getType())) {

                    String variableName = formField.getId();
                    if (variables.containsKey(variableName)) {
                        String variableValue = (String) variables.get(variableName);
                        if (StringUtils.isNotEmpty(variableValue)) {
                            String[] contentItemIds = StringUtils.split(variableValue, ",");
                            Set<String> contentItemIdSet = new HashSet<>();
                            Collections.addAll(contentItemIdSet, contentItemIds);

                            List<ContentItem> contentItems = contentService.createContentItemQuery().ids(contentItemIdSet).list();

                            for (ContentItem contentItem : contentItems) {
                                contentItem.setScopeId(caseInstanceId);
                                contentItem.setScopeType(VariableScopeType.CMMN);
                                contentItem.setField(formField.getId());
                                contentService.saveContentItem(contentItem);
                            }
                        }
                    }
                }
            }
        }
    }

}
