package org.flowable.common.engine.impl;

/**
 * @author Valentin Zickner
 */
public interface DefaultTenantProvider {

    String getDefaultTenant(String tenantId, String scope, String scopeKey);

}
