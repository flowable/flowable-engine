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
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.child.InParameterParser;
import org.flowable.bpmn.converter.child.OutParameterParser;
import org.flowable.bpmn.converter.export.MapExceptionExport;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;

/**
 * @author Tijs Rademakers
 */
public class CallActivityXMLConverter extends BaseBpmnXMLConverter {

    protected Map<String, BaseChildElementParser> childParserMap = new HashMap<>();

    public CallActivityXMLConverter() {
        InParameterParser inParameterParser = new InParameterParser();
        childParserMap.put(inParameterParser.getElementName(), inParameterParser);
        OutParameterParser outParameterParser = new OutParameterParser();
        childParserMap.put(outParameterParser.getElementName(), outParameterParser);
    }

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return CallActivity.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_CALL_ACTIVITY;
    }

    @Override
    protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
        CallActivity callActivity = new CallActivity();
        BpmnXMLUtil.addXMLLocation(callActivity, xtr);
        callActivity.setCalledElement(xtr.getAttributeValue(null, ATTRIBUTE_CALL_ACTIVITY_CALLEDELEMENT));
        callActivity.setCalledElementType(BpmnXMLUtil.getAttributeValue(BpmnXMLConstants.ATTRIBUTE_CALL_ACTIVITY_CALLEDELEMENTTYPE, xtr));
        callActivity.setProcessInstanceName(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CALL_ACTIVITY_PROCESS_INSTANCE_NAME, xtr));
        callActivity.setBusinessKey(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_BUSINESS_KEY, xtr));
        callActivity.setInheritBusinessKey(Boolean.parseBoolean(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_INHERIT_BUSINESS_KEY, xtr)));
        callActivity.setInheritVariables(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CALL_ACTIVITY_INHERITVARIABLES, xtr)));
        callActivity.setSameDeployment(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_SAME_DEPLOYMENT, xtr)));
        callActivity.setUseLocalScopeForOutParameters(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CALL_ACTIVITY_USE_LOCALSCOPE_FOR_OUTPARAMETERS, xtr)));
        callActivity.setCompleteAsync(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_CALL_ACTIVITY_COMPLETE_ASYNC, xtr)));
        
        String fallbackToDefaultTenant = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT, xtr);
        if (StringUtils.isNotEmpty(fallbackToDefaultTenant)) {
            callActivity.setFallbackToDefaultTenant(Boolean.valueOf(fallbackToDefaultTenant));
        }

        String idVariableName = BpmnXMLUtil.getAttributeValue(BpmnXMLConstants.ATTRIBUTE_ID_VARIABLE_NAME, xtr);
        if (StringUtils.isNotEmpty(idVariableName)) {
            callActivity.setProcessInstanceIdVariableName(idVariableName);
        }
        
        parseChildElements(getXMLElementName(), callActivity, childParserMap, model, xtr);
        return callActivity;
    }

    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        CallActivity callActivity = (CallActivity) element;
        if (StringUtils.isNotEmpty(callActivity.getCalledElement())) {
            xtw.writeAttribute(ATTRIBUTE_CALL_ACTIVITY_CALLEDELEMENT, callActivity.getCalledElement());
        }
        if (StringUtils.isNotEmpty(callActivity.getCalledElementType())) {
            writeQualifiedAttribute(ATTRIBUTE_CALL_ACTIVITY_CALLEDELEMENTTYPE, callActivity.getCalledElementType(), xtw);
        }
        if (StringUtils.isNotEmpty(callActivity.getProcessInstanceName())) {
            writeQualifiedAttribute(ATTRIBUTE_CALL_ACTIVITY_PROCESS_INSTANCE_NAME, callActivity.getProcessInstanceName(), xtw);
        }
        if (StringUtils.isNotEmpty(callActivity.getBusinessKey())) {
            writeQualifiedAttribute(ATTRIBUTE_BUSINESS_KEY, callActivity.getBusinessKey(), xtw);
        }
        if (callActivity.isInheritBusinessKey()) {
            writeQualifiedAttribute(ATTRIBUTE_INHERIT_BUSINESS_KEY, "true", xtw);
        }
        if (callActivity.isUseLocalScopeForOutParameters()) {
            writeQualifiedAttribute(ATTRIBUTE_CALL_ACTIVITY_USE_LOCALSCOPE_FOR_OUTPARAMETERS, "true", xtw);
        }
        if (callActivity.isInheritVariables()) {
            writeQualifiedAttribute(ATTRIBUTE_CALL_ACTIVITY_INHERITVARIABLES, "true", xtw);
        }
        if (callActivity.isSameDeployment()) {
            writeQualifiedAttribute(ATTRIBUTE_SAME_DEPLOYMENT, "true", xtw);
        }
        if (callActivity.isCompleteAsync()) {
            writeQualifiedAttribute(ATTRIBUTE_CALL_ACTIVITY_COMPLETE_ASYNC, "true", xtw);
        }
        if (callActivity.getFallbackToDefaultTenant() != null) {
            writeQualifiedAttribute(ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT, callActivity.getFallbackToDefaultTenant().toString(), xtw);
        }
        if (callActivity.getProcessInstanceIdVariableName() != null) {
            writeQualifiedAttribute(ATTRIBUTE_ID_VARIABLE_NAME, callActivity.getProcessInstanceIdVariableName(), xtw);
        }
    }

    @Override
    protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        CallActivity callActivity = (CallActivity) element;
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_IN_PARAMETERS,
                callActivity.getInParameters(), didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = BpmnXMLUtil.writeIOParameters(ELEMENT_OUT_PARAMETERS,
                callActivity.getOutParameters(), didWriteExtensionStartElement, xtw);
        didWriteExtensionStartElement = MapExceptionExport.writeMapExceptionExtensions(callActivity.getMapExceptions(), didWriteExtensionStartElement, xtw);
        return didWriteExtensionStartElement;
    }

    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    }
}
