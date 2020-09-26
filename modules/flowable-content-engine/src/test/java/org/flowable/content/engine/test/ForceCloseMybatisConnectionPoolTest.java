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
package org.flowable.content.engine.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.datasource.pooled.PoolState;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.impl.cfg.StandaloneInMemContentEngineConfiguration;
import org.junit.jupiter.api.Test;

/**
 * @author Zheng Ji
 */
public class ForceCloseMybatisConnectionPoolTest {

    @Test
    public void testForceCloseMybatisConnectionPoolTrue() {

        // given
        // that the AbstractEngineConfiguration is configured with forceCloseMybatisConnectionPool = true
        StandaloneInMemContentEngineConfiguration standaloneInMemContentEngineConfiguration = new StandaloneInMemContentEngineConfiguration();
        standaloneInMemContentEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-content-" + this.getClass().getName());
        standaloneInMemContentEngineConfiguration.setForceCloseMybatisConnectionPool(true);

        ContentEngine contentEngine = standaloneInMemContentEngineConfiguration.buildContentEngine();

        PooledDataSource pooledDataSource = (PooledDataSource) standaloneInMemContentEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();
        assertThat(state.getIdleConnectionCount()).isPositive();

        // then
        // if the  engine is closed
        contentEngine.close();

        // the idle connections are closed
        assertThat(state.getIdleConnectionCount()).isZero();

    }

    @Test
    public void testForceCloseMybatisConnectionPoolFalse() {

        // given
        // that the AbstractEngineConfiguration is configured with forceCloseMybatisConnectionPool = false
        StandaloneInMemContentEngineConfiguration standaloneInMemContentEngineConfiguration = new StandaloneInMemContentEngineConfiguration();
        standaloneInMemContentEngineConfiguration.setJdbcUrl("jdbc:h2:mem:flowable-content-" + this.getClass().getName());
        standaloneInMemContentEngineConfiguration.setForceCloseMybatisConnectionPool(false);

        ContentEngine contentEngine = standaloneInMemContentEngineConfiguration.buildContentEngine();

        PooledDataSource pooledDataSource = (PooledDataSource) standaloneInMemContentEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();
        assertThat(state.getIdleConnectionCount()).isPositive();

        // then
        // if the  engine is closed
        contentEngine.close();

        // the idle connections are not closed
        assertThat(state.getIdleConnectionCount()).isPositive();
        pooledDataSource.forceCloseAll();
        assertThat(state.getIdleConnectionCount()).isZero();
    }

}
