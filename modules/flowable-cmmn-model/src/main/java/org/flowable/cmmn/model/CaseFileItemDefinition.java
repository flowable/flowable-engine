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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joram Barrez
 */
public class CaseFileItemDefinition extends CmmnElement {

    protected String name;
    protected String definitionType;
    protected List<CaseFileItemPropertyDefinition> propertyDefinitions = new ArrayList<>();

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDefinitionType() {
        return definitionType;
    }
    public void setDefinitionType(String definitionType) {
        this.definitionType = definitionType;
    }
    public void addPropertyDefinition(CaseFileItemPropertyDefinition caseFileItemPropertyDefinition) {
        this.propertyDefinitions.add(caseFileItemPropertyDefinition);
    }
    public List<CaseFileItemPropertyDefinition> getPropertyDefinitions() {
        return propertyDefinitions;
    }
    public void setPropertyDefinitions(List<CaseFileItemPropertyDefinition> propertyDefinitions) {
        this.propertyDefinitions = propertyDefinitions;
    }
}
