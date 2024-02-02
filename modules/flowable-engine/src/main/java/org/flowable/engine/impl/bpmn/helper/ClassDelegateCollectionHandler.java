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
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.delegate.FlowableCollectionHandler;

/**
 * Helper class for Collection handlers to allow class delegation.
 * 
 * This class will lazily instantiate the referenced classes when needed at runtime.
 * 
 * @author Lori Small
 */
public class ClassDelegateCollectionHandler extends AbstractClassDelegate implements FlowableCollectionHandler {

    private static final long serialVersionUID = 1L;

    public ClassDelegateCollectionHandler(String className, List<FieldDeclaration> fieldDeclarations) {
        super(className, fieldDeclarations);
    }
    
    public ClassDelegateCollectionHandler(Class<?> clazz, List<FieldDeclaration> fieldDeclarations) {
        super(clazz, fieldDeclarations);
    }

	@Override
	@SuppressWarnings("rawtypes")
	public Collection resolveCollection(Object collectionValue, DelegateExecution execution) {
		return getCollectionHandlerInstance().resolveCollection(collectionValue, execution);
	}

    protected FlowableCollectionHandler getCollectionHandlerInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof FlowableCollectionHandler) {
            return (FlowableCollectionHandler) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + FlowableCollectionHandler.class);
        }
    }
}
