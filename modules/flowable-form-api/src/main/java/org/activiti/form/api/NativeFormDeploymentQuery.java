package org.activiti.form.api;

import org.activiti.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link org.activiti.FormDeployment.repository.DmnDeployment}s via native (SQL) queries
 * 
 * @author Tijs Rademakers
 */
public interface NativeFormDeploymentQuery extends NativeQuery<NativeFormDeploymentQuery, FormDeployment> {

}