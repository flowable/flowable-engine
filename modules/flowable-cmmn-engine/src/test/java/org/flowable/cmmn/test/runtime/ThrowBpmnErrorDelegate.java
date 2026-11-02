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
package org.flowable.cmmn.test.runtime;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.common.engine.api.delegate.BusinessError;

/**
 * Test delegate that throws a non-CmmnFault BusinessError subclass from a CMMN plan item.
 * Simulates the scenario where user code throws a BpmnError (or any other BusinessError subclass)
 * from within a CMMN service task. Verifies that the BusinessError base class enables
 * cross-engine error handling.
 */
public class ThrowBpmnErrorDelegate implements PlanItemJavaDelegate {

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        throw new OtherBusinessError("FOREIGN_ERROR", "A non-CmmnFault BusinessError");
    }

    /**
     * A BusinessError subclass that is NOT CmmnFault — simulates BpmnError or any other engine's error.
     */
    static class OtherBusinessError extends BusinessError {
        OtherBusinessError(String errorCode, String message) {
            super(errorCode, message);
        }

        @Override
        public void addAdditionalData(String name, Object value) {
            // not needed for test
        }
    }
}
