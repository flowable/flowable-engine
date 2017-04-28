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
package org.flowable.bpm.model.bpmn.impl.instance;

import org.flowable.bpm.model.bpmn.BpmnModelException;
import org.flowable.bpm.model.bpmn.builder.AbstractBaseElementBuilder;
import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.flowable.bpm.model.bpmn.instance.SubProcess;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;

/**
 * Shared base class for all BPMN Model Elements. Provides implementation of the {@link BpmnModelElementInstance} interface.
 */
public abstract class BpmnModelElementInstanceImpl
        extends ModelElementInstanceImpl
        implements BpmnModelElementInstance {

    public BpmnModelElementInstanceImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @SuppressWarnings("rawtypes")
    public AbstractBaseElementBuilder builder() {
        throw new BpmnModelException("No builder implemented for " + this);
    }

    public boolean isScope() {
        return this instanceof org.flowable.bpm.model.bpmn.instance.Process || this instanceof SubProcess;
    }

    public BpmnModelElementInstance getScope() {
        BpmnModelElementInstance parentElement = (BpmnModelElementInstance) getParentElement();
        if (parentElement != null) {
            if (parentElement.isScope()) {
                return parentElement;
            } else {
                return parentElement.getScope();
            }
        } else {
            return null;
        }
    }
}
