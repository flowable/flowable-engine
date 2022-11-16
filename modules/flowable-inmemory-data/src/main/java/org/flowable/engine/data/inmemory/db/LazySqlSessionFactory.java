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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

/**
 * A Lazy {@link SqlSessionFactory} that opens a real JDBC connection only when
 * needed.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class LazySqlSessionFactory implements SqlSessionFactory {

    private final SqlSessionFactory parent;

    private boolean defaultReadOnly;

    private int defaultIsolationLevel;

    private int defaultHoldability;

    public LazySqlSessionFactory(SqlSessionFactory sqlSessionFactory, boolean readOnly, int isolationLevel, int holdability) {
        this.parent = sqlSessionFactory;
        this.defaultReadOnly = readOnly;
        this.defaultIsolationLevel = isolationLevel;
        this.defaultHoldability = holdability;
    }

    @Override
    public SqlSession openSession() {
        return openSessionFromDataSource(parent.getConfiguration().getDefaultExecutorType(), null, false);
    }

    @Override
    public SqlSession openSession(boolean autoCommit) {
        return openSessionFromDataSource(parent.getConfiguration().getDefaultExecutorType(), null, autoCommit);
    }

    @Override
    public SqlSession openSession(ExecutorType execType) {
        return openSessionFromDataSource(execType, null, false);
    }

    @Override
    public SqlSession openSession(TransactionIsolationLevel level) {
        return openSessionFromDataSource(parent.getConfiguration().getDefaultExecutorType(), level, false);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
        return openSessionFromDataSource(execType, level, false);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
        return openSessionFromDataSource(execType, null, autoCommit);
    }

    @Override
    public SqlSession openSession(Connection connection) {
        return parent.openSession(connection);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, Connection connection) {
        return parent.openSession(execType, connection);
    }

    @Override
    public Configuration getConfiguration() {
        return parent.getConfiguration();
    }

    private LazySqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
        Transaction tx = null;
        try {
            final Environment environment = parent.getConfiguration().getEnvironment();
            final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
            tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
            final Executor executor = parent.getConfiguration().newExecutor(tx, execType);
            return new LazySqlSession(parent.getConfiguration(), executor, autoCommit, this.defaultReadOnly, this.defaultIsolationLevel,
                            this.defaultHoldability);
        } catch (Exception e) {
            closeTransaction(tx); // may have fetched a connection so lets
                                  // call close()
            throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
        if (environment == null || environment.getTransactionFactory() == null) {
            return new ManagedTransactionFactory();
        }
        return environment.getTransactionFactory();
    }

    private void closeTransaction(Transaction tx) {
        if (tx != null) {
            try {
                tx.close();
            } catch (SQLException ignore) {
                // Intentionally ignore. Prefer previous error.
            }
        }
    }

}