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
package org.flowable.dmn.rest.service.api.decision;

import java.util.List;

import org.flowable.common.rest.variable.EngineRestVariable;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceRequest {

    protected String decisionKey;
    protected String tenantId;
    protected String parentDeploymentId;
    protected List<EngineRestVariable> inputVariables;
    protected boolean disableHistory;

    public String getDecisionKey() {
        return decisionKey;
    }

    public void setDecisionKey(String decisionKey) {
        this.decisionKey = decisionKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getParentDeploymentId() {
        return parentDeploymentId;
    }

    public void setParentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
    }

    public List<EngineRestVariable> getInputVariables() {
        return inputVariables;
    }

    public void setInputVariables(List<EngineRestVariable> variables) {
        this.inputVariables = variables;
    }

    public boolean isDisableHistory() {
        return disableHistory;
    }

    public void setDisableHistory(boolean disableHistory) {
        this.disableHistory = disableHistory;
    }
}
