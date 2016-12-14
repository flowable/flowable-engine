package org.flowable.dmn.api;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link DmnDeployment}s via native (SQL) queries
 * 
 * @author Tijs Rademakers
 */
public interface NativeDmnDeploymentQuery extends NativeQuery<NativeDmnDeploymentQuery, DmnDeployment> {

}
