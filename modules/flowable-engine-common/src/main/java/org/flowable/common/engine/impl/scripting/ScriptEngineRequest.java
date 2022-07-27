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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Request to execute a script in the scripting environment
 */
public class ScriptEngineRequest {


    protected String language;
    protected String script;
    protected VariableContainer variableContainer;
    protected List<Resolver> additionalResolver;
    protected boolean storeScriptVariables;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String language;
        private String script;

        private VariableContainer variableContainer;

        private List<Resolver> additionalResolver = new LinkedList<>();

        private boolean storeScriptVariables;

        protected Builder() {
        }

        /**
         * The script content for the given language.
         */

        public Builder setScript(String script) {
            this.script = script;
            return this;
        }

        /**
         * The script language for the script.
         */
        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }

        /**
         * The variable container used to create {@link Resolver}s for the script context.
         *
         * The variable container will be passed to {@link ResolverFactory} to create specialized Resolvers
         * for the specific VariableContainer implementations.
         */
        public Builder setVariableContainer(VariableContainer variableContainer) {
            this.variableContainer = variableContainer;
            return this;
        }

        /**
         * Whether to automatically store variables in script evaluation context
         * to the given variable container. Not recommended, to avoid variableContainer pollution.
         * Better to put the script evaluation result object to the variableContainer, if required.
         */
        public Builder setStoreScriptVariables(boolean storeScriptVariables) {
            this.storeScriptVariables = storeScriptVariables;
            return this;
        }

        /**
         * A list if additional Resolvers for the script context.
         * The resolvers take precedence over the resolvers created for the {@link #variableContainer}.
         * Useful to provide context objects to the scripting environment.
         */
        public Builder addAdditionalResolver(Resolver additionalResolver) {
            this.additionalResolver.add(additionalResolver);
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
                    additionalResolver != null ? additionalResolver : Collections.emptyList());
        }
    }

    private ScriptEngineRequest(String script, String language, VariableContainer variableContainer, boolean storeScriptVariables,
            List<Resolver> additionalResolver) {
        this.script = script;
        this.language = language;
        this.variableContainer = variableContainer;
        this.storeScriptVariables = storeScriptVariables;
        this.additionalResolver = additionalResolver;
    }

    /**
     * @see Builder#setLanguage(String)
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @see Builder#setScript(String)
     */
    public String getScript() {
        return script;
    }

    /**
     * @see Builder#setVariableContainer(VariableContainer)
     */
    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    /**
     * @see Builder#setStoreScriptVariables(boolean)
     */
    public boolean isStoreScriptVariables() {
        return storeScriptVariables;
    }

    /**
     * @see Builder#addAdditionalResolver(Resolver) 
     */
    public List<Resolver> getAdditionalResolver() {
        return additionalResolver;
    }
}