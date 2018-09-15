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
package org.flowable.common.engine.impl.el.function;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.FlowableExpressionEnhancer;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;

/**
 * Implementations of this interface are both 
 * 
 * - a {@link FlowableFunctionDelegate}, allowing to define a custom function to be used in an {@link Expression}
 * - a {@link FlowableExpressionEnhancer}, allowing to use a shorthand version of a function that gets expanded
 *   by enhancing the expression string before it gets transformed to an {@link Expression} instance.
 *   
 * This simplifies registration with an engine configuration, as only one instance needs to be added instead of keeping both in sync. 
 * 
 * @author Joram Barrez
 */
public interface FlowableShortHandExpressionFunction extends FlowableFunctionDelegate, FlowableExpressionEnhancer {

}
