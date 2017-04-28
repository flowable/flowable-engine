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
import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
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
    protected static ChildElementCollection<BpmnDiagram> bpmnDiagramCollection;
    protected static ChildElementCollection<Relationship> relationshipCollection;

    public static void registerType(ModelBuilder bpmnModelBuilder) {

        ModelElementTypeBuilder typeBuilder = bpmnModelBuilder.defineType(Definitions.class, BPMN_ELEMENT_DEFINITIONS)
                .namespaceUri(BPMN20_NS)
                .instanceProvider(new ModelElementTypeBuilder.ModelTypeInstanceProvider<Definitions>() {
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

        bpmnDiagramCollection = sequenceBuilder.elementCollection(BpmnDiagram.class)
                .build();

        relationshipCollection = sequenceBuilder.elementCollection(Relationship.class)
                .build();

        typeBuilder.build();
    }

    public DefinitionsImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getId() {
        return idAttribute.getValue(this);
    }

    public void setId(String id) {
        idAttribute.setValue(this, id);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public String getTargetNamespace() {
        return targetNamespaceAttribute.getValue(this);
    }

    public void setTargetNamespace(String namespace) {
        targetNamespaceAttribute.setValue(this, namespace);
    }

    public String getExpressionLanguage() {
        return expressionLanguageAttribute.getValue(this);
    }

    public void setExpressionLanguage(String expressionLanguage) {
        expressionLanguageAttribute.setValue(this, expressionLanguage);
    }

    public String getTypeLanguage() {
        return typeLanguageAttribute.getValue(this);
    }

    public void setTypeLanguage(String typeLanguage) {
        typeLanguageAttribute.setValue(this, typeLanguage);
    }

    public String getExporter() {
        return exporterAttribute.getValue(this);
    }

    public void setExporter(String exporter) {
        exporterAttribute.setValue(this, exporter);
    }

    public String getExporterVersion() {
        return exporterVersionAttribute.getValue(this);
    }

    public void setExporterVersion(String exporterVersion) {
        exporterVersionAttribute.setValue(this, exporterVersion);
    }

    public Collection<Import> getImports() {
        return importCollection.get(this);
    }

    public Collection<Extension> getExtensions() {
        return extensionCollection.get(this);
    }

    public Collection<RootElement> getRootElements() {
        return rootElementCollection.get(this);
    }

    public Collection<BpmnDiagram> getBpmDiagrams() {
        return bpmnDiagramCollection.get(this);
    }

    public Collection<Relationship> getRelationships() {
        return relationshipCollection.get(this);
    }

}
