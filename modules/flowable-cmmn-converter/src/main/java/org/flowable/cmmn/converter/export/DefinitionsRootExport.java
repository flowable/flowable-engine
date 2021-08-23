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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.CmmnModel;

public class DefinitionsRootExport implements CmmnXmlConstants {

    /** default namespaces for definitions */
    protected static final Set<String> defaultNamespaces = new HashSet<>(Arrays.asList(XSI_PREFIX, FLOWABLE_EXTENSIONS_PREFIX, CMMNDI_PREFIX, OMGDC_PREFIX, OMGDI_PREFIX));

    public static void writeRootElement(CmmnModel model, XMLStreamWriter xtw, String encoding) throws Exception {
        xtw.writeStartDocument(encoding, "1.0");

        // start definitions root element
        xtw.writeStartElement(ELEMENT_DEFINITIONS);
        xtw.setDefaultNamespace(CMMN_NAMESPACE);
        xtw.writeDefaultNamespace(CMMN_NAMESPACE);
        xtw.writeNamespace(XSI_PREFIX, XSI_NAMESPACE);
        xtw.writeNamespace(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE);
        xtw.writeNamespace(CMMNDI_PREFIX, CMMNDI_NAMESPACE);
        xtw.writeNamespace(OMGDC_PREFIX, OMGDC_NAMESPACE);
        xtw.writeNamespace(OMGDI_PREFIX, OMGDI_NAMESPACE);
        for (String prefix : model.getNamespaces().keySet()) {
            if (!defaultNamespaces.contains(prefix) && StringUtils.isNotEmpty(prefix)) {
                xtw.writeNamespace(prefix, model.getNamespaces().get(prefix));
            }
        }
        if (StringUtils.isNotEmpty(model.getTargetNamespace())) {
            xtw.writeAttribute(ATTRIBUTE_TARGET_NAMESPACE, model.getTargetNamespace());
        } else {
            xtw.writeAttribute(ATTRIBUTE_TARGET_NAMESPACE, CASE_NAMESPACE);
        }

        if (StringUtils.isNotEmpty(model.getExporter())) {
            xtw.writeAttribute(CmmnXmlConstants.ATTRIBUTE_EXPORTER, model.getExporter());
        }
        if (StringUtils.isNotEmpty(model.getExporterVersion())) {
            xtw.writeAttribute(CmmnXmlConstants.ATTRIBUTE_EXPORTER_VERSION, model.getExporterVersion());
        }
    }
}
