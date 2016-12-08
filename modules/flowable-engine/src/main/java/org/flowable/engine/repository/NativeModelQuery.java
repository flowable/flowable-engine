package org.flowable.engine.repository;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link org.flowable.engine.repository.Model}s via native (SQL) queries
 * 
 * @author Henry Yan(http://www.kafeitu.me)
 */
public interface NativeModelQuery extends NativeQuery<NativeModelQuery, Model> {

}