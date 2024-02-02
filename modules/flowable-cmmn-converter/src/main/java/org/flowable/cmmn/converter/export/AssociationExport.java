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
import org.flowable.cmmn.model.Association;

/**
 * @author Joram Barrez
 */
public class AssociationExport implements CmmnXmlConstants {

    /**
     * Note: currently only meant for text annotation associations.
     */
    public static void writeAssociation(Association association, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_ASSOCIATION);

        String id = association.getId();
        if (StringUtils.isNotEmpty(id)) {
            xtw.writeAttribute(ATTRIBUTE_ID, id);
        }

        String sourceRef = association.getSourceRef();
        if (StringUtils.isNotEmpty(sourceRef)) {
            xtw.writeAttribute(ATTRIBUTE_SOURCE_REF, sourceRef);
        }

        String targetRef = association.getTargetRef();
        if (StringUtils.isNotEmpty(targetRef)) {
            xtw.writeAttribute(ATTRIBUTE_TARGET_REF, targetRef);
        }

        xtw.writeEndElement();
    }

}
