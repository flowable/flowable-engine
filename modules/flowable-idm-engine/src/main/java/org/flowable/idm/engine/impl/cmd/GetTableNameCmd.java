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
package org.flowable.idm.engine.impl.cmd;

import static org.flowable.idm.engine.impl.util.CommandContextUtil.getDbSqlSession;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.engine.impl.db.EntityToTableMap;

public class GetTableNameCmd implements Command<String>, Serializable {

    private static final long serialVersionUID = 1L;

    private Class<?> entityClass;

    public GetTableNameCmd(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public String execute(CommandContext commandContext) {
        if (entityClass == null) {
            throw new FlowableIllegalArgumentException("entityClass is null");
        }

        String databaseTablePrefix = getDbSqlSession(commandContext).getDbSqlSessionFactory().getDatabaseTablePrefix();
        String tableName = EntityToTableMap.getTableName(entityClass);

        return databaseTablePrefix + tableName;
    }

}
