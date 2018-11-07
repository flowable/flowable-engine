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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.common.engine.impl.util.ReflectUtil;

import java.util.List;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnClassDelegateFactory implements CmmnClassDelegateFactory {

    @Override
    public CmmnClassDelegate create(String className, List<FieldExtension> fieldExtensions) {
        return new CmmnClassDelegate(className, fieldExtensions);
    }

    @Override
    public Object defaultInstantiateDelegate(Class<?> clazz, ServiceTask serviceTask) {
        return defaultInstantiateDelegate(clazz.getName(), serviceTask);
    }

    protected static Object defaultInstantiateDelegate(String className, ServiceTask serviceTask) {
        Object object = ReflectUtil.instantiate(className);
        for (FieldExtension extension : serviceTask.getFieldExtensions()) {
            String value;
            if (StringUtils.isEmpty(extension.getStringValue())) {
                value = extension.getExpression();
            } else {
                value = extension.getStringValue();
            }
            ReflectUtil.invokeSetterOrField(object, extension.getFieldName(), value, false);
        }

        if (serviceTask != null) {
            ReflectUtil.invokeSetterOrField(object, "serviceTask", serviceTask, false);
        }

        return object;
    }

}
