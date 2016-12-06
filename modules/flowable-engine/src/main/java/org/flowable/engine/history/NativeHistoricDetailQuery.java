package org.flowable.engine.history;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link org.flowable.engine.history.HistoricDetail}s via native (SQL) queries
 * 
 * @author Henry Yan(http://www.kafeitu.me)
 */
public interface NativeHistoricDetailQuery extends NativeQuery<NativeHistoricDetailQuery, HistoricDetail> {

}