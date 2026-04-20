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

import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.ReactivateEventListener;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Micha Kiener
 */
public abstract class BaseCmmnXmlConverter {

    public abstract String getXMLElementName();

    /**
     * @return True if the current {@link CmmnElement} can have child elements and needs to be pushed
     *         to the stack of elements during parsing. 
     */
    public abstract boolean hasChildElements();

    public BaseElement convertToCmmnModel(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        BaseElement baseElement = convert(xtr, conversionHelper);
        if (baseElement != null) {

            baseElement.setId(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ID));
            CmmnXmlUtil.addXMLLocation(baseElement, xtr);

            if (baseElement instanceof CmmnElement cmmnElement) {
                conversionHelper.setCurrentCmmnElement(cmmnElement);
            }

            if (baseElement instanceof Criterion criterion) {
                conversionHelper.getCmmnModel().addCriterion(criterion.getId(), criterion);
            }

            // the reactivate event listener is a very specific user event listener in need to be registered in the case model
            if (baseElement instanceof ReactivateEventListener) {
                if (conversionHelper.getCurrentCase().getReactivateEventListener() != null) {
                    throw new FlowableIllegalArgumentException(
                        "There can only be one reactivation listener on a case model, not multiple ones. Use a start form on the listener, "
                        + "if there are several options on how to reactivate a case and use conditions to handle the different options on reactivation.");
                }
                conversionHelper.getCurrentCase().setReactivateEventListener((ReactivateEventListener) baseElement);
            }
        }
        return baseElement;
    }

    protected abstract BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper);

    protected void elementEnd(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        if (hasChildElements()) {
            conversionHelper.removeCurrentCmmnElement();
        }
    }

}
