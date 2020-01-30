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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.common.engine.impl.util.ReflectUtil;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnClassDelegateFactory implements CmmnClassDelegateFactory {

    protected ExpressionManager expressionManager;

    public DefaultCmmnClassDelegateFactory(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

    @Override
    public CmmnClassDelegate create(String className, List<FieldExtension> fieldExtensions) {
        return new CmmnClassDelegate(className, fieldExtensions);
    }

    @Override
    public CmmnClassDelegate createLifeCycleListener(String className, String sourceState, String targetState, List<FieldExtension> fieldExtensions) {
        CmmnClassDelegate cmmnClassDelegate = create(className, fieldExtensions);
        cmmnClassDelegate.setSourceState(sourceState);
        cmmnClassDelegate.setTargetState(targetState);
        return cmmnClassDelegate;
    }

    @Override
    public Object defaultInstantiateDelegate(Class<?> clazz, ServiceTask serviceTask, boolean allExpressions) {
        Object object = ReflectUtil.instantiate(clazz.getName());
        if (serviceTask != null) {
            for (FieldExtension extension : serviceTask.getFieldExtensions()) {
                Object value;
                if (StringUtils.isNotEmpty(extension.getExpression())) {
                    value = expressionManager.createExpression(extension.getExpression());
                } else if (allExpressions) {
                    value = new FixedValue(extension.getStringValue());
                } else {
                    value = extension.getStringValue();
                }
                ReflectUtil.invokeSetterOrField(object, extension.getFieldName(), value, false);
            }

            ReflectUtil.invokeSetterOrField(object, "serviceTask", serviceTask, false);
        }

        return object;
    }

}
