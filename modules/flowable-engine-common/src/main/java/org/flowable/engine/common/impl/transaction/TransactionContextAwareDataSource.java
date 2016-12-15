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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * A wrapper for a {@link DataSource}, to be used by a 'dependent' engine 
 * (e.g. IDM/DMN/Form, NOT the process engine). The process engine 
 * will have created a valid transaction context and typically opened a {@link Connection}.
 * 
 * This implementation will check for such an active transaction context
 * and reuse an existing context and connection, such that the transaction
 * and connection is shared between engines.
 * Note that this only applies to the 'standalone mybatis' engine setup.
 * 
 * @author Joram Barrez
 */
public class TransactionContextAwareDataSource implements DataSource {
  
  protected DataSource dataSource;
  
  public TransactionContextAwareDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }
  
  @Override
  public Connection getConnection() throws SQLException {
    if (TransactionContextHolder.isTransactionContextActive()) {
      Connection connection = ConnectionHolder.get();
      if (connection != null) {
        return connection;
      }
    } 
    
    return dataSource.getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    if (TransactionContextHolder.isTransactionContextActive()) {
      Connection connection = ConnectionHolder.get();
      if (connection != null) {
        return connection;
      }
    } 
    return dataSource.getConnection(username, password);
  }
  
  /*
   * Following methods simply delegate to the wrapper datasource.
   */

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

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return dataSource.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return dataSource.isWrapperFor(iface);
  }

}
