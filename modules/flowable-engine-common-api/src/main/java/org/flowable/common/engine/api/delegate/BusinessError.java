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
package org.flowable.common.engine.api.delegate;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Abstract base class for business errors that can be thrown from user code (delegates, expressions, scripts)
 * and are caught by the engine's error handling mechanisms (BPMN boundary error events, CMMN fault sentries).
 *
 * This is distinct from technical errors ({@link FlowableException} and its other subclasses) which indicate
 * programming errors or infrastructure failures. Business errors represent expected exceptional conditions
 * in the business process/case logic.
 *
 * Subclasses: {@code BpmnError} (BPMN engine) and {@code CmmnFault} (CMMN engine).
 *
 * @author Joram Barrez
 */
public abstract class BusinessError extends FlowableException {

    private static final long serialVersionUID = 1L;

    protected String errorCode;
    protected VariableContainer additionalDataContainer;

    protected BusinessError(String errorCode) {
        super("");
        setErrorCode(errorCode);
    }

    protected BusinessError(String errorCode, String message) {
        super(message);
        setErrorCode(errorCode);
    }

    protected BusinessError(String errorCode, String message, Throwable cause) {
        super(message, cause);
        setErrorCode(errorCode);
    }

    protected void setErrorCode(String errorCode) {
        if (errorCode == null) {
            throw new FlowableIllegalArgumentException("Error code must not be null.");
        }
        if (errorCode.isEmpty()) {
            throw new FlowableIllegalArgumentException("Error code must not be empty.");
        }
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public VariableContainer getAdditionalDataContainer() {
        return additionalDataContainer;
    }

    public void setAdditionalDataContainer(VariableContainer additionalDataContainer) {
        this.additionalDataContainer = additionalDataContainer;
    }

    public abstract void addAdditionalData(String name, Object value);
}
