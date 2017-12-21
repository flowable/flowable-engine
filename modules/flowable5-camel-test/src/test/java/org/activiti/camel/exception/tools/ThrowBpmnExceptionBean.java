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
package org.activiti.camel.exception.tools;

import org.apache.camel.Handler;
import org.flowable.engine.delegate.BpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThrowBpmnExceptionBean {

    public enum ExceptionType {
        NO_EXCEPTION, BPMN_EXCEPTION, NON_BPMN_EXCEPTION
    }

    protected static ExceptionType exceptionType;

    public static ExceptionType getExceptionType() {
        return exceptionType;
    }

    public static void setExceptionType(ExceptionType exceptionType) {
        ThrowBpmnExceptionBean.exceptionType = exceptionType;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ThrowBpmnExceptionBean.class);

    @Handler
    public void throwNonBpmnException() throws Exception {
        LOGGER.debug("throwing non bpmn bug");

        switch (getExceptionType()) {
        case NO_EXCEPTION:
            break;
        case NON_BPMN_EXCEPTION:
            throw new Exception("arbitrary non bpmn exception");
        case BPMN_EXCEPTION:
            throw new BpmnError("testError");
        }
    }
}
