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
package org.flowable.cmmn.engine.delegate;

import java.util.HashMap;

import org.flowable.common.engine.api.delegate.BusinessError;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;

/**
 * Special exception that can be used to throw a CMMN Fault from {@link org.flowable.cmmn.api.delegate.PlanItemJavaDelegate}s, expressions, and scripts.
 *
 * This should only be used for business faults, which shall be handled by sentries with {@code standardEvent="fault"}.
 * Technical errors should be represented by other exception types.
 *
 * This is the CMMN equivalent of BPMN's {@code BpmnError}. When thrown during plan item execution,
 * it triggers the {@code fault} transition (Active → Failed) instead of propagating as an exception.
 *
 * @author Joram Barrez
 */
public class CmmnFault extends BusinessError {

    private static final long serialVersionUID = 1L;

    public CmmnFault(String faultCode) {
        super(faultCode);
    }

    public CmmnFault(String faultCode, String message) {
        super(faultCode, message);
    }

    /**
     * Returns the fault code. This is an alias for {@link #getErrorCode()}.
     */
    public String getFaultCode() {
        return getErrorCode();
    }

    @Override
    public void addAdditionalData(String name, Object value) {
        if (this.getAdditionalDataContainer() == null) {
            this.setAdditionalDataContainer(new VariableContainerWrapper(new HashMap<>()));
        }
        this.getAdditionalDataContainer().setVariable(name, value);
    }
}
