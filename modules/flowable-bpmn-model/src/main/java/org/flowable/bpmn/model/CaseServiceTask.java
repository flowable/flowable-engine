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
 */
public class CaseServiceTask extends ServiceTask {

    protected String caseDefinitionKey;
    protected String caseInstanceName;
    protected boolean sameDeployment;
    protected String businessKey;
    protected boolean inheritBusinessKey;
    protected boolean fallbackToDefaultTenant;
    protected String caseInstanceIdVariableName;
    
    protected List<IOParameter> inParameters = new ArrayList<>();
    protected List<IOParameter> outParameters = new ArrayList<>();

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }

    public String getCaseInstanceName() {
        return caseInstanceName;
    }

    public void setCaseInstanceName(String caseInstanceName) {
        this.caseInstanceName = caseInstanceName;
    }

    public boolean isSameDeployment() {
        return sameDeployment;
    }

    public void setSameDeployment(boolean sameDeployment) {
        this.sameDeployment = sameDeployment;
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

    public boolean isFallbackToDefaultTenant() {
        return fallbackToDefaultTenant;
    }

    public void setFallbackToDefaultTenant(boolean fallbackToDefaultTenant) {
        this.fallbackToDefaultTenant = fallbackToDefaultTenant;
    }

    public List<IOParameter> getInParameters() {
        return inParameters;
    }

    public void setInParameters(List<IOParameter> inParameters) {
        this.inParameters = inParameters;
    }

    public List<IOParameter> getOutParameters() {
        return outParameters;
    }

    public void setOutParameters(List<IOParameter> outParameters) {
        this.outParameters = outParameters;
    }

    public String getCaseInstanceIdVariableName() {
        return caseInstanceIdVariableName;
    }

    public void setCaseInstanceIdVariableName(String caseInstanceIdVariableName) {
        this.caseInstanceIdVariableName = caseInstanceIdVariableName;
    }

    @Override
    public CaseServiceTask clone() {
        CaseServiceTask clone = new CaseServiceTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(CaseServiceTask otherElement) {
        super.setValues(otherElement);

        setCaseDefinitionKey(otherElement.getCaseDefinitionKey());
        setCaseInstanceName(otherElement.getCaseInstanceName());
        setBusinessKey(otherElement.getBusinessKey());
        setInheritBusinessKey(otherElement.isInheritBusinessKey());
        setSameDeployment(otherElement.isSameDeployment());
        setFallbackToDefaultTenant(otherElement.isFallbackToDefaultTenant());
        setCaseInstanceIdVariableName(otherElement.getCaseInstanceIdVariableName());

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
