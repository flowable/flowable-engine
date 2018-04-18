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
package org.flowable.engine.impl.util;

import org.flowable.cmmn.api.CmmnEngineConfigurationApi;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.content.api.ContentEngineConfigurationApi;
import org.flowable.content.api.ContentService;
import org.flowable.dmn.api.DmnEngineConfigurationApi;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;

public class EngineServiceUtil {
    
    // IDM ENGINE
    
    public static IdmEngineConfiguration getIdmEngineConfiguration(AbstractEngineConfiguration engineConfiguration) {
        return (IdmEngineConfiguration) engineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
    }
    
    public static IdmIdentityService getIdmIdentityService(AbstractEngineConfiguration engineConfiguration) {
        IdmIdentityService idmIdentityService = null;
        IdmEngineConfiguration idmEngineConfiguration = getIdmEngineConfiguration(engineConfiguration);
        if (idmEngineConfiguration != null) {
            idmIdentityService = idmEngineConfiguration.getIdmIdentityService();
        }
        
        return idmIdentityService;
    }
    
    // CMMN ENGINE
    
    public static CmmnEngineConfigurationApi getCmmnEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        return (CmmnEngineConfigurationApi) processEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
    }
    
    public static CmmnRepositoryService getCmmnRepositoryService(ProcessEngineConfiguration processEngineConfiguration) {
        CmmnRepositoryService cmmnRepositoryService = null;
        CmmnEngineConfigurationApi cmmnEngineConfiguration = getCmmnEngineConfiguration(processEngineConfiguration);
        if (cmmnEngineConfiguration != null) {
            cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        }
        
        return cmmnRepositoryService;
    }
    
    public static CmmnRuntimeService getCmmnRuntimeService(ProcessEngineConfiguration processEngineConfiguration) {
        CmmnRuntimeService cmmnRuntimeService = null;
        CmmnEngineConfigurationApi cmmnEngineConfiguration = getCmmnEngineConfiguration(processEngineConfiguration);
        if (cmmnEngineConfiguration != null) {
            cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        }
        
        return cmmnRuntimeService;
    }
    
    public static CmmnHistoryService getCmmnHistoryService(ProcessEngineConfiguration processEngineConfiguration) {
        CmmnHistoryService cmmnHistoryService = null;
        CmmnEngineConfigurationApi cmmnEngineConfiguration = getCmmnEngineConfiguration(processEngineConfiguration);
        if (cmmnEngineConfiguration != null) {
            cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
        }
        
        return cmmnHistoryService;
    }
    
    public static CmmnManagementService getCmmnManagementService(ProcessEngineConfiguration processEngineConfiguration) {
        CmmnManagementService cmmnManagementService = null;
        CmmnEngineConfigurationApi cmmnEngineConfiguration = getCmmnEngineConfiguration(processEngineConfiguration);
        if (cmmnEngineConfiguration != null) {
            cmmnManagementService = cmmnEngineConfiguration.getCmmnManagementService();
        }
        
        return cmmnManagementService;
    }
    
    // DMN ENGINE
    
    public static DmnEngineConfigurationApi getDmnEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        return (DmnEngineConfigurationApi) processEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
    }
    
    public static DmnRepositoryService getDmnRepositoryService(ProcessEngineConfiguration processEngineConfiguration) {
        DmnRepositoryService dmnRepositoryService = null;
        DmnEngineConfigurationApi dmnEngineConfiguration = getDmnEngineConfiguration(processEngineConfiguration);
        if (dmnEngineConfiguration != null) {
            dmnRepositoryService = dmnEngineConfiguration.getDmnRepositoryService();
        }
        
        return dmnRepositoryService;
    }
    
    public static DmnRuleService getDmnRuleService(ProcessEngineConfiguration processEngineConfiguration) {
        DmnRuleService dmnRuleService = null;
        DmnEngineConfigurationApi dmnEngineConfiguration = getDmnEngineConfiguration(processEngineConfiguration);
        if (dmnEngineConfiguration != null) {
            dmnRuleService = dmnEngineConfiguration.getDmnRuleService();
        }
        
        return dmnRuleService;
    }
    
    public static DmnManagementService getDmnManagementService(ProcessEngineConfiguration processEngineConfiguration) {
        DmnManagementService dmnManagementService = null;
        DmnEngineConfigurationApi dmnEngineConfiguration = getDmnEngineConfiguration(processEngineConfiguration);
        if (dmnEngineConfiguration != null) {
            dmnManagementService = dmnEngineConfiguration.getDmnManagementService();
        }
        
        return dmnManagementService;
    }
    
    // FORM ENGINE
    
    public static FormEngineConfigurationApi getFormEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        return (FormEngineConfigurationApi) processEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
    }
    
    public static FormRepositoryService getFormRepositoryService(ProcessEngineConfiguration processEngineConfiguration) {
        FormRepositoryService formRepositoryService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration(processEngineConfiguration);
        if (formEngineConfiguration != null) {
            formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        }
        
        return formRepositoryService;
    }
    
    public static FormService getFormService(ProcessEngineConfiguration processEngineConfiguration) {
        FormService formService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration(processEngineConfiguration);
        if (formEngineConfiguration != null) {
            formService = formEngineConfiguration.getFormService();
        }
        
        return formService;
    }
    
    public static FormManagementService getFormManagementService(ProcessEngineConfiguration processEngineConfiguration) {
        FormManagementService formManagementService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration(processEngineConfiguration);
        if (formEngineConfiguration != null) {
            formManagementService = formEngineConfiguration.getFormManagementService();
        }
        
        return formManagementService;
    }
    
    // CONTENT ENGINE
    
    public static ContentEngineConfigurationApi getContentEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        return (ContentEngineConfigurationApi) processEngineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG);
    }
    
    public static ContentService getContentService(ProcessEngineConfiguration processEngineConfiguration) {
        ContentService contentService = null;
        ContentEngineConfigurationApi contentEngineConfiguration = getContentEngineConfiguration(processEngineConfiguration);
        if (contentEngineConfiguration != null) {
            contentService = contentEngineConfiguration.getContentService();
        }
        
        return contentService;
    }

}
