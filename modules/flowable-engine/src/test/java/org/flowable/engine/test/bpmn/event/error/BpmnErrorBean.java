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

package org.flowable.engine.test.bpmn.event.error;

import java.io.Serializable;
import java.util.concurrent.Future;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Falko Menge
 */
public class BpmnErrorBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public void throwBpmnError() {
        throw new BpmnError("23", "This is a business fault, which can be caught by a BPMN Error Event.");
    }

    public Future<?> throwBpmnErrorInFuture() {
        return CommandContextUtil.getProcessEngineConfiguration()
                .getAsyncTaskInvoker()
                .submit(() -> {
                    throw new BpmnError("23", "This is a business fault, which can be caught by a BPMN Error Event.");
                });
    }

    public void throwComplexBpmnError(String errorCode, String errorMessage, String... additionalData) {
        BpmnError error = new BpmnError(errorCode, errorMessage);
        if (additionalData.length > 1) {
            for (int i = 1; i < additionalData.length; i+=2) {
                String key = additionalData[i - 1];
                String value = additionalData[i];
                error.addAdditionalData(key, value);
            }
        }
        throw error;
    }

    public JavaDelegate getDelegate() {
        return new ThrowBpmnErrorDelegate();
    }
}