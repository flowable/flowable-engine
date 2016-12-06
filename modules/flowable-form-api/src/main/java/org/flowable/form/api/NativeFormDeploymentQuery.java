package org.flowable.form.api;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link org.activiti.FormDeployment.repository.DmnDeployment}s via native (SQL) queries
 * 
 * @author Tijs Rademakers
 */
public interface NativeFormDeploymentQuery extends NativeQuery<NativeFormDeploymentQuery, FormDeployment> {

}