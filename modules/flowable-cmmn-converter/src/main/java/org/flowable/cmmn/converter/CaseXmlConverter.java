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

import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnElement;

/**
 * @author Joram Barrez
 */
public class CaseXmlConverter extends BaseCmmnXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_CASE;
    }
    
    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Case caze = new Case();
        caze.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        caze.setInitiatorVariableName(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_INITIATOR_VARIABLE_NAME));
        
        String candidateUsersString = CmmnXmlUtil.getAttributeValue(CmmnXmlConstants.ATTRIBUTE_CASE_CANDIDATE_USERS, xtr);
        if (StringUtils.isNotEmpty(candidateUsersString)) {
            List<String> candidateUsers = CmmnXmlUtil.parseDelimitedList(candidateUsersString);
            caze.setCandidateStarterUsers(candidateUsers);
        }

        String candidateGroupsString = CmmnXmlUtil.getAttributeValue(CmmnXmlConstants.ATTRIBUTE_CASE_CANDIDATE_GROUPS, xtr);
        if (StringUtils.isNotEmpty(candidateGroupsString)) {
            List<String> candidateGroups = CmmnXmlUtil.parseDelimitedList(candidateGroupsString);
            caze.setCandidateStarterGroups(candidateGroups);
        }

        if ("true".equals(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_IS_ASYNCHRONOUS))) {
            caze.setAsync(true);
        }

        conversionHelper.getCmmnModel().addCase(caze);
        conversionHelper.setCurrentCase(caze);
        return caze;
    }
    
}
