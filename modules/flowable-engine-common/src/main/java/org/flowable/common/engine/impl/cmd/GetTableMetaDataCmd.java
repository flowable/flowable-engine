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
package org.flowable.common.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class GetTableMetaDataCmd implements Command<TableMetaData>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected String tableName;
    protected String engineType;

    public GetTableMetaDataCmd(String tableName, String engineType) {
        this.tableName = tableName;
        this.engineType = engineType;
    }

    @Override
    public TableMetaData execute(CommandContext commandContext) {
        if (tableName == null) {
            throw new FlowableIllegalArgumentException("tableName is null");
        }
        return commandContext.getEngineConfigurations().get(engineType).getTableDataManager().getTableMetaData(tableName);
    }

}
