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
package org.activiti.form.engine.impl;

import java.util.Map;

import org.activiti.engine.common.api.management.TableMetaData;
import org.activiti.engine.common.api.management.TablePageQuery;
import org.activiti.engine.common.impl.cmd.CustomSqlExecution;
import org.activiti.form.api.FormManagementService;
import org.activiti.form.engine.impl.cmd.ExecuteCustomSqlCmd;
import org.activiti.form.engine.impl.cmd.GetTableCountCmd;
import org.activiti.form.engine.impl.cmd.GetTableMetaDataCmd;
import org.activiti.form.engine.impl.cmd.GetTableNameCmd;

/**
 * @author Tijs Rademakers
 */
public class FormManagementServiceImpl extends ServiceImpl implements FormManagementService {

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

  public <MapperType, ResultType> ResultType executeCustomSql(CustomSqlExecution<MapperType, ResultType> customSqlExecution) {
    Class<MapperType> mapperClass = customSqlExecution.getMapperClass();
    return commandExecutor.execute(new ExecuteCustomSqlCmd<MapperType, ResultType>(mapperClass, customSqlExecution));
  }

}
