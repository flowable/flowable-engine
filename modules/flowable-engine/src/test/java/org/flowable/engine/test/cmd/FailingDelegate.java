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
package org.flowable.engine.test.cmd;

import java.util.function.Supplier;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author Saeid Mirzaei
 * @author Filip Hrisafov
 */
public class FailingDelegate implements JavaDelegate {

    public static final String EXCEPTION_MESSAGE = "Expected exception.";
    protected static final Supplier<RuntimeException> ORIGNAL_SUPPLIER = () -> new RuntimeException(EXCEPTION_MESSAGE);
    protected static Supplier<RuntimeException> exceptionSupplier = ORIGNAL_SUPPLIER;

    @Override
    public void execute(DelegateExecution execution) {
        Boolean fail = (Boolean) execution.getVariable("fail");

        if (fail == null || fail) {
            throw exceptionSupplier.get();
        }

    }

    public static void setExceptionSupplier(Supplier<RuntimeException> exceptionSupplier) {
        FailingDelegate.exceptionSupplier = exceptionSupplier;
    }

    public static void resetExceptionSupplier() {
        FailingDelegate.exceptionSupplier = ORIGNAL_SUPPLIER;
    }
}
