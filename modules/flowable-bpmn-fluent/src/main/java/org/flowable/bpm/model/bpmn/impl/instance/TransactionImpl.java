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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_METHOD;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_TRANSACTION;

import org.flowable.bpm.model.bpmn.TransactionMethod;
import org.flowable.bpm.model.bpmn.instance.SubProcess;
import org.flowable.bpm.model.bpmn.instance.Transaction;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

public class TransactionImpl
        extends SubProcessImpl
        implements Transaction {

    protected static Attribute<TransactionMethod> methodAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Transaction.class, BPMN_ELEMENT_TRANSACTION)
                .namespaceUri(BPMN20_NS)
                .extendsType(SubProcess.class)
                .instanceProvider(new ModelTypeInstanceProvider<Transaction>() {
                    public Transaction newInstance(ModelTypeInstanceContext instanceContext) {
                        return new TransactionImpl(instanceContext);
                    }
                });

        methodAttribute = typeBuilder.namedEnumAttribute(BPMN_ATTRIBUTE_METHOD, TransactionMethod.class)
                .defaultValue(TransactionMethod.Compensate)
                .build();

        typeBuilder.build();
    }

    public TransactionImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public TransactionMethod getMethod() {
        return methodAttribute.getValue(this);
    }

    @Override
    public void setMethod(TransactionMethod method) {
        methodAttribute.setValue(this, method);
    }

}
