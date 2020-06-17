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

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.ProcessTask;

/**
 * @author Joram Barrez
 */
public class ProcessTaskXmlConverter extends TaskXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_PROCESS_TASK;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        ProcessTask processTask = new ProcessTask();
        convertCommonTaskAttributes(xtr, processTask);
        processTask.setProcessRef(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_PROCESS_REF));

        String businessKey = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_BUSINESS_KEY);
        if (businessKey != null) {
            processTask.setBusinessKey(businessKey);
        }

        String inheritBusinessKey = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_INHERIT_BUSINESS_KEY);
        if (inheritBusinessKey != null) {
            processTask.setInheritBusinessKey(Boolean.parseBoolean(inheritBusinessKey));
        }

        String fallbackToDefaultTenantValue = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_FALLBACK_TO_DEFAULT_TENANT);
        if (fallbackToDefaultTenantValue != null) {
            processTask.setFallbackToDefaultTenant(Boolean.parseBoolean(fallbackToDefaultTenantValue));
        }

        String sameDeploymentValue = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_SAME_DEPLOYMENT);
        if (sameDeploymentValue != null) {
            processTask.setSameDeployment(Boolean.parseBoolean(sameDeploymentValue));
        }

        String idVariableName = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_ID_VARIABLE_NAME);
        if (StringUtils.isNotEmpty(idVariableName)) {
            processTask.setProcessInstanceIdVariableName(idVariableName);
        }
        
        return processTask;
    }
    
}
