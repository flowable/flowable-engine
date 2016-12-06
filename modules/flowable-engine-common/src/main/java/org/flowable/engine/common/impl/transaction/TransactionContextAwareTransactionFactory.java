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
package org.flowable.engine.common.impl.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransaction;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.flowable.engine.common.impl.cfg.BaseTransactionContext;

/**
 * A {@link TransactionFactory} implementation for 'dependent' engine
 * (so not the process engine, but the IDM/DMN/Form/... engine).
 * 
 * This implementation will switch depending on the current context:
 * 
 * If a transaction context is active, the dependent engine does not
 * need to create a transaction, and a regular {@link JdbcTransaction} is used.
 * Otherwise, the engine simply need to participate in the existing transaction,
 * and a {@link ManagedTransaction} is used. 
 * 
 * @author Joram Barrez
 */
public class TransactionContextAwareTransactionFactory<T> implements TransactionFactory {
  
  protected Class<T> transactionContextClass; // sadly, needed because of generics and type erasure ...
  
  protected ManagedTransactionFactory managedTransactionFactory;
  protected JdbcTransactionFactory jdbcTransactionFactory;
  
  public TransactionContextAwareTransactionFactory(Class<T> transactionContextClass) {
    this.transactionContextClass = transactionContextClass;

    this.jdbcTransactionFactory = new JdbcTransactionFactory();
    
    this.managedTransactionFactory = new ManagedTransactionFactory();
    Properties properties = new Properties();
    properties.put("closeConnection", "false");
    this.managedTransactionFactory.setProperties(properties);
  }

  @Override
  public void setProperties(Properties props) {
    if (isNonIdmTransactionContextActive()) {
      managedTransactionFactory.setProperties(props);
    } else {
      jdbcTransactionFactory.setProperties(props);
    }
  }

  @Override
  public Transaction newTransaction(Connection conn) {
    if (isNonIdmTransactionContextActive()) {
      return managedTransactionFactory.newTransaction(conn);
    } else {
      return jdbcTransactionFactory.newTransaction(conn);
    }
  }

  @Override
  public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
    if (isNonIdmTransactionContextActive()) {
      return managedTransactionFactory.newTransaction(dataSource, level, autoCommit);
    } else {
      return jdbcTransactionFactory.newTransaction(dataSource, level, autoCommit);
    }
  }
  
  @SuppressWarnings("rawtypes")
  protected boolean isNonIdmTransactionContextActive() {
    BaseTransactionContext transactionContext = TransactionContextHolder.getTransactionContext();
    if (transactionContext != null) {
      return transactionContext != null && !transactionContextClass.isInstance(transactionContext);
    } 
    return false;
  }

}
