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
package org.flowable.standalone.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableWrongDbException;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.test.AbstractTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class ProcessEngineInitializationTest extends AbstractTestCase {

    @Test
    public void testNoTables() {
        assertThatThrownBy(() -> ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("org/flowable/standalone/initialization/notables.flowable.cfg.xml").buildProcessEngine())
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("no flowable tables in db");
    }

    @Test
    public void testVersionMismatch() {
        // first create the schema
        ProcessEngineImpl processEngine = (ProcessEngineImpl) ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("org/flowable/standalone/initialization/notables.flowable.cfg.xml")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP).buildProcessEngine();

        // then update the version to something that is different to the library version
        DbSqlSessionFactory dbSqlSessionFactory = (DbSqlSessionFactory) processEngine.getProcessEngineConfiguration().getSessionFactories()
                .get(DbSqlSession.class);
        SqlSessionFactory sqlSessionFactory = dbSqlSessionFactory.getSqlSessionFactory();
        SqlSession sqlSession = sqlSessionFactory.openSession();
        boolean success = false;
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "schema.version");
            parameters.put("value", "25.7");
            parameters.put("revision", 1);
            parameters.put("newRevision", 2);
            sqlSession.update("updateProperty", parameters);
            success = true;
        } catch (Exception e) {
            throw new FlowableException("couldn't update db schema version", e);
        } finally {
            if (success) {
                sqlSession.commit();
            } else {
                sqlSession.rollback();
            }
            sqlSession.close();
        }

        // now we can see what happens if when a process engine is being
        // build with a version mismatch between library and db tables
        Consumer<FlowableWrongDbException> flowableWrongDbExceptionRequirements = flowableWrongDbException -> {
            assertThat(flowableWrongDbException.getDbVersion()).isEqualTo("25.7");
            assertThat(flowableWrongDbException.getLibraryVersion()).isEqualTo(ProcessEngine.VERSION);
        };
        assertThatThrownBy(() -> ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("org/flowable/standalone/initialization/notables.flowable.cfg.xml")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE).buildProcessEngine())
                .isInstanceOfSatisfying(FlowableWrongDbException.class, flowableWrongDbExceptionRequirements)
                .hasMessageContaining("version mismatch");

        // closing the original process engine to drop the db tables
        processEngine.close();
    }
}
