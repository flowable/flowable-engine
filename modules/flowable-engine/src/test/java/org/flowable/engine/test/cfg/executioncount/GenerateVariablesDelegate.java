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
package org.flowable.engine.test.cfg.executioncount;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class GenerateVariablesDelegate implements JavaDelegate {

    private Expression numberOfVariablesString;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        int numberOfVariables = Integer.valueOf(numberOfVariablesString.getValue(delegateExecution).toString());
        for (int i = 0; i < numberOfVariables; i++) {
            if (i % 2 == 0) {
                delegateExecution.setVariable("var" + i, i); // integer
            } else {
                delegateExecution.setVariable("var" + i, String.valueOf(i)); // string
            }
        }
    }

}
