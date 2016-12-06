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
package org.flowable.idm.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.idm.api.NativeUserQuery;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.interceptor.CommandContext;
import org.flowable.idm.engine.impl.interceptor.CommandExecutor;

public class NativeUserQueryImpl extends AbstractNativeQuery<NativeUserQuery, User> implements NativeUserQuery {

  private static final long serialVersionUID = 1L;

  public NativeUserQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public NativeUserQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  // results ////////////////////////////////////////////////////////////////

  public List<User> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return commandContext.getUserEntityManager().findUsersByNativeQuery(parameterMap, firstResult, maxResults);
  }

  public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
    return commandContext.getUserEntityManager().findUserCountByNativeQuery(parameterMap);
  }

}