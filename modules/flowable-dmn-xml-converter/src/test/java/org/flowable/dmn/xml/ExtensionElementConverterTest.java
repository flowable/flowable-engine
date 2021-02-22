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
package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnExtensionAttribute;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.jupiter.api.Test;

public class ExtensionElementConverterTest extends AbstractConverterTest {

    protected static final String YOURCO_EXTENSIONS_NAMESPACE = "http://yourco/bpmn";
    protected static final String YOURCO_EXTENSIONS_PREFIX = "yourco";

    protected static final String ELEMENT_ATTRIBUTES = "attributes";
    protected static final String ELEMENT_ATTRIBUTE = "attribute";
    protected static final String ATTRIBUTE_NAME = "name";
    protected static final String ATTRIBUTE_VALUE = "value";

    protected static final String ELEMENT_I18LN_LOCALIZATION = "i18ln";
    protected static final String ATTRIBUTE_RESOURCE_BUNDLE_KEY_FOR_NAME = "resourceBundleKeyForName";
    protected static final String ATTRIBUTE_RESOURCE_BUNDLE_KEY_FOR_DESCRIPTION = "resourceBundleKeyForDescription";
    protected static final String ATTRIBUTE_LABELED_ENTITY_ID_FOR_NAME = "labeledEntityIdForName";
    protected static final String ATTRIBUTE_LABELED_ENTITY_ID_FOR_DESCRIPTION = "labeledEntityIdForDescription";

    @Test
    public void convertXMLToModel() throws Exception {
        DmnDefinition definition = readXMLFile();
        validateModel(definition);
    }

    @Test
    public void convertModelToXML() throws Exception {
        DmnDefinition bpmnModel = readXMLFile();
        DmnDefinition parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "extensionElements.dmn";
    }

    private void validateModel(DmnDefinition model) {
        assertThat(model.getDescription()).isEqualTo("DMN description");

        /*
         * Verify attributes extension
         */
        Map<String, String> attributes = getAttributes(model);
        assertThat(attributes)
                .containsOnly(
                        entry("Attr3", "3"),
                        entry("Attr4", "4")
                );

        /*
         * Verify localization extension
         */
        Localization localization = getLocalization(model);
        assertThat(localization.getResourceBundleKeyForName()).isEqualTo("rbkfn-2");
        assertThat(localization.getResourceBundleKeyForDescription()).isEqualTo("rbkfd-2");
        assertThat(localization.getLabeledEntityIdForName()).isEqualTo("leifn-2");
        assertThat(localization.getLabeledEntityIdForDescription()).isEqualTo("leifd-2");

        List<Decision> decisions = model.getDecisions();
        assertThat(decisions).hasSize(1);

        DecisionTable decisionTable = (DecisionTable) decisions.get(0).getExpression();
        assertThat(decisionTable).isNotNull();

        assertThat(decisionTable.getDescription()).isEqualTo("Decision table description");

        /*
         * Verify decision table localization extension
         */
        localization = getLocalization(decisionTable);
        assertThat(localization.getResourceBundleKeyForName()).isEqualTo("rbkfn-3");
        assertThat(localization.getResourceBundleKeyForDescription()).isEqualTo("rbkfd-3");
        assertThat(localization.getLabeledEntityIdForName()).isEqualTo("leifn-3");
        assertThat(localization.getLabeledEntityIdForDescription()).isEqualTo("leifd-3");

        attributes = getAttributes(decisionTable);
        assertThat(attributes)
                .containsOnly(
                        entry("Attr5", "5"),
                        entry("Attr6", "6")
                );

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertThat(inputClauses).hasSize(4);

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertThat(outputClauses).hasSize(1);

        /*
         * Verify input entry extension elements
         */
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(3).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("NONE OF");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(3).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("20, 13");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(3).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("ANY OF");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(3).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("\"20\", \"13\"");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(3).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("ALL OF");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(3).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("20");
    }

