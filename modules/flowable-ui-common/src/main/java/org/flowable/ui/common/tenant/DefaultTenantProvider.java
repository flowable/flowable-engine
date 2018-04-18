package org.flowable.ui.common.tenant;

import org.apache.commons.lang3.StringUtils;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.flowable.ui.common.security.FlowableAppUser;
import org.flowable.ui.common.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultTenantProvider implements TenantProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTenantProvider.class);

    private String tenantId;
    
    public DefaultTenantProvider(FlowableCommonAppProperties commonAppProperties) {
        super();
        String configuredTenantId = commonAppProperties.getTenantId();
        if(! StringUtils.isBlank(configuredTenantId)) {
            // trim whitespace as trailing whitespace are possible in properties files and easy to do
            configuredTenantId = configuredTenantId.trim();
            
            // quotes can help solve whitespace issues
            LOGGER.debug("Found configured tenantId: '" + configuredTenantId + "'");
            
            this.tenantId = configuredTenantId;
        }
    }

    @Override
    public String getTenantId() {
        if(tenantId != null) {
            LOGGER.debug("Using configured tenantId: '" + tenantId + "'");
            return tenantId;
        }
        
        FlowableAppUser appUser = SecurityUtils.getCurrentFlowableAppUser();
        if(appUser != null) {
            // quotes can help solve whitespace issues, trimming here would not 
            // help solve the problem at source which is in user database
            LOGGER.debug("Using user tenantId: '" + tenantId + "'");
            
            return appUser.getUserObject().getTenantId();
        }

        LOGGER.debug("No tenantId");

        return null;
    }
    
}
