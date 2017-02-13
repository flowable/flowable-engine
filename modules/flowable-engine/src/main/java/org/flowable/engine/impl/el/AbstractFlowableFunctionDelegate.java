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

package org.flowable.engine.impl.el;

import java.lang.reflect.Method;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.FlowableFunctionDelegate;

/**
 * Abstract class that can be used as a base class for pluggable functions that can be used in the EL expressions
 * 
 * @author Tijs Rademakers
 */
public abstract class AbstractFlowableFunctionDelegate implements FlowableFunctionDelegate {
  
  protected Method getNoParameterMethod() {
    try {
      return functionClass().getDeclaredMethod(localName());
    } catch (Exception e) {
      throw new FlowableException("Error getting method " + localName(), e);
    }
  }
  
  protected Method getSingleObjectParameterMethod() {
    try {
      return functionClass().getDeclaredMethod(localName(), Object.class);
    } catch (Exception e) {
      throw new FlowableException("Error getting method " + localName(), e);
    }
  }
  
  protected Method getTwoObjectParameterMethod() {
    try {
      return functionClass().getDeclaredMethod(localName(), Object.class, Object.class);
    } catch (Exception e) {
      throw new FlowableException("Error getting method " + localName(), e);
    }
  }
}
