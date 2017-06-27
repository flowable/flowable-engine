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

package org.flowable.dmn.engine.impl.db;

import java.sql.Connection;

import org.flowable.engine.common.impl.db.AbstractNonCachingDbSqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DbSqlSession extends AbstractNonCachingDbSqlSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbSqlSession.class);

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory) {
        super(dbSqlSessionFactory);
    }

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
        super(dbSqlSessionFactory, connection, catalog, schema);
    }

    // schema operations
    // ////////////////////////////////////////////////////////

    public void dbSchemaCheckVersion() {
        LOGGER.debug("flowable dmn db schema check successful");
    }

    public void dbSchemaCreate() {

    }

    public void dbSchemaDrop() {

    }

}
