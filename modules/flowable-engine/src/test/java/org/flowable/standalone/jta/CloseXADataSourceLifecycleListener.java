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

package org.flowable.standalone.jta;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 * Used in JPA-tests to close the XA-datasource after engine is closed, due to internal caching of datasource, independent of process-engine/spring-context.
 * 
 * @author Frederik Heremans
 */
public class CloseXADataSourceLifecycleListener implements ProcessEngineLifecycleListener {

    private PoolingDataSource dataSource;
    private BitronixTransactionManager transactionManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(CloseXADataSourceLifecycleListener.class);

    @Override
    public void onProcessEngineBuilt(ProcessEngine processEngine) {
        LOGGER.info("--------------------- Callback for engine start");
    }

    @Override
    public void onProcessEngineClosed(ProcessEngine processEngine) {
        LOGGER.info("--------------------- Callback for engine end");
        if (dataSource != null) {
            LOGGER.info("--------------------- Closing datasource");
            dataSource.close();
        }

        if (transactionManager != null) {
            transactionManager.shutdown();
        }
    }

    public void setDataSource(PoolingDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setTransactionManager(BitronixTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

}
