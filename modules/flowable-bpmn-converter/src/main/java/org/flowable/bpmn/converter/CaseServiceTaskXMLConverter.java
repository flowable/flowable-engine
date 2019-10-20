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
package org.flowable.bpmn.converter;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.child.InParameterParser;
import org.flowable.bpmn.converter.child.OutParameterParser;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.ServiceTask;

/**
 * @author Tijs Rademakers
 */
public class CaseServiceTaskXMLConverter extends ServiceTaskXMLConverter {
    
    protected Map<String, BaseChildElementParser> childParserMap = new HashMap<>();
    
    public CaseServiceTaskXMLConverter() {
        InParameterParser inParameterParser = new InParameterParser();
        childParserMap.put(inParameterParser.getElementName(), inParameterParser);
        OutParameterParser outParameterParser = new OutParameterParser();
        childParserMap.put(outParameterParser.getElementName(), outParameterParser);
    }

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return CaseServiceTask.class;
    }

    @Override
    protected void convertCaseServiceTaskXMLProperties(CaseServiceTask caseServiceTask, BpmnModel bpmnModel, XMLStreamReader xtr) throws Exception {
        String caseDefinitionKey = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CASE_TASK_CASE_DEFINITION_KEY, xtr);
        if (StringUtils.isNotEmpty(caseDefinitionKey)) {
            caseServiceTask.setCaseDefinitionKey(caseDefinitionKey);
        }
        
        String caseInstanceName = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CASE_TASK_CASE_INSTANCE_NAME, xtr);
        if (StringUtils.isNotEmpty(caseInstanceName)) {
            caseServiceTask.setCaseInstanceName(caseInstanceName);
        }
        
        caseServiceTask.setBusinessKey(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_BUSINESS_KEY, xtr));
        caseServiceTask.setInheritBusinessKey(Boolean.parseBoolean(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_INHERIT_BUSINESS_KEY, xtr)));
        caseServiceTask.setSameDeployment(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_SAME_DEPLOYMENT, xtr)));
        caseServiceTask.setFallbackToDefaultTenant(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT, xtr)));
        caseServiceTask.setCaseInstanceIdVariableName(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ID_VARIABLE_NAME, xtr));
        parseChildElements(getXMLElementName(), caseServiceTask, childParserMap, bpmnModel, xtr);
    }
    
    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        CaseServiceTask caseServiceTask = (CaseServiceTask) element;
        writeQualifiedAttribute(ATTRIBUTE_TYPE, ServiceTask.CASE_TASK, xtw);
        
        if (StringUtils.isNotEmpty(caseServiceTask.getSkipExpression())) {
            writeQualifiedAttribute(ATTRIBUTE_TASK_SERVICE_SKIP_EXPRESSION, caseServiceTask.getSkipExpression(), xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getCaseDefinitionKey())) {
            writeQualifiedAttribute(ATTRIBUTE_CASE_TASK_CASE_DEFINITION_KEY, caseServiceTask.getCaseDefinitionKey(), xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getCaseInstanceName())) {
            writeQualifiedAttribute(ATTRIBUTE_CASE_TASK_CASE_INSTANCE_NAME, caseServiceTask.getCaseInstanceName(), xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getBusinessKey())) {
            writeQualifiedAttribute(ATTRIBUTE_BUSINESS_KEY, caseServiceTask.getBusinessKey(), xtw);
        }
        if (caseServiceTask.isInheritBusinessKey()) {
            writeQualifiedAttribute(ATTRIBUTE_INHERIT_BUSINESS_KEY, "true", xtw);
        }
        if (caseServiceTask.isSameDeployment()) {
            writeQualifiedAttribute(ATTRIBUTE_SAME_DEPLOYMENT, "true", xtw);
        }
        if (caseServiceTask.isFallbackToDefaultTenant()) {
            writeQualifiedAttribute(ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT, "true", xtw);
        }
        if (StringUtils.isNotEmpty(caseServiceTask.getCaseInstanceIdVariableName())) {
            writeQualifiedAttribute(ATTRIBUTE_ID_VARIABLE_NAME, caseServiceTask.getCaseInstanceIdVariableName(), xtw);
        }
    }

    @Override
    protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        CaseServiceTask caseServiceTask = (CaseServiceTask) element;
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_IN_PARAMETERS, caseServiceTask.getInParameters(), didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_OUT_PARAMETERS, caseServiceTask.getOutParameters(), didWriteExtensionStartElement, xtw);
        return didWriteExtensionStartElement;
    }

    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    }
}
