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
package org.activiti.form.engine.impl;

import java.util.List;
import java.util.Map;

import org.activiti.form.api.FormDefinition;
import org.activiti.form.api.NativeFormDefinitionQuery;
import org.activiti.form.engine.impl.interceptor.CommandContext;
import org.activiti.form.engine.impl.interceptor.CommandExecutor;

public class NativeFormDefinitionQueryImpl extends AbstractNativeQuery<NativeFormDefinitionQuery, FormDefinition> implements NativeFormDefinitionQuery {

  private static final long serialVersionUID = 1L;

  public NativeFormDefinitionQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public NativeFormDefinitionQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  // results ////////////////////////////////////////////////////////////////

  public List<FormDefinition> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return commandContext.getFormDefinitionEntityManager().findFormDefinitionsByNativeQuery(parameterMap, firstResult, maxResults);
  }

  public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
    return commandContext.getFormDefinitionEntityManager().findFormDefinitionCountByNativeQuery(parameterMap);
  }

}
