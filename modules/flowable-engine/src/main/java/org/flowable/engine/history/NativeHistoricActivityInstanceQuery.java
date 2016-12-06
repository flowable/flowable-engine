package org.flowable.engine.history;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link HistoricActivityInstanceQuery}s via native (SQL) queries
 * 
 * @author Bernd Ruecker (camunda)
 */
public interface NativeHistoricActivityInstanceQuery extends NativeQuery<NativeHistoricActivityInstanceQuery, HistoricActivityInstance> {

}
