package org.activiti5.engine.repository;

import org.activiti5.engine.query.NativeQuery;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * Allows querying of {@link org.activiti5.engine.repository.ProcessDefinition}s via native (SQL) queries
 * @author Henry Yan(http://www.kafeitu.me)
 */
public interface NativeProcessDefinitionQuery extends NativeQuery<NativeProcessDefinitionQuery, ProcessDefinition> {

}