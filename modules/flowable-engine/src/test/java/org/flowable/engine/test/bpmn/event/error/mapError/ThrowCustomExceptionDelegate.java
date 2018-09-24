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
package org.flowable.engine.test.bpmn.event.error.mapError;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author Saeid Mirzaei
 */
public class ThrowCustomExceptionDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Object exceptionClassVar = execution.getVariable("exceptionClass");
        if (exceptionClassVar == null)
            return;

        String exceptionClassName = exceptionClassVar.toString();

        if (StringUtils.isNotEmpty(exceptionClassName)) {
            RuntimeException exception = null;
            try {
                Class<?> clazz = Class.forName(exceptionClassName);
                exception = (RuntimeException) clazz.newInstance();

            } catch (Exception e) {
                throw new FlowableException("Class not found", e);
            }
            throw exception;
        }
    }
}
