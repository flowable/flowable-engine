package org.flowable.engine.history;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link HistoricTaskInstanceQuery}s via native (SQL) queries
 * 
 * @author Bernd Ruecker (camunda)
 */
public interface NativeHistoricTaskInstanceQuery extends NativeQuery<NativeHistoricTaskInstanceQuery, HistoricTaskInstance> {

}
