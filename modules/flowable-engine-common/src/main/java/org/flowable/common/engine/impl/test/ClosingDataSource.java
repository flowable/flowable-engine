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
package org.flowable.common.engine.impl.test;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.flowable.common.engine.api.Engine;
import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.slf4j.LoggerFactory;

/**
 * A {@link DataSource} implementation for test purposes that wraps another {@link DataSource}
 * and makes sure that the {@link DataSource} is closed when a Flowable engine gets closed.
 *
 * @author Joram Barrez
 */
public class ClosingDataSource implements DataSource, EngineLifecycleListener {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ClosingDataSource.class);

    protected DataSource dataSource;

    public ClosingDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onEngineBuilt(Engine engine) {
        // Nothing to do
    }

    @Override
    public void onEngineClosed(Engine engine) {
        if (dataSource instanceof Closeable) {
            try {
                LOGGER.info("About to close dataSource");
                ((Closeable) dataSource).close();
                LOGGER.info("DataSource closed");
            } catch (IOException e) {
                LOGGER.warn("Exception while closing dataSource", e);
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return dataSource.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

}
