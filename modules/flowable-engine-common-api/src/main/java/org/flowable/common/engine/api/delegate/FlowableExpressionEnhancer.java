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

/**
 * Instances of this interface can be registered by engines that use expressions through an expression manager.
 * 
 * The instances will be called in the order as they are configured and can be used to change the text of the 
 * expression before the actual {@link Expression} is created from it.
 * 
 * @author Joram Barrez
 */
public interface FlowableExpressionEnhancer {

    String enhance(String expressionText);

}
