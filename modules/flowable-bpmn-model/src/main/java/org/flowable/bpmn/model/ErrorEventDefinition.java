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
package org.flowable.bpmn.model;

/**
 * @author Tijs Rademakers
 */
public class ErrorEventDefinition extends EventDefinition {

    protected String errorCode;
    protected String errorVariableName;
    protected Boolean errorVariableTransient;
    protected Boolean errorVariableLocalScope;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorVariableName() {
        return errorVariableName;
    }

    public void setErrorVariableName(String errorVariableName) {
        this.errorVariableName = errorVariableName;
    }

    public Boolean getErrorVariableTransient() {
        return errorVariableTransient;
    }

    public void setErrorVariableTransient(Boolean errorVariableTransient) {
        this.errorVariableTransient = errorVariableTransient;
    }

    public Boolean getErrorVariableLocalScope() {
        return errorVariableLocalScope;
    }

    public void setErrorVariableLocalScope(Boolean errorVariableLocalScope) {
        this.errorVariableLocalScope = errorVariableLocalScope;
    }

    @Override
    public ErrorEventDefinition clone() {
        ErrorEventDefinition clone = new ErrorEventDefinition();
        clone.setValues(this);
        return clone;
    }

    public void setValues(ErrorEventDefinition otherDefinition) {
        super.setValues(otherDefinition);
        setErrorCode(otherDefinition.getErrorCode());
        setErrorVariableName(otherDefinition.getErrorVariableName());
        setErrorVariableLocalScope(otherDefinition.getErrorVariableLocalScope());
        setErrorVariableTransient(otherDefinition.getErrorVariableTransient());
    }
}
