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
package org.flowable.engine.data.inmemory.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;

/**
 * DbSqlSessionFactory for in-memory data managers.
 * <p>
 * Provides MemoryDbSqlSessions with simple transaction support for the non-sql
 * DataManagers
 * <p>
 * Supports lazy SQL sessions which open a real JDBC connection only when needed
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryDbSqlSessionFactory extends DbSqlSessionFactory {

    private boolean useLazySessions;

    public MemoryDbSqlSessionFactory(boolean usePrefixId, boolean useLazySessions) {
        super(usePrefixId);
        this.useLazySessions = useLazySessions;
    }

    @Override
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        if (!useLazySessions) {
            super.setSqlSessionFactory(sqlSessionFactory);
            return;
        }

        // Obtain a connection from the parent factory to get defaults for
        // LazySqlSession proxy connections
        try (Connection connection = sqlSessionFactory.openSession().getConnection()) {
            this.sqlSessionFactory = new LazySqlSessionFactory(sqlSessionFactory, connection.isReadOnly(), connection.getTransactionIsolation(),
                            connection.getHoldability());
        } catch (SQLException e) {
            throw new FlowableException("Unable to obtain database connection", e);
        }
    }
}
