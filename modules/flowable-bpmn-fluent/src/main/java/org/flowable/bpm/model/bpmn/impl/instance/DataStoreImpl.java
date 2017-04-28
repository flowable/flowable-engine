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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CAPACITY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IS_UNLIMITED;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ITEM_SUBJECT_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_DATA_STORE;

import org.flowable.bpm.model.bpmn.instance.DataState;
import org.flowable.bpm.model.bpmn.instance.DataStore;
import org.flowable.bpm.model.bpmn.instance.ItemDefinition;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN dataStore element.
 */
public class DataStoreImpl
        extends RootElementImpl
        implements DataStore {

    protected static Attribute<String> nameAttribute;
    protected static Attribute<Integer> capacityAttribute;
    protected static Attribute<Boolean> isUnlimitedAttribute;
    protected static AttributeReference<ItemDefinition> itemSubjectRefAttribute;
    protected static ChildElement<DataState> dataStateChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(DataStore.class, BPMN_ELEMENT_DATA_STORE)
                .namespaceUri(BPMN20_NS)
                .extendsType(RootElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<DataStore>() {
                    @Override
                    public DataStore newInstance(ModelTypeInstanceContext instanceContext) {
                        return new DataStoreImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        capacityAttribute = typeBuilder.integerAttribute(BPMN_ATTRIBUTE_CAPACITY)
                .build();

        isUnlimitedAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_UNLIMITED)
                .defaultValue(true)
                .build();

        itemSubjectRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ITEM_SUBJECT_REF)
                .qNameAttributeReference(ItemDefinition.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        dataStateChild = sequenceBuilder.element(DataState.class)
                .build();

        typeBuilder.build();
    }

    public DataStoreImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getName() {
        return nameAttribute.getValue(this);
    }

    @Override
    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    @Override
    public Integer getCapacity() {
        return capacityAttribute.getValue(this);
    }

    @Override
    public void setCapacity(Integer capacity) {
        capacityAttribute.setValue(this, capacity);
    }

    @Override
    public Boolean isUnlimited() {
        return isUnlimitedAttribute.getValue(this);
    }

    @Override
    public void setUnlimited(Boolean isUnlimited) {
        isUnlimitedAttribute.setValue(this, isUnlimited);
    }

    @Override
    public ItemDefinition getItemSubject() {
        return itemSubjectRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setItemSubject(ItemDefinition itemSubject) {
        itemSubjectRefAttribute.setReferenceTargetElement(this, itemSubject);
    }

    @Override
    public DataState getDataState() {
        return dataStateChild.getChild(this);
    }

    @Override
    public void setDataState(DataState dataState) {
        dataStateChild.setChild(this, dataState);
    }

}
