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
package org.flowable.dmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.flowable.dmn.converter.util.DmnXMLUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.ItemDefinition;

/**
 * @author Yvo Swillens
 */
public class ItemComponentParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_ITEM_COMPONENT;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, DmnElement parentElement, Decision decision) throws Exception {
        if (!(parentElement instanceof ItemDefinition itemDefinition)) {
            return;
        }

        ItemDefinition itemComponent = new ItemDefinition();
        itemDefinition.addItemComponent(itemComponent);

        itemComponent.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
        itemComponent.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
        itemComponent.setLabel(xtr.getAttributeValue(null, ATTRIBUTE_LABEL));
        itemComponent.setCollection(Boolean.parseBoolean(xtr.getAttributeValue(null, ATTRIBUTE_IS_COLLECTION)));

        DmnXMLUtil.parseChildElements(ELEMENT_ITEM_COMPONENT, itemComponent, xtr, null, decision);
    }
}
