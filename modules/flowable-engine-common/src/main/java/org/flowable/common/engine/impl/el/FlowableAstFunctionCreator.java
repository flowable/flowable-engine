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
package org.flowable.common.engine.impl.el;

import java.util.Collection;

import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstFunction;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstParameters;

/**
 * @author Filip Hrisafov
 */
public interface FlowableAstFunctionCreator {

    /**
     * The names of the functions that this creator can create.
     */
    Collection<String> getFunctionNames();

    /**
     * Create an {@link AstFunction} based on the provided name, index, parameters.
     * Potentially creating new parameters to enhance the function.
     *
     * @param name the name of the function
     * @param index the index
     * @param parameters the parameters for the function
     * @param varargs whether varargs is supported
     * @param parser the parser for potentially creating identifiers
     */
    AstFunction createFunction(String name, int index, AstParameters parameters, boolean varargs, FlowableExpressionParser parser);

}
