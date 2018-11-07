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

package org.activiti.engine.impl.bpmn.behavior;

import org.activiti.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.delegate.JavaDelegateInvocation;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.delegate.ActivityBehavior;

/**
 * @author Tom Baeyens
 */
public class ServiceTaskJavaDelegateActivityBehavior extends TaskActivityBehavior implements ActivityBehavior, ExecutionListener {

    protected JavaDelegate javaDelegate;
    protected Expression skipExpression;

    protected ServiceTaskJavaDelegateActivityBehavior() {
    }

    public ServiceTaskJavaDelegateActivityBehavior(JavaDelegate javaDelegate, Expression skipExpression) {
        this.javaDelegate = javaDelegate;
        this.skipExpression = skipExpression;
    }

    @Override
    public void execute(DelegateExecution execution) {
        boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(execution, skipExpression);
        if (!isSkipExpressionEnabled ||
                (isSkipExpressionEnabled && !SkipExpressionUtil.shouldSkipFlowElement(execution, skipExpression))) {
            
            Context.getProcessEngineConfiguration()
                    .getDelegateInterceptor()
                    .handleInvocation(new JavaDelegateInvocation(javaDelegate, execution));
        }
        
        leave((ActivityExecution) execution);
    }

    @Override
    public void notify(DelegateExecution execution) {
        execute(execution);
    }
}
