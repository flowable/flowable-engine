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
package org.flowable.cmmn.converter.export;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.Case;

public class CaseExport implements CmmnXmlConstants {
    
    public static void writeCase(Case caseModel, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_CASE);
        xtw.writeAttribute(ATTRIBUTE_ID, caseModel.getId());

        if (StringUtils.isNotEmpty(caseModel.getName())) {
            xtw.writeAttribute(ATTRIBUTE_NAME, caseModel.getName());
        }
        
        if (StringUtils.isNotEmpty(caseModel.getInitiatorVariableName())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_INITIATOR_VARIABLE_NAME, caseModel.getInitiatorVariableName());
        }
        
        if (!caseModel.getCandidateStarterUsers().isEmpty()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CASE_CANDIDATE_USERS, CmmnXmlUtil.convertToDelimitedString(caseModel.getCandidateStarterUsers()));
        }

        if (!caseModel.getCandidateStarterGroups().isEmpty()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CASE_CANDIDATE_GROUPS, CmmnXmlUtil.convertToDelimitedString(caseModel.getCandidateStarterGroups()));
        }

        if (StringUtils.isNotEmpty(caseModel.getDocumentation())) {

            xtw.writeStartElement(ELEMENT_DOCUMENTATION);
            xtw.writeCharacters(caseModel.getDocumentation());
            xtw.writeEndElement();
        }
    }
}
