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
package org.flowable.cmmn.engine.impl.function;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.el.CmmnVariableScopeELResolver;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceContainerUtil;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstFunction;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstParameters;
import org.flowable.common.engine.impl.el.FlowableAstFunctionCreator;
import org.flowable.common.engine.impl.el.FlowableExpressionParser;

/**
 * This function evaluates a stage to be completable, which is the case, if all required and active plan items are completed
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class IsStageCompletableExpressionFunction implements FlowableFunctionDelegate, FlowableAstFunctionCreator {

    @Override
    public String prefix() {
        return "cmmn";
    }

    @Override
    public String localName() {
        return "isStageCompletable";
    }

    @Override
    public Method functionMethod() {
        try {
            return IsStageCompletableExpressionFunction.class.getMethod("isStageCompletable", Object.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find isStageCompletable function", e);
        }
    }

    @Override
    public Collection<String> getFunctionNames() {
        return Collections.singleton(prefix() + ":" + localName());
    }

    @Override
    public AstFunction createFunction(String name, int index, AstParameters parameters, boolean varargs, FlowableExpressionParser parser) {
        if (parameters.getCardinality() == 0) {
            // If there are no parameters then we need to add the plan item instance identifier
            AstParameters newParameters = new AstParameters(Collections.singletonList(parser.createIdentifier(CmmnVariableScopeELResolver.PLAN_ITEM_INSTANCE_KEY)));
            return new AstFunction(name, index, newParameters, varargs);
        }
        return new AstFunction(name, index, parameters, varargs);
    }

    public static boolean isStageCompletable(Object object) {
        if (object instanceof PlanItemInstanceEntity planItemInstanceEntity) {

            if (planItemInstanceEntity.isStage()) {
                return planItemInstanceEntity.isCompletable();

            } else if (planItemInstanceEntity.getStageInstanceId() != null) {
                PlanItemInstanceEntity stagePlanItemInstanceEntity = planItemInstanceEntity.getStagePlanItemInstanceEntity();

                // Special care needed for the event listeners with an available condition: a new evaluation needs to be done
                // as the completable only gets set at the end of the evaluation cycle.

                PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
                if (planItemDefinition instanceof EventListener
                        && (PlanItemInstanceState.AVAILABLE.equals(planItemInstanceEntity.getState()) || PlanItemInstanceState.UNAVAILABLE.equals(planItemInstanceEntity.getState()))
                        && (StringUtils.isNotEmpty(((EventListener) planItemDefinition).getAvailableConditionExpression()))) {

                    return PlanItemInstanceContainerUtil.shouldPlanItemContainerComplete(stagePlanItemInstanceEntity,
                        Collections.singletonList(planItemInstanceEntity.getId()), true).isCompletable();

                } else {
                    return stagePlanItemInstanceEntity.isCompletable();

                }

            } else {
                CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstanceEntity.getCaseInstanceId());
                return caseInstanceEntity.isCompletable();

            }

        } else if (object instanceof CaseInstanceEntity caseInstanceEntity) {
            return caseInstanceEntity.isCompletable();

        }
        return false;
    }

}
