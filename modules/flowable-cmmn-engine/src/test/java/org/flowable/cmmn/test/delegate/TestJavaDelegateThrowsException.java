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
package org.flowable.cmmn.test.delegate;

import java.util.function.Supplier;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;

/**
 * @author Tijs Rademakers
 */
public class TestJavaDelegateThrowsException implements PlanItemJavaDelegate {

    protected static final Supplier<RuntimeException> ORIGNAL_SUPPLIER = () -> new RuntimeException("original exception");
    protected static Supplier<RuntimeException> exceptionSupplier = ORIGNAL_SUPPLIER;

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        throw exceptionSupplier.get();
    }

    public static void setExceptionSupplier(Supplier<RuntimeException> exceptionSupplier) {
        TestJavaDelegateThrowsException.exceptionSupplier = exceptionSupplier;
    }

    public static void resetExceptionSupplier() {
        TestJavaDelegateThrowsException.exceptionSupplier = ORIGNAL_SUPPLIER;
    }
}