    protected Map<String, String> getAttributes(DmnElement bObj) {
        Map<String, String> attributes = null;

        if (null != bObj) {
            List<DmnExtensionElement> attributesExtension = bObj.getExtensionElements().get(ELEMENT_ATTRIBUTES);

            if (null != attributesExtension && !attributesExtension.isEmpty()) {
                attributes = new HashMap<>();
                List<DmnExtensionElement> attributeExtensions = attributesExtension.get(0).getChildElements().get(ELEMENT_ATTRIBUTE);

                for (DmnExtensionElement attributeExtension : attributeExtensions) {
                    attributes.put(attributeExtension.getAttributeValue(YOURCO_EXTENSIONS_NAMESPACE, ATTRIBUTE_NAME),
                            attributeExtension.getAttributeValue(YOURCO_EXTENSIONS_NAMESPACE, ATTRIBUTE_VALUE));
                }
            }
        }
        return attributes;
    }

    protected Localization getLocalization(DmnElement bObj) {
        Localization localization = new Localization();
        List<DmnExtensionElement> i18lnExtension = bObj.getExtensionElements().get(ELEMENT_I18LN_LOCALIZATION);

        if (!i18lnExtension.isEmpty()) {
            Map<String, List<DmnExtensionAttribute>> extensionAttributes = i18lnExtension.get(0).getAttributes();
            localization.setLabeledEntityIdForName(extensionAttributes.get(ATTRIBUTE_LABELED_ENTITY_ID_FOR_NAME)
                    .get(0).getValue());
            localization.setLabeledEntityIdForDescription(extensionAttributes.get(ATTRIBUTE_LABELED_ENTITY_ID_FOR_DESCRIPTION)
                    .get(0).getValue());
            localization.setResourceBundleKeyForName(extensionAttributes.get(ATTRIBUTE_RESOURCE_BUNDLE_KEY_FOR_NAME)
                    .get(0).getValue());
            localization.setResourceBundleKeyForDescription(extensionAttributes.get(ATTRIBUTE_RESOURCE_BUNDLE_KEY_FOR_DESCRIPTION)
                    .get(0).getValue());
        }
        return localization;
    }

    /*
     * Inner class used to hold localization DataObject extension values
     */
    public class Localization {

        private String resourceBundleKeyForName;
        private String resourceBundleKeyForDescription;
        private String labeledEntityIdForName;
        private String labeledEntityIdForDescription;

        public String getResourceBundleKeyForName() {
            return resourceBundleKeyForName;
        }

        public void setResourceBundleKeyForName(String resourceBundleKeyForName) {
            this.resourceBundleKeyForName = resourceBundleKeyForName;
        }

        public String getResourceBundleKeyForDescription() {
            return resourceBundleKeyForDescription;
        }

        public void setResourceBundleKeyForDescription(String resourceBundleKeyForDescription) {
            this.resourceBundleKeyForDescription = resourceBundleKeyForDescription;
        }

        public String getLabeledEntityIdForName() {
            return labeledEntityIdForName;
        }

        public void setLabeledEntityIdForName(String labeledEntityIdForName) {
            this.labeledEntityIdForName = labeledEntityIdForName;
        }

        public String getLabeledEntityIdForDescription() {
            return labeledEntityIdForDescription;
        }

        public void setLabeledEntityIdForDescription(String labeledEntityIdForDescription) {
            this.labeledEntityIdForDescription = labeledEntityIdForDescription;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(100);
            sb.append("Localization: [");
            sb.append("resourceBundleKeyForName=").append(resourceBundleKeyForName);
            sb.append(", resourceBundleKeyForDescription=").append(resourceBundleKeyForDescription);
            sb.append(", labeledEntityIdForName=").append(labeledEntityIdForName);
            sb.append(", labeledEntityIdForDescription=").append(labeledEntityIdForDescription);
            sb.append("]");
            return sb.toString();
        }
    }
    /*
     * End of inner classes
     */
}
