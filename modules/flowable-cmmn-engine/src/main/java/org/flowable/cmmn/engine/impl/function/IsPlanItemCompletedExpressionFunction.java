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
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.el.CmmnVariableScopeELResolver;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstFunction;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstParameters;
import org.flowable.common.engine.impl.el.FlowableAstFunctionCreator;
import org.flowable.common.engine.impl.el.FlowableExpressionParser;

/**
 * This function evaluates a plan item to be completed, which is most likely used on a plan item with a repetition rule to check, whether it has alreday
 * been completed before.
 *
 * @author Micha Kiener
 */
public class IsPlanItemCompletedExpressionFunction implements FlowableFunctionDelegate, FlowableAstFunctionCreator {

    @Override
    public String prefix() {
        return "cmmn";
    }

    @Override
    public String localName() {
        return "isPlanItemCompleted";
    }

    @Override
    public Method functionMethod() {
        try {
            return IsPlanItemCompletedExpressionFunction.class.getMethod("isPlanItemCompleted", Object.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find isPlanItemCompleted function", e);
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

    public static boolean isPlanItemCompleted(Object object) {
        if (object instanceof PlanItemInstanceEntity planItemInstanceEntity) {
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstanceEntity.getCaseInstanceId());
            List<PlanItemInstanceEntity> planItemInstances = caseInstanceEntity.getChildPlanItemInstances();
            if (planItemInstances == null) {
                return false;
            }

            for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
                if (PlanItemInstanceState.COMPLETED.equals(planItemInstance.getState()) &&
                    planItemInstance.getPlanItemDefinitionId().equals(planItemInstanceEntity.getPlanItemDefinitionId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
