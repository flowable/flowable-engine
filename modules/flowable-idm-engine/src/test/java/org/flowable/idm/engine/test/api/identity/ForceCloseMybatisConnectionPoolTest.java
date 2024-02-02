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
package org.flowable.idm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.datasource.pooled.PoolState;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.impl.cfg.StandaloneInMemIdmEngineConfiguration;
import org.junit.jupiter.api.Test;

/**
 * @author Zheng Ji
 */
public class ForceCloseMybatisConnectionPoolTest {

    @Test
    public void testForceCloseMybatisConnectionPoolTrue() {

        // given
        // that the AbstractEngineConfiguration is configured with forceCloseMybatisConnectionPool = true
        StandaloneInMemIdmEngineConfiguration standaloneInMemIdmEngineConfiguration = new StandaloneInMemIdmEngineConfiguration();
        standaloneInMemIdmEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-idm-" + this.getClass().getName());
        standaloneInMemIdmEngineConfiguration.setForceCloseMybatisConnectionPool(true);
        standaloneInMemIdmEngineConfiguration.setDatabaseSchemaUpdate("drop-create");

        IdmEngine idmEngine = standaloneInMemIdmEngineConfiguration.buildIdmEngine();

        PooledDataSource pooledDataSource = (PooledDataSource) standaloneInMemIdmEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();
        assertThat(state.getIdleConnectionCount()).isPositive();

        // then
        // if the  engine is closed
        idmEngine.close();

        // the idle connections are closed
        assertThat(state.getIdleConnectionCount()).isZero();
    }

    @Test
    public void testForceCloseMybatisConnectionPoolFalse() {

        // given
        // that the AbstractEngineConfiguration is configured with forceCloseMybatisConnectionPool = false
        StandaloneInMemIdmEngineConfiguration standaloneInMemIdmEngineConfiguration = new StandaloneInMemIdmEngineConfiguration();
        standaloneInMemIdmEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-idm-" + this.getClass().getName());
        standaloneInMemIdmEngineConfiguration.setForceCloseMybatisConnectionPool(false);
        standaloneInMemIdmEngineConfiguration.setDatabaseSchemaUpdate("drop-create");

        IdmEngine idmEngine = standaloneInMemIdmEngineConfiguration.buildIdmEngine();

        PooledDataSource pooledDataSource = (PooledDataSource) standaloneInMemIdmEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();
        assertThat(state.getIdleConnectionCount()).isPositive();

        // then
        // if the  engine is closed
        idmEngine.close();

        // the idle connections are not closed
        assertThat(state.getIdleConnectionCount()).isPositive();

        pooledDataSource.forceCloseAll();
        assertThat(state.getIdleConnectionCount()).isZero();
    }

}
