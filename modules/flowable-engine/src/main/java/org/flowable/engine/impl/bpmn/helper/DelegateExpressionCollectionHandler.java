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
package org.flowable.engine.impl.bpmn.helper;

import java.util.Collection;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.FlowableCollectionHandler;
import org.flowable.variable.service.delegate.Expression;

/**
 * @author Lori Small
 */
public class DelegateExpressionCollectionHandler implements FlowableCollectionHandler {
    
	private static final long serialVersionUID = 1L;

	protected Expression expression;

    public DelegateExpressionCollectionHandler(Expression expression) {
        this.expression = expression;
    }

	@Override
	@SuppressWarnings("rawtypes")
	public Collection resolveCollection(DelegateExecution execution, String collectionString) {
		return getCollectionHandlerInstance(execution).resolveCollection(execution, collectionString);
	}

    protected FlowableCollectionHandler getCollectionHandlerInstance(DelegateExecution execution) {
        Object delegateInstance = DelegateExpressionUtil.resolveDelegateExpression(expression, execution);
        if (delegateInstance instanceof FlowableCollectionHandler) {
            return (FlowableCollectionHandler) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + FlowableCollectionHandler.class);
        }
    }
}
