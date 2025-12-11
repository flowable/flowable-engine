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
package org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast;

import java.util.List;

import org.flowable.common.engine.impl.de.odysseus.el.tree.Bindings;
import org.flowable.common.engine.impl.javax.el.ELContext;

/**
 * Lambda parameters node. Represents the parameter list of a lambda expression.
 *
 * @author Filip Hrisafov
 */
public class AstLambdaParameters extends AstRightValue {
    private final List<String> parameterNames;

    public AstLambdaParameters(List<String> parameterNames) {
        this.parameterNames = parameterNames;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    @Override
    public void appendStructure(StringBuilder builder, Bindings bindings) {
        if (parameterNames.size() == 1) {
            builder.append(parameterNames.get(0));
        } else {
            builder.append("(");
            for (int i = 0; i < parameterNames.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(parameterNames.get(i));
            }
            builder.append(")");
        }
    }

    @Override
    public Object eval(Bindings bindings, ELContext context) {
        // Lambda parameters don't evaluate to a value
        return parameterNames;
    }

    @Override
    public int getCardinality() {
        return 0;
    }

    @Override
    public AstNode getChild(int i) {
        return null;
    }

    @Override
    public String toString() {
        return parameterNames.size() == 1 ? parameterNames.get(0) : parameterNames.toString();
    }
}
