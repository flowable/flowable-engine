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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CallActivity extends Activity implements HasOutParameters, HasInParameters {

    protected String calledElement;
    protected String calledElementType;
    protected boolean inheritVariables;
    protected boolean sameDeployment;
    protected List<IOParameter> inParameters = new ArrayList<>();
    protected List<IOParameter> outParameters = new ArrayList<>();
    protected String processInstanceName;
    protected String businessKey;
    protected boolean inheritBusinessKey;
    protected boolean useLocalScopeForOutParameters;
    protected boolean completeAsync;
    protected Boolean fallbackToDefaultTenant;
    protected String processInstanceIdVariableName;

    public String getCalledElement() {
        return calledElement;
    }

    public void setCalledElement(String calledElement) {
        this.calledElement = calledElement;
    }

    public boolean isInheritVariables() {
        return inheritVariables;
    }

    public void setInheritVariables(boolean inheritVariables) {
        this.inheritVariables = inheritVariables;
    }

    public boolean isSameDeployment() {
        return sameDeployment;
    }

    public void setSameDeployment(boolean sameDeployment) {
        this.sameDeployment = sameDeployment;
    }

    @Override
    public List<IOParameter> getInParameters() {
        return inParameters;
    }

    @Override
    public void addInParameter(IOParameter inParameter) {
        inParameters.add(inParameter);
    }

    @Override
    public void setInParameters(List<IOParameter> inParameters) {
        this.inParameters = inParameters;
    }

    @Override
    public List<IOParameter> getOutParameters() {
        return outParameters;
    }

    @Override
    public void addOutParameter(IOParameter outParameter) {
        this.outParameters.add(outParameter);
    }

    @Override
    public void setOutParameters(List<IOParameter> outParameters) {
        this.outParameters = outParameters;
    }
    
    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void setProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public boolean isInheritBusinessKey() {
        return inheritBusinessKey;
    }

    public void setInheritBusinessKey(boolean inheritBusinessKey) {
        this.inheritBusinessKey = inheritBusinessKey;
    }

    public boolean isUseLocalScopeForOutParameters() {
        return useLocalScopeForOutParameters;
    }

    public void setUseLocalScopeForOutParameters(boolean useLocalScopeForOutParameters) {
        this.useLocalScopeForOutParameters = useLocalScopeForOutParameters;
    }
    
    public boolean isCompleteAsync() {
        return completeAsync;
    }

    public void setCompleteAsync(boolean completeAsync) {
        this.completeAsync = completeAsync;
    }

    public Boolean getFallbackToDefaultTenant() {
        return fallbackToDefaultTenant;
    }

    public void setFallbackToDefaultTenant(Boolean fallbackToDefaultTenant) {
        this.fallbackToDefaultTenant = fallbackToDefaultTenant;
    }
    
    public void setCalledElementType(String calledElementType) {
        this.calledElementType = calledElementType;
    }

    public String getCalledElementType() {
        return calledElementType;
    }

    public String getProcessInstanceIdVariableName() {
        return processInstanceIdVariableName;
    }

    public void setProcessInstanceIdVariableName(String processInstanceIdVariableName) {
        this.processInstanceIdVariableName = processInstanceIdVariableName;
    }

    @Override
    public CallActivity clone() {
        CallActivity clone = new CallActivity();
        clone.setValues(this);
        return clone;
    }

    public void setValues(CallActivity otherElement) {
        super.setValues(otherElement);
        setCalledElement(otherElement.getCalledElement());
        setCalledElementType(otherElement.getCalledElementType());
        setBusinessKey(otherElement.getBusinessKey());
        setInheritBusinessKey(otherElement.isInheritBusinessKey());
        setInheritVariables(otherElement.isInheritVariables());
        setSameDeployment(otherElement.isSameDeployment());
        setUseLocalScopeForOutParameters(otherElement.isUseLocalScopeForOutParameters());
        setCompleteAsync(otherElement.isCompleteAsync());
        setFallbackToDefaultTenant(otherElement.getFallbackToDefaultTenant());

        inParameters = new ArrayList<>();
        if (otherElement.getInParameters() != null && !otherElement.getInParameters().isEmpty()) {
            for (IOParameter parameter : otherElement.getInParameters()) {
                inParameters.add(parameter.clone());
            }
        }

        outParameters = new ArrayList<>();
        if (otherElement.getOutParameters() != null && !otherElement.getOutParameters().isEmpty()) {
            for (IOParameter parameter : otherElement.getOutParameters()) {
                outParameters.add(parameter.clone());
            }
        }
    }

}
