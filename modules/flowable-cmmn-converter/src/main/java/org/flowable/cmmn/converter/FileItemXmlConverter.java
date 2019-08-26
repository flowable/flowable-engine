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
import org.flowable.cmmn.model.CaseFileItem;
import org.flowable.cmmn.model.CmmnElement;

/**
 * @author Joram Barrez
 */
public class FileItemXmlConverter extends CaseElementXmlConverter {

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_FILE_ITEM;
    }

    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CaseFileItem caseFileItem = new CaseFileItem();

        String name = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME);
        if (StringUtils.isNotEmpty(name)) {
            caseFileItem.setName(name);
        }

        String definitionRef = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_DEFINITION_REF);
        if (StringUtils.isNotEmpty(definitionRef)) {
            caseFileItem.setCaseFileItemDefinitionRef(definitionRef);
        }

        String sourceRef = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_SOURCE_REF);
        if (StringUtils.isNotEmpty(sourceRef)) {
            caseFileItem.setSourceCaseFileItemRef(sourceRef);
        }

        String targetRefs = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_TARGET_REFS);
        if (StringUtils.isNotEmpty(targetRefs)) {
            caseFileItem.setTargetCaseFileItemRefs(targetRefs);
        }

        String multiplicity = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_MULTIPLICITY);
        if (StringUtils.isNotEmpty(multiplicity)) {
            if ("ZeroOrOne".equalsIgnoreCase(multiplicity)) {
                caseFileItem.setMultiplicity(CaseFileItem.CaseFileItemMultiplicity.ZERO_OR_ONE);
            } else if ("ZeroOrMore".equalsIgnoreCase(multiplicity)) {
                caseFileItem.setMultiplicity(CaseFileItem.CaseFileItemMultiplicity.ZERO_OR_MORE);
            } else if ("ExactlyOne".equalsIgnoreCase(multiplicity)) {
                caseFileItem.setMultiplicity(CaseFileItem.CaseFileItemMultiplicity.EXACTLY_ONE);
            } else if ("OneOrMore".equalsIgnoreCase(multiplicity)) {
                caseFileItem.setMultiplicity(CaseFileItem.CaseFileItemMultiplicity.ONE_OR_MORE);
            } else if ("Unspecified".equalsIgnoreCase(multiplicity)) {
                caseFileItem.setMultiplicity(CaseFileItem.CaseFileItemMultiplicity.UNSPECIFIED);
            } else {
                caseFileItem.setMultiplicity(CaseFileItem.CaseFileItemMultiplicity.UNKNOWN);
            }
        }

        conversionHelper.getCurrentFileItemContainer().addCaseFileItem(caseFileItem);

        // The file item can have nested children
        // Children will be parsed in the FileItemChildXmlConverter
        conversionHelper.setCurrentFileItemContainer(caseFileItem);
        return caseFileItem;
    }

    @Override
    protected void elementEnd(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        conversionHelper.removeCurrentFileItemContainer();
        super.elementEnd(xtr, conversionHelper);
    }
}
