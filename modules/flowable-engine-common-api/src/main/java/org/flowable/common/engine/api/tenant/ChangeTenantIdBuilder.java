package org.flowable.common.engine.api.tenant;

/**
 * Helper for changing the tenant id from active and historic instances.
 * 
 * An instance can be created with the corresponding management service of each of the BPMN, CMMN or DMN scopes. 
 * 
 * @author Jorge Mora Giménez
 * 
 */
public interface ChangeTenantIdBuilder {

    /**
     * Only the instances that were created following a definition from the default tenant will be affected by the change,
     * that is, if the instance was created with the "fallback to default tenant" feature.
     *
     */    
    ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions(boolean onlyInstancesFromDefaultTenantDefinitionsEnabled);

    /**
     * Executes a simulation of the change of tenant id, calculating the amount of instances that will be affected
     * 
     * @return {@link ChangeTenantIdResult}  
     */    
   ChangeTenantIdResult simulate();

    /**
     * Executes change in the tenant id, returning the amount of instances that were affected
     * 
     * @return {@link ChangeTenantIdResult}  
     */
    ChangeTenantIdResult complete();

}
