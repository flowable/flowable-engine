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
package org.flowable.common.engine.impl.scripting;

import java.util.LinkedList;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Request to execute a script in the scripting environment.
 * Use {@link ScriptEngineRequest#builder()} to create and configure instances.
  */
public class ScriptEngineRequest {

    protected final String language;
    protected final String script;
    protected final VariableContainer variableContainer;
    protected final List<Resolver> additionalResolvers;
    protected final boolean storeScriptVariables;

    /**
     * @return a new Builder instance to create a {@link ScriptEngineRequest}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ScriptEngineRequest}.
     */
    public static class Builder {

        protected String language;
        protected String script;
        protected VariableContainer variableContainer;
        protected List<Resolver> additionalResolvers = new LinkedList<>();
        protected boolean storeScriptVariables;

        protected Builder() {
        }

        /**
         * The script content for the given language.
         */
        public Builder script(String script) {
            this.script = script;
            return this;
        }

        /**
         * The script language for the script.
         */
        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /**
         * The variable container used to create {@link Resolver}s for the script context.
         *
         * The variable container will be passed to {@link ResolverFactory} to create specialized Resolvers
         * for the specific VariableContainer implementations.
         */
        public Builder variableContainer(VariableContainer variableContainer) {
            this.variableContainer = variableContainer;
            return this;
        }

        /**
         * Automatically store variables from script evaluation context
         * to the given variable container. Not recommended, to avoid variableContainer pollution.
         * Better to put the script evaluation result object to the variableContainer, if required.
         */
        public Builder storeScriptVariables() {
            this.storeScriptVariables = true;
            return this;
        }

        /**
         * Adds additional resolver to the end of the list of resolvers.
         * The order of the resolvers matter, as the first resolver returning containsKey = true
         * will be used to resolve a variable during script execution.
         * The resolvers take precedence over the resolvers created for the {@link #variableContainer}.
         * Useful to provide context objects to the scripting environment.
         */
        public Builder additionalResolver(Resolver additionalResolver) {
            this.additionalResolvers.add(additionalResolver);
            return this;
        }

        public ScriptEngineRequest build() {
            if (script == null || script.isEmpty()) {
                throw new FlowableIllegalStateException("A script is required");
            }
            return new ScriptEngineRequest(script,
                    language,
                    variableContainer,
                    storeScriptVariables,
                    additionalResolvers);
        }
    }

    private ScriptEngineRequest(String script,
            String language,
            VariableContainer variableContainer,
            boolean storeScriptVariables,
            List<Resolver> additionalResolvers) {
        this.script = script;
        this.language = language;
        this.variableContainer = variableContainer;
        this.storeScriptVariables = storeScriptVariables;
        this.additionalResolvers = additionalResolvers;
    }

    /**
     * @see Builder#(String)
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @see Builder#(String)
     */
    public String getScript() {
        return script;
    }

    /**
     * @see Builder#variableContainer(VariableContainer)
     */
    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    /**
     * @see Builder#storeScriptVariables
     */
    public boolean isStoreScriptVariables() {
        return storeScriptVariables;
    }

    /**
     * @see Builder#additionalResolver(Resolver)
     */
    public List<Resolver> getAdditionalResolvers() {
        return additionalResolvers;
    }
}