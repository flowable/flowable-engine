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
package org.flowable.cmmn.test.cfg;

import org.apache.ibatis.datasource.pooled.PoolState;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
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
        StandaloneInMemCmmnEngineConfiguration standaloneInMemCmmnEngineConfiguration =  new StandaloneInMemCmmnEngineConfiguration();
        standaloneInMemCmmnEngineConfiguration.setForceCloseMybatisConnectionPool(true);

        CmmnEngine cmmnEngine = standaloneInMemCmmnEngineConfiguration.buildCmmnEngine();


        PooledDataSource pooledDataSource = (PooledDataSource) standaloneInMemCmmnEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();


        // then
        // if the  engine is closed
        cmmnEngine.close();

        // the idle connections are closed
        assertTrue(state.getIdleConnectionCount() == 0);

    }

    @Test
    public void testForceCloseMybatisConnectionPoolFalse() {

        // given
        // that the AbstractEngineConfiguration is configured with forceCloseMybatisConnectionPool = false
        StandaloneInMemCmmnEngineConfiguration standaloneInMemCmmnEngineConfiguration =  new StandaloneInMemCmmnEngineConfiguration();
        standaloneInMemCmmnEngineConfiguration.setForceCloseMybatisConnectionPool(false);

        CmmnEngine cmmnEngine = standaloneInMemCmmnEngineConfiguration.buildCmmnEngine();

        PooledDataSource pooledDataSource = (PooledDataSource) standaloneInMemCmmnEngineConfiguration.getDataSource();
        PoolState state = pooledDataSource.getPoolState();
        int idleConnections = state.getIdleConnectionCount();


        // then
        // if the  engine is closed
        standaloneInMemCmmnEngineConfiguration.close();

        // the idle connections are not closed
        assertEquals(state.getIdleConnectionCount(), idleConnections);

        pooledDataSource.forceCloseAll();

        assertTrue(state.getIdleConnectionCount() == 0);
    }

}
