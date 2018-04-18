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

package org.flowable.common.engine.api.delegate;

import java.lang.reflect.Method;

/**
 * Interface for pluggable functions that can be used in the EL expressions
 * 
 * @author Tijs Rademakers
 */
public interface FlowableFunctionDelegate {

    String prefix();

    String localName();

    Method functionMethod();

    Class<?> functionClass();
}
