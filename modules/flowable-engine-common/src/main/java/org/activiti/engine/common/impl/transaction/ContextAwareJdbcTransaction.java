
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
package org.activiti.engine.common.impl.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;

/**
 * Extension of the regular {@link JdbcTransaction} of Mybatis.
 * The main difference is that the threadlocal on {@link ConnectionHolder}
 * gets set/cleared when the connection is opened/closed.
 * 
 * This class will be used by the Process Engine when running in 
 * 'standalone' mode (i.e. Mybatis directly vs in a transaction managed environment).
 * 
 * @author Joram Barrez
 */
public class ContextAwareJdbcTransaction extends JdbcTransaction {
  
  protected boolean connectionStored;

  public ContextAwareJdbcTransaction(Connection connection) {
    super(connection);
  }
  
  public ContextAwareJdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
    super(ds, desiredLevel, desiredAutoCommit);
  }  
  
  protected void openConnection() throws SQLException {
    super.openConnection();
    
    if (!connectionStored) {
      ConnectionHolder.setConnection(super.connection);
    }
  }
  
  @Override
  public void close() throws SQLException {
    ConnectionHolder.clear();
    super.close();
  }

}
