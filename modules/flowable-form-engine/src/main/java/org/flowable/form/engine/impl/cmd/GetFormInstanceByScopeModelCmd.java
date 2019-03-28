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
package org.flowable.form.engine.impl.cmd;

import java.util.Map;

/**
 * @author Tijs Rademakers
 */
public class GetFormInstanceByScopeModelCmd extends AbstractGetFormInstanceModelCmd {

    private static final long serialVersionUID = 1L;
    
    public GetFormInstanceByScopeModelCmd(String formDefinitionKey, String parentDeploymentId, String scopeId, String scopeType, 
                    Map<String, Object> variables) {
        
        initializeValuesForScope(formDefinitionKey, parentDeploymentId, formDefinitionKey, null, scopeId, scopeType, variables, false);
    }

    public GetFormInstanceByScopeModelCmd(String formDefinitionKey, String parentDeploymentId, String scopeId, String scopeType, 
                    String tenantId, Map<String, Object> variables, boolean fallbackToDefaultTenant) {

        initializeValuesForScope(formDefinitionKey, parentDeploymentId, null, tenantId, 
                        scopeId, scopeType, variables, fallbackToDefaultTenant);
    }
}
