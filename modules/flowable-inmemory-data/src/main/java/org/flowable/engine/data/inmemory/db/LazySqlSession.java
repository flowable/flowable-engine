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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * A Lazy {@link SqlSession} that opens a real JDBC connection only when any
 * operation is performed.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class LazySqlSession extends DefaultSqlSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(LazySqlSession.class);

    private final Executor executor;

    private Connection connection = null;

    private boolean closed = false;

    private boolean autoCommit;

    private boolean readOnly;

    private int transactionIsolation;

    private int holdability;

    public LazySqlSession(Configuration configuration, Executor executor, boolean autoCommit, boolean readOnly, int isolationLevel, int holdability) {
        super(configuration, executor, autoCommit);
        this.executor = executor;
        this.autoCommit = autoCommit;
        this.readOnly = readOnly;
        this.transactionIsolation = isolationLevel;
        this.holdability = holdability;
    }

    @Override
    public Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        // Create a proxy for the actual connection
        connection = (Connection) Proxy.newProxyInstance(ConnectionProxy.class.getClassLoader(), new Class< ? >[] { ConnectionProxy.class },
                        new LazyConnectionProxyInvocationHandler(this));
        return connection;
    }

    // This is loosely based on Spring JDBC LazyConnectionInvocationHandler
    private static class LazyConnectionProxyInvocationHandler implements InvocationHandler {

        private final LazySqlSession session;

        @Nullable
        private Connection connection = null;

        public LazyConnectionProxyInvocationHandler(LazySqlSession lazySqlSession) {
            this.session = lazySqlSession;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
            case "equals":
                return (proxy == args[0]);
            case "hashCode":
                return System.identityHashCode(proxy);
            case "getRealConnection":
                return getRealConnection(method.getName());
            case "unwrap":
                if (((Class< ? >) args[0]).isInstance(proxy)) {
                    return proxy;
                }
                break;
            case "isWrapperFor":
                if (((Class< ? >) args[0]).isInstance(proxy)) {
                    return true;
                }
                break;
            }

            if (connection == null) {
                // No real connection established
                switch (method.getName()) {
                case "toString":
                    return "LazyConnectionProxy[dataSource='" + getDataSource() + "']";

                // Setters and getters that do not need a connection
                case "setAutoCommit":
                    session.autoCommit = (boolean) args[0];
                    return null;
                case "getAutoCommit":
                    return session.autoCommit;
                case "setReadOnly":
                    session.readOnly = (boolean) args[0];
                    return null;
                case "isReadOnly":
                    return session.readOnly;
                case "setTransactionIsolation":
                    session.transactionIsolation = (int) args[0];
                    return null;
                case "getTransactionIsolation":
                    return session.transactionIsolation;
                case "setHoldability":
                    session.holdability = (int) args[0];
                    return null;
                case "getHoldability":
                    return session.holdability;

                // methods that can be ignored until a connection is created
                case "commit":
                case "rollback":
                case "getWarnings":
                case "clearWarnings":
                    return null;

                // close without creating a real connection
                case "close":
                    session.closed = true;
                    return null;
                case "isClosed":
                    return session.closed;
                default:
                    if (session.closed) {
                        throw new SQLException("Connection is closed");
                    }
                    // Anything else will use the real connection
                }
            }

            try {
                return method.invoke(getRealConnection(method.getName()), args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private DataSource getDataSource() {
            return session.getConfiguration().getEnvironment().getDataSource();
        }

        private Connection getRealConnection(String method) throws SQLException {
            if (connection == null) {
                LOGGER.debug("Opening new JDBC connection to DataSource '{}', transaction '{}' (method '{}')", getDataSource(),
                                session.executor.getTransaction(), method);

                connection = session.executor.getTransaction().getConnection();

                if (session.readOnly != connection.isReadOnly()) {
                    connection.setReadOnly(session.readOnly);
                }
                if (session.autoCommit != connection.getAutoCommit()) {
                    connection.setAutoCommit(session.autoCommit);
                }
                if (session.transactionIsolation != connection.getTransactionIsolation()) {
                    connection.setTransactionIsolation(session.transactionIsolation);
                }
                if (session.holdability != connection.getHoldability()) {
                    connection.setHoldability(session.holdability);
                }
            } else {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Using existing JDBC connection '{}' to DataSource '{}', transaction '{}' (method '{}')", connection, getDataSource(),
                                    session.executor.getTransaction(), method);
                }
            }
            return connection;
        }
    }
}