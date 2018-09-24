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
package org.flowable.cmmn.engine.impl.delegate;

import java.util.List;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemJavaDelegateActivityBehavior;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public class CmmnClassDelegate implements CmmnActivityBehavior {

    protected String className;
    protected List<FieldExtension> fieldExtensions;
    protected CmmnActivityBehavior activityBehaviorInstance;

    public CmmnClassDelegate(String className, List<FieldExtension> fieldExtensions) {
        this.className = className;
        this.fieldExtensions = fieldExtensions;
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getCmmnActivityBehavior(className, planItemInstance);
        }
        activityBehaviorInstance.execute(planItemInstance);
    }

    protected CmmnActivityBehavior getCmmnActivityBehavior(String className, VariableScope variableScope) {
        Object instance = instantiate(className);
        applyFieldExtensions(fieldExtensions, instance, variableScope, false);

        if (instance instanceof PlanItemJavaDelegate) {
            return new PlanItemJavaDelegateActivityBehavior((PlanItemJavaDelegate) instance);

        } else if (instance instanceof CmmnActivityBehavior) {
            return (CmmnActivityBehavior) instance;

        } else {
            throw new FlowableIllegalArgumentException(className + " does not implement the "
                    + CmmnActivityBehavior.class + " nor the " + PlanItemJavaDelegate.class + " interface");

        }
    }

    protected Object instantiate(String className) {
        return ReflectUtil.instantiate(className);
    }

    public static void applyFieldExtensions(List<FieldExtension> fieldExtensions, Object target, VariableContainer variableContainer, boolean throwExceptionOnMissingField) {
        if (fieldExtensions != null) {
            for (FieldExtension fieldExtension : fieldExtensions) {
                applyFieldExtension(fieldExtension, target, variableContainer, throwExceptionOnMissingField);
            }
        }
    }

    protected static void applyFieldExtension(FieldExtension fieldExtension, Object target, VariableContainer variableContainer, boolean throwExceptionOnMissingField) {
        Object value = null;
        if (fieldExtension.getStringValue() != null) {
            value = fieldExtension.getStringValue();
        } else if (fieldExtension.getExpression() != null) {
            ExpressionManager expressionManager = CommandContextUtil.getCmmnEngineConfiguration().getExpressionManager();
            value = expressionManager.createExpression(fieldExtension.getExpression());
        }

        ReflectUtil.invokeSetterOrField(target, fieldExtension.getFieldName(), value, throwExceptionOnMissingField);
    }

}
