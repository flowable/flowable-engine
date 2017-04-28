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

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.TransactionMethod;
import org.flowable.bpm.model.bpmn.instance.Transaction;

public class AbstractTransactionBuilder<B extends AbstractTransactionBuilder<B>>
        extends AbstractSubProcessBuilder<B> {

    protected AbstractTransactionBuilder(BpmnModelInstance modelInstance, Transaction element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    public B method(TransactionMethod method) {
        ((Transaction) element).setMethod(method);
        return myself;
    }


}
