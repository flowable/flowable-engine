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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_EXPORTER;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_EXPORTER_VERSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_EXPRESSION_LANGUAGE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ID;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TARGET_NAMESPACE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TYPE_LANGUAGE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_DEFINITIONS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.XML_SCHEMA_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.XPATH_NS;

import org.flowable.bpm.model.bpmn.instance.Definitions;
import org.flowable.bpm.model.bpmn.instance.Extension;
import org.flowable.bpm.model.bpmn.instance.Import;
import org.flowable.bpm.model.bpmn.instance.Relationship;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN definitions element.
 */
public class DefinitionsImpl
        extends BpmnModelElementInstanceImpl
        implements Definitions {

    protected static Attribute<String> idAttribute;
    protected static Attribute<String> nameAttribute;
    protected static Attribute<String> targetNamespaceAttribute;
    protected static Attribute<String> expressionLanguageAttribute;
    protected static Attribute<String> typeLanguageAttribute;
    protected static Attribute<String> exporterAttribute;
    protected static Attribute<String> exporterVersionAttribute;

    protected static ChildElementCollection<Import> importCollection;
    protected static ChildElementCollection<Extension> extensionCollection;
    protected static ChildElementCollection<RootElement> rootElementCollection;
    protected static ChildElementCollection<Relationship> relationshipCollection;

    public static void registerType(ModelBuilder bpmnModelBuilder) {

        ModelElementTypeBuilder typeBuilder = bpmnModelBuilder.defineType(Definitions.class, BPMN_ELEMENT_DEFINITIONS)
                .namespaceUri(BPMN20_NS)
                .instanceProvider(new ModelElementTypeBuilder.ModelTypeInstanceProvider<Definitions>() {
                    @Override
                    public Definitions newInstance(ModelTypeInstanceContext instanceContext) {
                        return new DefinitionsImpl(instanceContext);
                    }
                });

        idAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ID)
                .idAttribute()
                .build();

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        targetNamespaceAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TARGET_NAMESPACE)
                .required()
                .build();

        expressionLanguageAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_EXPRESSION_LANGUAGE)
                .defaultValue(XPATH_NS)
                .build();

        typeLanguageAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TYPE_LANGUAGE)
                .defaultValue(XML_SCHEMA_NS)
                .build();

        exporterAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_EXPORTER)
                .build();

        exporterVersionAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_EXPORTER_VERSION)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        importCollection = sequenceBuilder.elementCollection(Import.class)
                .build();

        extensionCollection = sequenceBuilder.elementCollection(Extension.class)
                .build();

        rootElementCollection = sequenceBuilder.elementCollection(RootElement.class)
                .build();

        relationshipCollection = sequenceBuilder.elementCollection(Relationship.class)
                .build();

        typeBuilder.build();
    }

    public DefinitionsImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getId() {
        return idAttribute.getValue(this);
    }

    @Override
    public void setId(String id) {
        idAttribute.setValue(this, id);
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
    public String getTargetNamespace() {
        return targetNamespaceAttribute.getValue(this);
    }

    @Override
    public void setTargetNamespace(String namespace) {
        targetNamespaceAttribute.setValue(this, namespace);
    }

    @Override
    public String getExpressionLanguage() {
        return expressionLanguageAttribute.getValue(this);
    }

    @Override
    public void setExpressionLanguage(String expressionLanguage) {
        expressionLanguageAttribute.setValue(this, expressionLanguage);
    }

    @Override
    public String getTypeLanguage() {
        return typeLanguageAttribute.getValue(this);
    }

    @Override
    public void setTypeLanguage(String typeLanguage) {
        typeLanguageAttribute.setValue(this, typeLanguage);
    }

    @Override
    public String getExporter() {
        return exporterAttribute.getValue(this);
    }

    @Override
    public void setExporter(String exporter) {
        exporterAttribute.setValue(this, exporter);
    }

    @Override
    public String getExporterVersion() {
        return exporterVersionAttribute.getValue(this);
    }

    @Override
    public void setExporterVersion(String exporterVersion) {
        exporterVersionAttribute.setValue(this, exporterVersion);
    }

    @Override
    public Collection<Import> getImports() {
        return importCollection.get(this);
    }

    @Override
    public Collection<Extension> getExtensions() {
        return extensionCollection.get(this);
    }

    @Override
    public Collection<RootElement> getRootElements() {
        return rootElementCollection.get(this);
    }

    @Override
    public Collection<Relationship> getRelationships() {
        return relationshipCollection.get(this);
    }
}
