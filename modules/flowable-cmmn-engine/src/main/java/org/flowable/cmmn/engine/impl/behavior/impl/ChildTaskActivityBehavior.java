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
package org.flowable.cmmn.engine.impl.behavior.impl;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class ChildTaskActivityBehavior extends CoreCmmnTriggerableActivityBehavior {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChildTaskActivityBehavior.class);

    protected boolean isBlocking;
    protected String isBlockingExpression;
    protected List<IOParameter> inParameters;
    protected List<IOParameter> outParameters;

    public ChildTaskActivityBehavior(boolean isBlocking, String isBlockingExpression) {
        this.isBlocking = isBlocking;
        this.isBlockingExpression = isBlockingExpression;
    }

    public ChildTaskActivityBehavior(boolean isBlocking, String isBlockingExpression, List<IOParameter> inParameters, List<IOParameter> outParameters) {
        this(isBlocking, isBlockingExpression);
        this.inParameters = inParameters;
        this.outParameters = outParameters;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        execute(commandContext, planItemInstanceEntity, null);
    }

    public abstract void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, Map<String, Object> variables);

    protected boolean evaluateIsBlocking(DelegatePlanItemInstance planItemInstance) {
        boolean blocking = isBlocking;
        if (StringUtils.isNotEmpty(isBlockingExpression)) {
            Expression expression = CommandContextUtil.getExpressionManager().createExpression(isBlockingExpression);
            blocking = (Boolean) expression.getValue(planItemInstance);
        }
        return blocking;
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableException("Can only trigger a plan item that is in the ACTIVE state");
        }
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstance);
    }

    protected void handleInParameters(PlanItemInstanceEntity planItemInstanceEntity,
                                      CmmnEngineConfiguration cmmnEngineConfiguration, Map<String, Object> inParametersMap,
                                      ExpressionManager expressionManager) {

        if (inParameters == null) {
            return;
        }

        for (IOParameter inParameter : inParameters) {

            String variableName = null;
            if (StringUtils.isNotEmpty(inParameter.getTargetExpression())) {
                Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(inParameter.getTargetExpression());
                Object variableNameValue = expression.getValue(planItemInstanceEntity);
                if (variableNameValue != null) {
                    variableName = variableNameValue.toString();
                } else {
                    LOGGER.warn("In parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                            inParameter.getTargetExpression());
                }

            } else if (StringUtils.isNotEmpty(inParameter.getTarget())) {
                variableName = inParameter.getTarget();

            }

            Object variableValue = null;
            if (StringUtils.isNotEmpty(inParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(inParameter.getSourceExpression());
                variableValue = expression.getValue(planItemInstanceEntity);

            } else if (StringUtils.isNotEmpty(inParameter.getSource())) {
                variableValue = planItemInstanceEntity.getVariable(inParameter.getSource());

            }

            if (variableName != null) {
                inParametersMap.put(variableName, variableValue);
            }

        }
    }

    /**
     * Called when a manual delete is triggered (NOT when a terminate/complete is triggered),
     * for example when a deployment is deleted and everything related needs to be deleted.
     */
    public abstract void deleteChildEntity(CommandContext commandContext, DelegatePlanItemInstance delegatePlanItemInstance, boolean cascade);

}
