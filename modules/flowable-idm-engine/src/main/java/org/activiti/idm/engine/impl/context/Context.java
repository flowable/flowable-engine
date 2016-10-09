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

package org.activiti.idm.engine.impl.context;

import java.util.Stack;

import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class Context {

  protected static ThreadLocal<Stack<CommandContext>> commandContextThreadLocal = new ThreadLocal<Stack<CommandContext>>();
  protected static ThreadLocal<Stack<IdmEngineConfiguration>> formEngineConfigurationStackThreadLocal = new ThreadLocal<Stack<IdmEngineConfiguration>>();
  
  public static CommandContext getCommandContext() {
    Stack<CommandContext> stack = getStack(commandContextThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setCommandContext(CommandContext commandContext) {
    getStack(commandContextThreadLocal).push(commandContext);
  }

  public static void removeCommandContext() {
    getStack(commandContextThreadLocal).pop();
  }

  public static IdmEngineConfiguration getFormEngineConfiguration() {
    Stack<IdmEngineConfiguration> stack = getStack(formEngineConfigurationStackThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setFormEngineConfiguration(IdmEngineConfiguration formEngineConfiguration) {
    getStack(formEngineConfigurationStackThreadLocal).push(formEngineConfiguration);
  }

  public static void removeFormEngineConfiguration() {
    getStack(formEngineConfigurationStackThreadLocal).pop();
  }
  
  protected static <T> Stack<T> getStack(ThreadLocal<Stack<T>> threadLocal) {
    Stack<T> stack = threadLocal.get();
    if (stack == null) {
      stack = new Stack<T>();
      threadLocal.set(stack);
    }
    return stack;
  }
}
