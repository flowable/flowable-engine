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

import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.Stage;

/**
 * @author Joram Barrez
 */
public class PlanModelXmlConverter extends StageXmlConverter {

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_PLAN_MODEL;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Stage planModelStage = (Stage) super.convert(xtr, conversionHelper);
        planModelStage.setPlanModel(true);
        conversionHelper.getCurrentCase().setPlanModel(planModelStage);
        planModelStage.setFormKey(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_FORM_KEY));

        String sameDeploymentAttribute = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_SAME_DEPLOYMENT);
        if ("false".equalsIgnoreCase(sameDeploymentAttribute)) {
            planModelStage.setSameDeployment(false);

        }
        planModelStage.setValidateFormFields(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_FORM_FIELD_VALIDATION));
        return planModelStage;
    }

}
