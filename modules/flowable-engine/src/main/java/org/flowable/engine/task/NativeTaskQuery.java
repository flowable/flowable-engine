package org.flowable.engine.task;

import org.flowable.engine.common.api.query.NativeQuery;

/**
 * Allows querying of {@link Task}s via native (SQL) queries
 * 
 * @author Bernd Ruecker (camunda)
 */
public interface NativeTaskQuery extends NativeQuery<NativeTaskQuery, Task> {

}
