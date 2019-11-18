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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.Map;

import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * Implements the plan item instance builder API.
 *
 * @author Micha Kiener
 */
public class PlanItemInstanceBuilderImpl  implements PlanItemInstanceBuilder {

    protected final PlanItemInstanceEntityManagerImpl planItemInstanceEntityManager;
    protected PlanItem planItem;
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stagePlanItemInstanceId;
    protected String tenantId;
    protected Map<String, Object> localVariables;
    protected boolean addToParent;
    protected boolean silentNameExpressionEvaluation;

    public PlanItemInstanceBuilderImpl(PlanItemInstanceEntityManagerImpl planItemInstanceEntityManager) {
        this.planItemInstanceEntityManager = planItemInstanceEntityManager;
    }

    @Override
    public PlanItemInstanceBuilder planItem(PlanItem planItem) {
        this.planItem = planItem;
        return this;
    }
    @Override
    public PlanItemInstanceBuilder caseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }
    @Override
    public PlanItemInstanceBuilder caseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }
    @Override
    public PlanItemInstanceBuilder stagePlanItemInstanceId(String stagePlanItemInstanceId) {
        this.stagePlanItemInstanceId = stagePlanItemInstanceId;
        return this;
    }
    @Override
    public PlanItemInstanceBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    @Override
    public PlanItemInstanceBuilder localVariables(Map<String, Object> localVariables) {
        this.localVariables = localVariables;
        return this;
    }
    @Override
    public PlanItemInstanceBuilder addToParent(boolean addToParent) {
        this.addToParent = addToParent;
        return this;
    }
    @Override
    public PlanItemInstanceBuilder silentNameExpressionEvaluation(boolean silentNameExpressionEvaluation) {
        this.silentNameExpressionEvaluation = silentNameExpressionEvaluation;
        return this;
    }
    @Override
    public PlanItemInstanceEntity create() {
        validateData();
        return planItemInstanceEntityManager.createChildPlanItemInstance(planItem, caseDefinitionId, caseInstanceId, stagePlanItemInstanceId, tenantId,
            localVariables, addToParent, silentNameExpressionEvaluation);
    }

    protected void validateData() {
        if (planItem == null) {
            throw new FlowableIllegalArgumentException("The plan item must be provided when creating a new plan item instance");
        }
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("The case definition id must be provided when creating a new plan item instance");
        }
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("The case instance id must be provided when creating a new plan item instance");
        }
    }
}
