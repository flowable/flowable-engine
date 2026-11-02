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
package com.example.flowable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements JavaDelegate, PlanItemJavaDelegate {

    protected final ConcurrentHashMap<String, AtomicInteger> sends = new ConcurrentHashMap<>();

    protected AtomicInteger getSendCount(String key) {
        return this.sends.get(key);
    }

    @Override
    public void execute(DelegateExecution execution) {
        internalExecute(execution);
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        internalExecute(planItemInstance);
    }

    protected void internalExecute(VariableContainer variableContainer) {
        String customerId = (String) variableContainer.getVariable("customerId");
        String email = (String) variableContainer.getVariable("email");
        System.out.println("sending welcome email for " + customerId + " to " + email);
        sends.computeIfAbsent(email, e -> new AtomicInteger());
        sends.get(email).incrementAndGet();
    }

}
