package org.activiti.idm.engine.impl.cmd;

/**
 * @author jbarrez
 */
public interface CustomSqlExecution<Mapper, ResultType> {

  Class<Mapper> getMapperClass();

  ResultType execute(Mapper mapper);

}