/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.idm.engine.impl;

import java.sql.Connection;
import java.util.Map;

import org.activiti.engine.common.api.management.TableMetaData;
import org.activiti.engine.common.api.management.TablePageQuery;
import org.activiti.engine.common.impl.cmd.CustomSqlExecution;
import org.activiti.engine.common.impl.interceptor.CommandConfig;
import org.activiti.idm.api.IdmManagementService;
import org.activiti.idm.engine.impl.cmd.ExecuteCustomSqlCmd;
import org.activiti.idm.engine.impl.cmd.GetPropertiesCmd;
import org.activiti.idm.engine.impl.cmd.GetTableCountCmd;
import org.activiti.idm.engine.impl.cmd.GetTableMetaDataCmd;
import org.activiti.idm.engine.impl.cmd.GetTableNameCmd;
import org.activiti.idm.engine.impl.db.DbSqlSession;
import org.activiti.idm.engine.impl.db.DbSqlSessionFactory;
import org.activiti.idm.engine.impl.interceptor.Command;
import org.activiti.idm.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class IdmManagementServiceImpl extends ServiceImpl implements IdmManagementService {

  public Map<String, Long> getTableCount() {
    return commandExecutor.execute(new GetTableCountCmd());
  }

  public String getTableName(Class<?> activitiEntityClass) {
    return commandExecutor.execute(new GetTableNameCmd(activitiEntityClass));
  }

  public TableMetaData getTableMetaData(String tableName) {
    return commandExecutor.execute(new GetTableMetaDataCmd(tableName));
  }
  
  public TablePageQuery createTablePageQuery() {
    return new TablePageQueryImpl(commandExecutor);
  }

  public Map<String, String> getProperties() {
    return commandExecutor.execute(new GetPropertiesCmd());
  }

  public String databaseSchemaUpgrade(final Connection connection, final String catalog, final String schema) {
    CommandConfig config = commandExecutor.getDefaultConfig().transactionNotSupported();
    return commandExecutor.execute(config, new Command<String>() {
      public String execute(CommandContext commandContext) {
        DbSqlSessionFactory dbSqlSessionFactory = (DbSqlSessionFactory) commandContext.getSessionFactories().get(DbSqlSession.class);
        DbSqlSession dbSqlSession = new DbSqlSession(dbSqlSessionFactory, connection, catalog, schema);
        commandContext.getSessions().put(DbSqlSession.class, dbSqlSession);
        return dbSqlSession.dbSchemaUpdate();
      }
    });
  }

  public <MapperType, ResultType> ResultType executeCustomSql(CustomSqlExecution<MapperType, ResultType> customSqlExecution) {
    Class<MapperType> mapperClass = customSqlExecution.getMapperClass();
    return commandExecutor.execute(new ExecuteCustomSqlCmd<MapperType, ResultType>(mapperClass, customSqlExecution));
  }

}
