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
package org.activiti.engine.impl.transaction;

import java.sql.Connection;

/**
 * Simple wrapper for storing the {@link Connection} in a threadlocal object.
 * 
 * @author Joram Barrez
 */
public class ConnectionHolder {
  
  protected static ThreadLocal<Connection> connectionHolder = new ThreadLocal<Connection>();
  
  public static Connection get() {
    return connectionHolder.get();
  }
  
  public static void setConnection(Connection connection) {
    connectionHolder.set(connection);
  }
  
  public static void clear() {
    connectionHolder.remove();
  }

}
