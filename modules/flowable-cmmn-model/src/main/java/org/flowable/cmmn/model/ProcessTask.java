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
public class ProcessTask extends ChildTask {

    protected String processRefExpression;
    protected String processRef;
    protected Boolean fallbackToDefaultTenant;
    protected boolean sameDeployment;
    protected String processInstanceIdVariableName;

    protected Process process;

    public String getProcessRefExpression() {
        return processRefExpression;
    }

    public void setProcessRefExpression(String processRefExpression) {
        this.processRefExpression = processRefExpression;
    }

    public String getProcessRef() {
        return processRef;
    }

    public void setProcessRef(String processRef) {
        this.processRef = processRef;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public void setFallbackToDefaultTenant(Boolean fallbackToDefaultTenant) {
        this.fallbackToDefaultTenant = fallbackToDefaultTenant;
    }

    public Boolean getFallbackToDefaultTenant() {
        return fallbackToDefaultTenant;
    }

    public boolean isSameDeployment() {
        return sameDeployment;
    }

    public void setSameDeployment(boolean sameDeployment) {
        this.sameDeployment = sameDeployment;
    }

    public String getProcessInstanceIdVariableName() {
        return processInstanceIdVariableName;
    }
    public void setProcessInstanceIdVariableName(String processInstanceIdVariableName) {
        this.processInstanceIdVariableName = processInstanceIdVariableName;
    }
}
