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
package org.flowable.test.spring.executor.jms.delegate;

import java.util.Random;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class RandomDelegate implements JavaDelegate {

    private static Random random = new Random();

    @Override
    public void execute(DelegateExecution delegateExecution) {
        Number number1 = (Number) delegateExecution.getVariable("input1");
        Number number2 = (Number) delegateExecution.getVariable("input2");
        int result = number1.intValue() + number2.intValue();
        delegateExecution.setVariable("result_" + random.nextInt(), "result is " + result);
    }

}
