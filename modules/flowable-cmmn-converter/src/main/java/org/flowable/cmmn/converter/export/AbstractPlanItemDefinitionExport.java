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
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItemDefinition;

public abstract class AbstractPlanItemDefinitionExport<T extends PlanItemDefinition> implements CmmnXmlConstants {

    /**
     * The class for which exporter subclasess works for
     * @return a Class that extends PlanItemDefinition
     */
    protected abstract Class<? extends T> getExportablePlanItemDefinitionClass();

    /**
     * The steps followed to write a planItemDefinition
     *
     * @param planItemDefinition the plan item definition to write
     * @param xtw                the XML to write the definition to
     * @throws Exception in case of write exception
     */
    public void writePlanItemDefinition(CmmnModel model, T planItemDefinition, XMLStreamWriter xtw) throws Exception {
        writePlanItemDefinitionStartElement(planItemDefinition, xtw);
        writePlanItemDefinitionCommonAttributes(planItemDefinition, xtw);
        writePlanItemDefinitionSpecificAttributes(planItemDefinition, xtw);
        boolean didWriteExtensionElement = writePlanItemDefinitionCommonElements(model, planItemDefinition, xtw);
        didWriteExtensionElement = writePlanItemDefinitionExtensionElements(model, planItemDefinition, didWriteExtensionElement, xtw);
        if (didWriteExtensionElement) {
            xtw.writeEndElement();
        }
        writePlanItemDefinitionDefaultItemControl(planItemDefinition, xtw);
        writePlanItemDefinitionBody(model, planItemDefinition, xtw);
        writePlanItemDefinitionEndElement(xtw);
    }

    protected void writePlanItemDefinitionStartElement(T planItemDefinition, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(getPlanItemDefinitionXmlElementValue(planItemDefinition));
    }

    /**
     * Subclasses must override this method to provide the xml element tag value of this planItemDefintion
     *
     * @param planItemDefinition the plan item definition to write
     * @return the value of the xml element tag to write
     */
    protected abstract String getPlanItemDefinitionXmlElementValue(T planItemDefinition);

    protected void writePlanItemDefinitionCommonAttributes(T planItemDefinition, XMLStreamWriter xtw) throws Exception {
        xtw.writeAttribute(ATTRIBUTE_ID, planItemDefinition.getId());

        if (StringUtils.isNotEmpty(planItemDefinition.getName())) {
            xtw.writeAttribute(ATTRIBUTE_NAME, planItemDefinition.getName());
        }
    }

    /**
     * Subclasses can override this method to write attributes specific to the plainItemDefinition element
     *
     * @param planItemDefinition the plan item definition to write
     * @param xtw                the XML to write the definition to
     * @throws Exception in case of write exception
     */
    protected void writePlanItemDefinitionSpecificAttributes(T planItemDefinition, XMLStreamWriter xtw) throws Exception {

    }

    /**
     * Writes common elements like planItem documentation.
     * Subclasses should call super.writePlanItemDefinitionCommonElements(), it is recommended to override
     * writePlanItemDefinitionBody instead
     *
     * @param planItemDefinition the plan item definition to write
     * @param xtw                the XML to write the definition to
     * @throws Exception in case of write exception
     */
    protected boolean writePlanItemDefinitionCommonElements(CmmnModel model, T planItemDefinition, XMLStreamWriter xtw) throws Exception {
        if (StringUtils.isNotEmpty(planItemDefinition.getDocumentation())) {
            xtw.writeStartElement(ELEMENT_DOCUMENTATION);
            xtw.writeCharacters(planItemDefinition.getDocumentation());
            xtw.writeEndElement();
        }
        
        return CmmnXmlUtil.writeExtensionElements(planItemDefinition, false, model.getNamespaces(), xtw);
    }
    
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, T planItemDefinition, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws Exception {
        return didWriteExtensionElement;
    }

    protected void writePlanItemDefinitionDefaultItemControl(T planItemDefinition, XMLStreamWriter xtw) throws Exception {
        if (planItemDefinition.getDefaultControl() != null) {
            PlanItemControlExport.writeDefaultControl(planItemDefinition.getDefaultControl(), xtw);
        }
    }

    /**
     * Subclasses can override this method to write the content body xml content of the plainItemDefinition
     *
     * @param planItemDefinition the plan item definition to write
     * @param xtw                the XML to write the definition to
     * @throws Exception in case of write exception
     */
    protected void writePlanItemDefinitionBody(CmmnModel model, T planItemDefinition, XMLStreamWriter xtw) throws Exception {

    }

    protected void writePlanItemDefinitionEndElement(XMLStreamWriter xtw) throws Exception {
        xtw.writeEndElement();
    }

}
