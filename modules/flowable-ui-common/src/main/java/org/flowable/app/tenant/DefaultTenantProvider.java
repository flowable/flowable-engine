package org.flowable.app.tenant;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.security.FlowableAppUser;
import org.flowable.app.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DefaultTenantProvider implements TenantProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTenantProvider.class);

    private String tenantId;
    
    public DefaultTenantProvider(Environment environment) {
        super();
        String configuredTenantId = environment.getProperty("tenant.tenant_id");
        if(! StringUtils.isBlank(configuredTenantId)) {
            // trim whitespace as trailing whitespace are possible in properties files and easy to do
            configuredTenantId = configuredTenantId.trim();
            if(LOGGER.isDebugEnabled()) {
                // quotes can help solve whitespace issues
                LOGGER.debug("Found configured tenantId: '" + configuredTenantId + "'");
            }
            this.tenantId = configuredTenantId;
        }
    }

    @Override
    public String getTenantId() {
        if(tenantId != null) {
            if(LOGGER.isTraceEnabled()) {
                LOGGER.trace("Using configured tenantId: '" + tenantId + "'");
            }
            return tenantId;
        }
        
        FlowableAppUser appUser = SecurityUtils.getCurrentFlowableAppUser();
        if(appUser != null) {
            if(LOGGER.isTraceEnabled()) {
                // quotes can help solve whitespace issues, trimming here would not 
                // help solve the problem at source which is in user database
                LOGGER.trace("Using user tenantId: '" + tenantId + "'");
            }
            return appUser.getUserObject().getTenantId();
        }

        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("No tenantId");
        }

        return null;
    }
    
}
