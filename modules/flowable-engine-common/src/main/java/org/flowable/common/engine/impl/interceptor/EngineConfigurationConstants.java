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
package org.flowable.common.engine.impl.interceptor;

public interface EngineConfigurationConstants {
    
    String KEY_PROCESS_ENGINE_CONFIG = "cfg.processEngine";
    
    String KEY_DMN_ENGINE_CONFIG = "cfg.dmnEngine";
    
    String KEY_IDM_ENGINE_CONFIG = "cfg.idmEngine";
    
    String KEY_FORM_ENGINE_CONFIG = "cfg.formEngine";
    
    String KEY_CONTENT_ENGINE_CONFIG = "cfg.contentEngine";
    
    String KEY_CMMN_ENGINE_CONFIG = "cfg.cmmnEngine";
    
    String KEY_APP_ENGINE_CONFIG = "cfg.appEngine";

    String KEY_TASK_SERVICE_CONFIG = "cfg.taskService";
    
    String KEY_VARIABLE_SERVICE_CONFIG = "cfg.variableService";
    
    String KEY_IDENTITY_LINK_SERVICE_CONFIG = "cfg.identityLinkService";
    
    String KEY_JOB_SERVICE_CONFIG = "cfg.jobService";
    
    int PRIORITY_ENGINE_IDM = 100000;
    
    int PRIORITY_ENGINE_DMN = PRIORITY_ENGINE_IDM + 100000;
    
    int PRIORITY_ENGINE_FORM = PRIORITY_ENGINE_DMN + 100000;
    
    int PRIORITY_ENGINE_CONTENT = PRIORITY_ENGINE_FORM + 100000;
    
    int PRIORITY_ENGINE_CMMN = PRIORITY_ENGINE_CONTENT + 100000;
    
}
