/* Licensed under the Apache License, Version 20.0 (the "License");
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

package org.flowable.engine.test.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

import junit.framework.TestCase;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class DatabaseTablePrefixTest extends TestCase {

    public void testPerformDatabaseSchemaOperationCreate() throws Exception {

        DataSource dataSource = createDataSourceAndSchema();

        // configure & build two different process engines, each having a
        // separate table prefix
        ProcessEngineConfigurationImpl config1 = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setDataSource(dataSource)
                .setDatabaseSchemaUpdate("NO_CHECK"); // disable auto create/drop schema
        config1.setDatabaseTablePrefix("SCHEMA1.");
        config1.setValidateFlowable5EntitiesEnabled(false);
        config1.getPerformanceSettings().setValidateExecutionRelationshipCountConfigOnBoot(false);
        config1.getPerformanceSettings().setValidateTaskRelationshipCountConfigOnBoot(false);
        ProcessEngine engine1 = config1.buildProcessEngine();

        ProcessEngineConfigurationImpl config2 = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setDataSource(dataSource)
                .setDatabaseSchemaUpdate("NO_CHECK"); // disable auto create/drop schema
        config2.setDatabaseTablePrefix("SCHEMA2.");
        config2.setValidateFlowable5EntitiesEnabled(false);
        config2.getPerformanceSettings().setValidateExecutionRelationshipCountConfigOnBoot(false);
        config2.getPerformanceSettings().setValidateTaskRelationshipCountConfigOnBoot(false);
        ProcessEngine engine2 = config2.buildProcessEngine();

        // create the tables in SCHEMA1
        Connection connection = dataSource.getConnection();
        connection.createStatement().execute("set schema SCHEMA1");
        engine1.getManagementService().databaseSchemaUpgrade(connection, "", "SCHEMA1");
        connection.close();

        // create the tables in SCHEMA2
        connection = dataSource.getConnection();
        connection.createStatement().execute("set schema SCHEMA2");
        engine2.getManagementService().databaseSchemaUpgrade(connection, "", "SCHEMA2");
        connection.close();

        // if I deploy a process to one engine, it is not visible to the other
        // engine:
        try {
            engine1.getRepositoryService().createDeployment().addClasspathResource("org/flowable/engine/test/db/oneJobProcess.bpmn20.xml").deploy();

            assertEquals(1, engine1.getRepositoryService().createDeploymentQuery().count());
            assertEquals(0, engine2.getRepositoryService().createDeploymentQuery().count());

        } finally {
            engine1.close();
            engine2.close();
        }
    }
    
    protected DataSource createDataSourceAndSchema() throws SQLException {
        // both process engines will be using this datasource.
        PooledDataSource pooledDataSource = new PooledDataSource(ReflectUtil.getClassLoader(), 
                "org.h2.Driver", "jdbc:h2:mem:flowable-db-table-prefix-test;DB_CLOSE_DELAY=1000", "sa", "");

        // create two schemas is the database
        Connection connection = pooledDataSource.getConnection();
        connection.createStatement().execute("drop schema if exists SCHEMA1 cascade");
        connection.createStatement().execute("drop schema if exists SCHEMA2 cascade");
        connection.createStatement().execute("create schema SCHEMA1");
        connection.createStatement().execute("create schema SCHEMA2");
        connection.close();
        return pooledDataSource;
    }
    
    
    public void testProcessEngineReboot() throws Exception {
        
        ProcessEngine processEngine1 = null;
        ProcessEngine processEngine2 = null;
        
        try {
            
            createDataSourceAndSchema(); // Creating the schemas
        
            processEngine1 = createProcessEngine("SCHEMA1");
            processEngine1.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/engine/test/db/oneJobProcess.bpmn20.xml").deploy();
            assertEquals(1, processEngine1.getRepositoryService().createDeploymentQuery().count());
            
            // Boot second engine on other schema. Shouldn't be able to see the data
            processEngine2 = createProcessEngine("SCHEMA2");
            assertEquals(0, processEngine2.getRepositoryService().createDeploymentQuery().count());
            
            // Reboot both engines. The results should still be the same as before
            processEngine1.close();
            processEngine2.close();
            
            processEngine1 = createProcessEngine("SCHEMA1");
            processEngine2 = createProcessEngine("SCHEMA2");
            
            assertEquals(1, processEngine1.getRepositoryService().createDeploymentQuery().count());
            assertEquals(0, processEngine2.getRepositoryService().createDeploymentQuery().count());
            
        } finally {
            if (processEngine1 != null) {
                processEngine1.close();
            }
            if (processEngine2 != null) {
                processEngine2.close();
            }
        }
    }

    protected ProcessEngine createProcessEngine(String schema) {
        return ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:flowable-db-table-prefix-test;DB_CLOSE_DELAY=-1;SCHEMA=" + schema)
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setTablePrefixIsSchema(true)
                .setDatabaseTablePrefix(schema + ".")
                .buildProcessEngine();
    }
    
  
}
