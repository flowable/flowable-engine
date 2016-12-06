package org.flowable.engine.runtime;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link ProcessInstance}s via native (SQL) queries
 * 
 * @author Bernd Ruecker (camunda)
 */
public interface NativeProcessInstanceQuery extends NativeQuery<NativeProcessInstanceQuery, ProcessInstance> {

}
