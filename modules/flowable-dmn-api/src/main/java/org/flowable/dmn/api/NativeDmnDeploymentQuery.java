package org.flowable.dmn.api;

import org.flowable.common.engine.api.query.NativeQuery;

/**
 * Allows querying of {@link DmnDeployment}s via native (SQL) queries
 * 
 * @author Tijs Rademakers
 */
public interface NativeDmnDeploymentQuery extends NativeQuery<NativeDmnDeploymentQuery, DmnDeployment> {

}
