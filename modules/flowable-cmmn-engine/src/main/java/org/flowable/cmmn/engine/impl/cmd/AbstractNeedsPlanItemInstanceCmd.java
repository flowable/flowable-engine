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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormService;

/**
 * @author Joram Barrez
 */
public abstract class AbstractNeedsPlanItemInstanceCmd implements Command<Void>, Serializable {
    
    protected String planItemInstanceId;
    protected Map<String, Object> variables;
    protected Map<String, Object> formVariables;
    protected String formOutcome;
    protected FormInfo formInfo;
    protected Map<String, Object> localVariables;
    protected Map<String, Object> transientVariables;

    public AbstractNeedsPlanItemInstanceCmd(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    public AbstractNeedsPlanItemInstanceCmd(String planItemInstanceId, Map<String, Object> variables,
            Map<String, Object> formVariables, String formOutcome, FormInfo formInfo,
            Map<String, Object> localVariables, Map<String, Object> transientVariables) {
        
        this.planItemInstanceId = planItemInstanceId;
        this.variables = variables;
        this.formVariables = formVariables;
        this.formOutcome = formOutcome;
        this.formInfo = formInfo;
        this.localVariables = localVariables;
        this.transientVariables = transientVariables;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("Plan item instance id is null");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        PlanItemInstanceEntity planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(planItemInstanceId);
        if (planItemInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("Cannot find plan item instance for id " + planItemInstanceId, PlanItemInstanceEntity.class);
        }

        if (formInfo != null) {

            FormService formService = CommandContextUtil.getFormService(commandContext);
            if (formService == null) {
                throw new FlowableIllegalStateException("Form engine is not initialized");
            }

            Map<String, Object> variablesFromFormSubmission = formService.getVariablesFromFormSubmission(formInfo, formVariables, formOutcome);

            FormFieldHandler formFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
            formFieldHandler.handleFormFieldsOnSubmit(formInfo, null, null, planItemInstanceEntity.getCaseInstanceId(), ScopeTypes.CMMN, variablesFromFormSubmission,
                    planItemInstanceEntity.getTenantId());

            planItemInstanceEntity.setVariables(variablesFromFormSubmission);
        }

        if (variables != null) {
            planItemInstanceEntity.setVariables(variables);
        }

        if (localVariables != null) {
            planItemInstanceEntity.setVariablesLocal(localVariables);
        }

        if (transientVariables != null) {
            planItemInstanceEntity.setTransientVariables(transientVariables);
        }

        internalExecute(commandContext, planItemInstanceEntity);
        return null;
    }
    
    protected abstract void internalExecute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity);

    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }

    public void setPlanItemInstanceId(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }

    public void setTransientVariables(Map<String, Object> transientVariables) {
        this.transientVariables = transientVariables;
    }

}
