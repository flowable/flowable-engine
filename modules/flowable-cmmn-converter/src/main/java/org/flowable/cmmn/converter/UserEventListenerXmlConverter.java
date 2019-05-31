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
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.UserEventListener;

/**
 * @author Dennis Federico
 */
public class UserEventListenerXmlConverter extends PlanItemDefinitionXmlConverter {

    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_USER_EVENT_LISTENER;
    }

    @Override
    protected BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        return convertCommonAttributes(xtr, new UserEventListener());
    }

    protected UserEventListener convertCommonAttributes(XMLStreamReader xtr, UserEventListener listener) {
        listener.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        listener.setAvailableConditionExpression(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
            CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION));

        String csvRoles = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_AUTHORIZED_ROLE_REFS);
        if (StringUtils.isNotBlank(csvRoles)) {
            String[] roles = csvRoles.split(",");
            listener.setAuthorizedRoleRefs(roles);
        }
        return listener;
    }
}
