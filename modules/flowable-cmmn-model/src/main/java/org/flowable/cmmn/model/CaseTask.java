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

/**
 * @author Joram Barrez
 */
public class CaseTask extends ChildTask {

    protected String caseRef;
    protected String caseRefExpression;
    protected Boolean fallbackToDefaultTenant;
    protected boolean sameDeployment;
    protected String caseInstanceIdVariableName;

    public String getCaseRef() {
        return caseRef;
    }

    public void setCaseRef(String caseRef) {
        this.caseRef = caseRef;
    }

    public String getCaseRefExpression() {
        return caseRefExpression;
    }

    public void setCaseRefExpression(String caseRefExpression) {
        this.caseRefExpression = caseRefExpression;
    }

    public Boolean getFallbackToDefaultTenant() {
        return fallbackToDefaultTenant;
    }

    public void setFallbackToDefaultTenant(Boolean fallbackToDefaultTenant) {
        this.fallbackToDefaultTenant = fallbackToDefaultTenant;
    }

    public boolean isSameDeployment() {
        return sameDeployment;
    }

    public void setSameDeployment(boolean sameDeployment) {
        this.sameDeployment = sameDeployment;
    }

    public String getCaseInstanceIdVariableName() {
        return caseInstanceIdVariableName;
    }

    public void setCaseInstanceIdVariableName(String caseInstanceIdVariableName) {
        this.caseInstanceIdVariableName = caseInstanceIdVariableName;
    }

}
