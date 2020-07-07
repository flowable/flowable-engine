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
import org.flowable.engine.delegate.FlowableFutureJavaDelegate;

/**
 * @author Filip Hrisafov
 */
public class ThrowCustomExceptionFutureDelegate implements FlowableFutureJavaDelegate<Object, String> {

    @Override
    public Object prepareExecutionData(DelegateExecution execution) {
        String throwErrorIn = (String) execution.getVariable("throwErrorIn");
        if ("beforeExecution".equals(throwErrorIn)) {
            throwException(execution.getVariable("exceptionClass"));
        } else if ("execute".equals(throwErrorIn)) {
            return execution.getVariable("exceptionClass");
        }
        return null;
    }

    @Override
    public String execute(Object inputData) {
        throwException(inputData);
        return null;
    }

    @Override
    public void afterExecution(DelegateExecution execution, String executionData) {
        String throwErrorIn = (String) execution.getVariable("throwErrorIn");

        if ("afterExecution".equals(throwErrorIn)) {
            throwException(execution.getVariable("exceptionClass"));
        }
    }

    protected void throwException(Object exceptionClass) {
        if (exceptionClass == null) {
            return;
        }

        String exceptionClassName = exceptionClass.toString();
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
