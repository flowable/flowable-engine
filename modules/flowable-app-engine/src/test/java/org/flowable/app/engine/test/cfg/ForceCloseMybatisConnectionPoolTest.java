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
package org.flowable.app.engine.test.cfg;

import org.apache.ibatis.datasource.pooled.PoolState;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.impl.cfg.StandaloneInMemAppEngineConfiguration;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * @author Zheng Ji
 */
public class ForceCloseMybatisConnectionPoolTest {


    @Test
    public void testForceCloseMybatisConnectionPoolTrue() {

        // given
        // that the AbstractEngineConfiguration is configured with forceCloseMybatisConnectionPool = true
        StandaloneInMemAppEngineConfiguration appEngineConfiguration =  new StandaloneInMemAppEngineConfiguration();
        appEngineConfiguration.setForceCloseMybatisConnectionPool(true);

        AppEngine appEngine = appEngineConfiguration.buildAppEngine();


        PooledDataSource pooledDataSource = (PooledDataSource) appEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();


        // then
        // if the  engine is closed
        appEngine.close();

        // the idle connections are closed
        assertTrue(state.getIdleConnectionCount() == 0);

    }

    @Test
    public void testForceCloseMybatisConnectionPoolFalse() {

        // given
        // that the AbstractEngineConfiguration is configured with forceCloseMybatisConnectionPool = false
        StandaloneInMemAppEngineConfiguration appEngineConfiguration =  new StandaloneInMemAppEngineConfiguration();
        appEngineConfiguration.setForceCloseMybatisConnectionPool(false);

        AppEngine appEngine = appEngineConfiguration.buildAppEngine();

        PooledDataSource pooledDataSource = (PooledDataSource) appEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();
        int idleConnections = state.getIdleConnectionCount();


        // then
        // if the  engine is closed
        appEngine.close();

        // the idle connections are not closed
        assertEquals(state.getIdleConnectionCount(), idleConnections);

        pooledDataSource.forceCloseAll();

        assertTrue(state.getIdleConnectionCount() == 0);
    }

}
