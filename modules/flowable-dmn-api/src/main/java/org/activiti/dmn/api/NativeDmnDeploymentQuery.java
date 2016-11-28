package org.activiti.dmn.api;

import org.activiti.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link org.activiti.engine.repository.DmnDeployment}s via native (SQL) queries
 * 
 * @author Tijs Rademakers
 */
public interface NativeDmnDeploymentQuery extends NativeQuery<NativeDmnDeploymentQuery, DmnDeployment> {

}