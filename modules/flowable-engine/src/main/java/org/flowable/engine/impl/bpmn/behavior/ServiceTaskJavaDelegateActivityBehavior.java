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

package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.delegate.invocation.JavaDelegateInvocation;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 */
public class ServiceTaskJavaDelegateActivityBehavior extends TaskActivityBehavior implements ActivityBehavior, ExecutionListener {

    private static final long serialVersionUID = 1L;

    protected JavaDelegate javaDelegate;
    protected Expression skipExpression;
    protected boolean triggerable;

    protected ServiceTaskJavaDelegateActivityBehavior() {
    }

    public ServiceTaskJavaDelegateActivityBehavior(JavaDelegate javaDelegate, boolean triggerable, Expression skipExpression) {
        this.javaDelegate = javaDelegate;
        this.triggerable = triggerable;
        this.skipExpression = skipExpression;
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        if (triggerable && javaDelegate instanceof TriggerableActivityBehavior) {
            ((TriggerableActivityBehavior) javaDelegate).trigger(execution, signalName, signalData);
            leave(execution);
        }
    }

    @Override
    public void execute(DelegateExecution execution) {
        boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(execution, skipExpression);
        if (!isSkipExpressionEnabled || (isSkipExpressionEnabled && !SkipExpressionUtil.shouldSkipFlowElement(execution, skipExpression))) {
            CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor()
                .handleInvocation(new JavaDelegateInvocation(javaDelegate, execution));
        }

        if (!triggerable) {
            leave(execution);
        }
    }

    @Override
    public void notify(DelegateExecution execution) {
        execute(execution);
    }
}
