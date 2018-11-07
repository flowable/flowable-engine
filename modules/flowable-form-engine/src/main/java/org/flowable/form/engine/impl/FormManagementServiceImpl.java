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
package org.flowable.form.engine.impl;

import java.util.Map;

import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.api.management.TablePageQuery;
import org.flowable.common.engine.impl.cmd.CustomSqlExecution;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.cmd.ExecuteCustomSqlCmd;
import org.flowable.form.engine.impl.cmd.GetTableCountCmd;
import org.flowable.form.engine.impl.cmd.GetTableMetaDataCmd;
import org.flowable.form.engine.impl.cmd.GetTableNameCmd;

/**
 * @author Tijs Rademakers
 */
public class FormManagementServiceImpl extends CommonEngineServiceImpl<FormEngineConfiguration> implements FormManagementService {

    public FormManagementServiceImpl(FormEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public Map<String, Long> getTableCount() {
        return commandExecutor.execute(new GetTableCountCmd());
    }

    @Override
    public String getTableName(Class<?> flowableEntityClass) {
        return commandExecutor.execute(new GetTableNameCmd(flowableEntityClass));
    }

    @Override
    public TableMetaData getTableMetaData(String tableName) {
        return commandExecutor.execute(new GetTableMetaDataCmd(tableName));
    }

    @Override
    public TablePageQuery createTablePageQuery() {
        return new TablePageQueryImpl(commandExecutor);
    }

    public <MapperType, ResultType> ResultType executeCustomSql(CustomSqlExecution<MapperType, ResultType> customSqlExecution) {
        Class<MapperType> mapperClass = customSqlExecution.getMapperClass();
        return commandExecutor.execute(new ExecuteCustomSqlCmd<>(mapperClass, customSqlExecution));
    }

}
