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
package org.flowable.cmmn.converter;

/**
 * @author Joram Barrez
 */
public interface CmmnXmlConstants {
    
    String CMMN_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/MODEL";
    String FLOWABLE_EXTENSIONS_NAMESPACE = "http://flowable.org/cmmn";
    String FLOWABLE_EXTENSIONS_PREFIX = "flowable";
    String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    String XSI_PREFIX = "xsi";
    String CMMNDI_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/CMMNDI";
    String CMMNDI_PREFIX = "cmmndi";
    String OMGDC_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/DC";
    String OMGDC_PREFIX = "dc";
    String OMGDI_NAMESPACE = "http://www.omg.org/spec/CMMN/20151109/DI";
    String OMGDI_PREFIX = "di";
    String CASE_NAMESPACE = "http://www.flowable.org/test";
    
    String ATTRIBUTE_TARGET_NAMESPACE = "targetNamespace";
    String ATTRIBUTE_EXPRESSION_LANGUAGE = "expressionLanguage";
    String ATTRIBUTE_EXPORTER = "exporter";
    String ATTRIBUTE_EXPORTER_VERSION = "exporterVersion";
    String ATTRIBUTE_AUTHOR = "author";
    String ATTRIBUTE_CREATION_DATE = "creationDate";

    String ELEMENT_DEFINITIONS = "definitions";
    String ELEMENT_DOCUMENTATION = "documentation";
    String ELEMENT_CASE = "case";
    String ELEMENT_PLAN_MODEL = "casePlanModel";
    String ELEMENT_STAGE = "stage";    
    String ELEMENT_MILESTONE = "milestone";
    String ELEMENT_TASK = "task";
    String ELEMENT_HUMAN_TASK = "humanTask";
    String ELEMENT_CASE_TASK = "caseTask";
    String ELEMENT_PROCESS_TASK = "processTask";
    String ELEMENT_PLAN_ITEM = "planItem";
    String ELEMENT_SENTRY = "sentry";
    String ELEMENT_PLAN_ITEM_ON_PART = "planItemOnPart";
    String ELEMENT_STANDARD_EVENT = "standardEvent";
    String ELEMENT_ENTRY_CRITERION = "entryCriterion";
    String ELEMENT_EXIT_CRITERION = "exitCriterion";
    String ELEMENT_EXTENSIONS = "extensions";
    
    String ELEMENT_PROCESS = "process";
    String ATTRIBUTE_IMPLEMENTATION_TYPE = "implementationType";
    String ATTRIBUTE_EXTERNAL_REF = "externalRef";
    
    String ATTRIBUTE_ID = "id";
    String ATTRIBUTE_NAME = "name";
    String ATTRIBUTE_TEXT_FORMAT = "textFormat";
    String ATTRIBUTE_DEFINITION_REF = "definitionRef";
    String ATTRIBUTE_SOURCE_REF = "sourceRef";
    String ATTRIBUTE_SENTRY_REF = "sentryRef";
    String ATTRIBUTE_IS_BLOCKING = "isBlocking";
    
    String ATTRIBUTE_CASE_REF = "caseRef";
    String ATTRIBUTE_PROCESS_REF = "processRef";
    String ATTRIBUTE_PROCESS_REF_EXPRESSION = "processRefExpression";
    
    String ATTRIBUTE_CLASS = "class";
    
    String ELEMENT_DI_CMMN = "CMMNDI";
    String ELEMENT_DI_DIAGRAM = "CMMNDiagram";
    String ELEMENT_DI_SHAPE = "CMMNShape";
    String ELEMENT_DI_EDGE = "CMMNEdge";
    String ELEMENT_DI_LABEL = "CMMNLabel";
    String ELEMENT_DI_BOUNDS = "Bounds";
    String ELEMENT_DI_WAYPOINT = "waypoint";
    String ATTRIBUTE_DI_CMMN_ELEMENT_REF = "cmmnElementRef";
    String ATTRIBUTE_DI_TARGET_CMMN_ELEMENT_REF = "targetCMMNElementRef";
    String ATTRIBUTE_DI_WIDTH = "width";
    String ATTRIBUTE_DI_HEIGHT = "height";
    String ATTRIBUTE_DI_X = "x";
    String ATTRIBUTE_DI_Y = "y";
    
}
