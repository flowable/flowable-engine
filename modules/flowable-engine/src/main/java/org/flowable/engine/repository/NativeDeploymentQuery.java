package org.flowable.engine.repository;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link org.flowable.engine.repository.Deployment}s via native (SQL) queries
 * 
 * @author Henry Yan(http://www.kafeitu.me)
 */
public interface NativeDeploymentQuery extends NativeQuery<NativeDeploymentQuery, Deployment> {

}