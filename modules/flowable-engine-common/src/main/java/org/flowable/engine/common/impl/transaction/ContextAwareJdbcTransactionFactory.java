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

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

/**
  * Typically used for the process engine, as it's the 'driver' of other engine 
  * (i.e. it call other engines like idm/form/dmn).
  * 
  * @see ContextAwareJdbcTransaction
  *  
  * @author Joram Barrez
  */
public class ContextAwareJdbcTransactionFactory extends JdbcTransactionFactory {

  @Override
  public Transaction newTransaction(Connection conn) {
    return new ContextAwareJdbcTransaction(conn);
  }
  
  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    return new ContextAwareJdbcTransaction(ds, level, autoCommit);
  }
  
}
