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

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * Implements the plan item instance builder API.
 *
 * @author Micha Kiener
 */
public class PlanItemInstanceEntityBuilderImpl implements PlanItemInstanceEntityBuilder {

    protected final PlanItemInstanceEntityManagerImpl planItemInstanceEntityManager;
    protected PlanItem planItem;
    protected String name;
    protected String caseDefinitionId;
    protected String derivedCaseDefinitionId;
    protected String caseInstanceId;
    protected PlanItemInstance stagePlanItemInstance;
    protected String tenantId;
    protected Map<String, Object> localVariables;
    protected boolean addToParent;
    protected boolean silentNameExpressionEvaluation;

    public PlanItemInstanceEntityBuilderImpl(PlanItemInstanceEntityManagerImpl planItemInstanceEntityManager) {
        this.planItemInstanceEntityManager = planItemInstanceEntityManager;
    }

    @Override
    public PlanItemInstanceEntityBuilder planItem(PlanItem planItem) {
        this.planItem = planItem;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder name(String name) {
        this.name = name;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder caseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder derivedCaseDefinitionId(String derivedCaseDefinitionId) {
        this.derivedCaseDefinitionId = derivedCaseDefinitionId;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder caseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder stagePlanItemInstance(PlanItemInstance stagePlanItemInstance) {
        this.stagePlanItemInstance = stagePlanItemInstance;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder localVariables(Map<String, Object> localVariables) {
        this.localVariables = localVariables;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder addToParent(boolean addToParent) {
        this.addToParent = addToParent;
        return this;
    }
    @Override
    public PlanItemInstanceEntityBuilder silentNameExpressionEvaluation(boolean silentNameExpressionEvaluation) {
        this.silentNameExpressionEvaluation = silentNameExpressionEvaluation;
        return this;
    }
    @Override
    public PlanItemInstanceEntity create() {
        validateData();
        return planItemInstanceEntityManager.createChildPlanItemInstance(this);
    }

    public PlanItem getPlanItem() {
        return planItem;
    }
    public String getName() {
        return name;
    }
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    public String getDerivedCaseDefinitionId() {
        return derivedCaseDefinitionId;
    }
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    public PlanItemInstance getStagePlanItemInstance() {
        return stagePlanItemInstance;
    }
    public String getTenantId() {
        return tenantId;
    }
    public Map<String, Object> getLocalVariables() {
        return localVariables;
    }
    public boolean hasLocalVariables() {
        return localVariables != null && localVariables.size() > 0;
    }
    public boolean isAddToParent() {
        return addToParent;
    }
    public boolean isSilentNameExpressionEvaluation() {
        return silentNameExpressionEvaluation;
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
