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
package org.activiti.engine.impl.interceptor;

/**
 * A listener that can be used to be notified of the closure of a {@link CommandContext}.
 * 
 * @author Joram Barrez
 */
public interface CommandContextCloseListener {

    void closing(CommandContext commandContext);

    void closed(CommandContext commandContext);

}
