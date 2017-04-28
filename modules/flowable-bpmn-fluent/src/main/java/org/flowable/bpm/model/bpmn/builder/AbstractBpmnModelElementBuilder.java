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
package org.flowable.bpm.model.bpmn.builder;

import org.flowable.bpm.model.bpmn.BpmnModelException;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.flowable.bpm.model.bpmn.instance.SubProcess;
import org.flowable.bpm.model.bpmn.instance.Transaction;

public abstract class AbstractBpmnModelElementBuilder<B extends AbstractBpmnModelElementBuilder<B, E>, E extends BpmnModelElementInstance> {

    protected final BpmnModelInstance modelInstance;
    protected final E element;
    protected final B myself;

    @SuppressWarnings("unchecked")
    protected AbstractBpmnModelElementBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
        this.modelInstance = modelInstance;
        myself = (B) selfType.cast(this);
        this.element = element;
    }

    /**
     * Finishes the process building.
     *
     * @return the model instance with the build process
     */
    public BpmnModelInstance done() {
        return modelInstance;
    }

    /**
     * Finishes the building of an embedded sub-process.
     *
     * @return the parent sub-process builder
     * @throws BpmnModelException if no parent sub-process can be found
     */
    public SubProcessBuilder subProcessDone() {
        BpmnModelElementInstance lastSubProcess = element.getScope();
        if (lastSubProcess instanceof SubProcess) {
            return ((SubProcess) lastSubProcess).builder();
        } else {
            throw new BpmnModelException("Unable to find a parent subProcess.");
        }
    }

    public TransactionBuilder transactionDone() {
        BpmnModelElementInstance lastTransaction = element.getScope();
        if (lastTransaction instanceof Transaction) {
            return new TransactionBuilder(modelInstance, (Transaction) lastTransaction);
        } else {
            throw new BpmnModelException("Unable to find a parent transaction.");
        }
    }

    public E getElement() {
        return element;
    }
}
