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
package org.flowable.dmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yvo Swillens
 */
public class ItemDefinition extends NamedElement {

    protected String typeRef;
    protected UnaryTests allowedValues;
    protected List<ItemDefinition> itemComponents = new ArrayList<>();
    protected String typeLanguage;
    protected boolean isCollection;

    public String getTypeRef() {
        return typeRef;
    }

    public void setTypeRef(String typeRef) {
        this.typeRef = typeRef;
    }

    public UnaryTests getAllowedValues() {
        return allowedValues;
    }
    public void setAllowedValues(UnaryTests allowedValues) {
        this.allowedValues = allowedValues;
    }
    public List<ItemDefinition> getItemComponents() {
        return itemComponents;
    }
    public void setItemComponents(List<ItemDefinition> itemComponents) {
        this.itemComponents = itemComponents;
    }
    public void addItemComponent(ItemDefinition itemComponent) {
        this.itemComponents.add(itemComponent);
    }
    public String getTypeLanguage() {
        return typeLanguage;
    }
    public void setTypeLanguage(String typeLanguage) {
        this.typeLanguage = typeLanguage;
    }
    public boolean isCollection() {
        return isCollection;
    }
    public void setCollection(boolean collection) {
        isCollection = collection;
    }
}
