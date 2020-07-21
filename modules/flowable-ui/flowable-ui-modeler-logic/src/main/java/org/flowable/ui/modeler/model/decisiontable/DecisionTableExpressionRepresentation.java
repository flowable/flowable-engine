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
package org.flowable.ui.modeler.model.decisiontable;

import java.util.List;

/**
 * Created by Yvo Swillens
 */
public class DecisionTableExpressionRepresentation {

    public static final String VARIABLE_TYPE_VARIABLE = "variable";

    protected String id;
    protected String variableId;
    protected String variableType;
    protected String type;
    protected String label;
    protected List<String> entries;
    protected boolean newVariable;
    protected boolean complexExpression;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVariableId() {
        return variableId;
    }

    public void setVariableId(String variableId) {
        this.variableId = variableId;
    }

    public String getVariableType() {
        return variableType;
    }

    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getEntries() {
        return entries;
    }

    public void setEntries(List<String> entries) {
        this.entries = entries;
    }

    public boolean isNewVariable() {
        return newVariable;
    }

    public void setNewVariable(boolean newVariable) {
        this.newVariable = newVariable;
    }

    public boolean isComplexExpression() {
        return complexExpression;
    }

    public void setComplexExpression(boolean complexExpression) {
        this.complexExpression = complexExpression;
    }
}
