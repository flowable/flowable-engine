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
package org.flowable.dmn.xml.constants;

/**
 * @author Tijs Rademakers
 */
public interface DmnXMLConstants {

    String DMN_NAMESPACE = "https://www.omg.org/spec/DMN/20191111/MODEL/";
    String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    String XSI_PREFIX = "xsi";
    String SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    String MODEL_NAMESPACE = "http://www.flowable.org/dmn";
    String TARGET_NAMESPACE_ATTRIBUTE = "targetNamespace";
    String ATTRIBUTE_EXPORTER = "exporter";
    String ATTRIBUTE_EXPORTER_VERSION = "exporterVersion";
    String FLOWABLE_EXTENSIONS_NAMESPACE = "http://flowable.org/dmn";
    String FLOWABLE_EXTENSIONS_PREFIX = "flowable";
    String DMNDI_NAMESPACE = "https://www.omg.org/spec/DMN/20191111/DMNDI/";
    String DMNDI_PREFIX = "dmndi";
    String OMGDC_NAMESPACE = "http://www.omg.org/spec/DMN/20180521/DC/";
    String OMGDC_PREFIX = "dc";
    String OMGDI_NAMESPACE = "http://www.omg.org/spec/DMN/20180521/DI/";
    String OMGDI_PREFIX = "di";

    String ATTRIBUTE_ID = "id";
    String ATTRIBUTE_NAME = "name";
    String ATTRIBUTE_LABEL = "label";
    String ATTRIBUTE_TYPE_REF = "typeRef";
    String ATTRIBUTE_HREF = "href";
    String ATTRIBUTE_HIT_POLICY = "hitPolicy";
    String ATTRIBUTE_NAMESPACE = "namespace";
    String ATTRIBUTE_AGGREGATION = "aggregation";
    String ATTRIBUTE_FORCE_DMN_11 = "forceDMN11";
    String ATTRIBUTE_IS_COLLECTION = "isCollection";

    String ELEMENT_DEFINITIONS = "definitions";
    String ELEMENT_DECISION = "decision";
    String ELEMENT_INPUT_DATA = "inputData";
    String ELEMENT_VARIABLE = "variable";
    String ELEMENT_DECISION_TABLE = "decisionTable";

    String ELEMENT_DECISION_SERVICE = "decisionService";
    String ELEMENT_OUTPUT_DECISION = "outputDecision";
    String ELEMENT_ENCAPSULATED_DECISION = "encapsulatedDecision";

    String ELEMENT_INFORMATION_REQUIREMENT = "informationRequirement";
    String ELEMENT_AUTHORITY_REQUIREMENT = "authorityRequirement";
    String ELEMENT_REQUIRED_DECISION = "requiredDecision";
    String ELEMENT_REQUIRED_INPUT = "requiredInput";
    String ELEMENT_REQUIRED_AUTHORITY = "requiredAuthority";

    String ELEMENT_ITEM_DEFINITION = "itemDefinition";
    String ELEMENT_ITEM_COMPONENT = "itemComponent";
    String ELEMENT_TYPE_REF = "typeRef";
    String ELEMENT_ALLOWED_VALUES = "allowedValues";

    String ELEMENT_INPUT_CLAUSE = "input";
    String ELEMENT_OUTPUT_CLAUSE = "output";
    String ELEMENT_INPUT_EXPRESSION = "inputExpression";
    String ELEMENT_INPUT_VALUES = "inputValues";
    String ELEMENT_OUTPUT_VALUES = "outputValues";
    String ELEMENT_TEXT = "text";

    String ELEMENT_RULE = "rule";
    String ELEMENT_INPUT_ENTRY = "inputEntry";
    String ELEMENT_OUTPUT_ENTRY = "outputEntry";

    String ELEMENT_DESCRIPTION = "description";
    String ELEMENT_EXTENSIONS = "extensionElements";

    String ELEMENT_DI_DMN = "DMNDI";
    String ELEMENT_DI_DIAGRAM = "DMNDiagram";
    String ELEMENT_DI_SHAPE = "DMNShape";
    String ELEMENT_DI_EDGE = "DMNEdge";
    String ELEMENT_DI_SIZE = "Size";
    String ELEMENT_DI_LABEL = "DMNLabel";
    String ELEMENT_DI_BOUNDS = "Bounds";
    String ELEMENT_DI_WAYPOINT = "waypoint";
    String ELEMENT_DI_DECISION_SERVICE_DIVIDER_LINE = "DMNDecisionServiceDividerLine";
    String ATTRIBUTE_DI_DMN_ELEMENT_REF = "dmnElementRef";
    String ATTRIBUTE_DI_TARGET_DMN_ELEMENT_REF = "targetDMNElementRef";
    String ATTRIBUTE_DI_WIDTH = "width";
    String ATTRIBUTE_DI_HEIGHT = "height";
    String ATTRIBUTE_DI_X = "x";
    String ATTRIBUTE_DI_Y = "y";
}
