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
package org.flowable.bpmn.model;

/**
 * Contains relevant information for script evaluation.
 *
 * @author Arthur Hupka-Merle
 */
public class ScriptInfo extends BaseElement {

    protected String language;
    protected String resultVariable;
    protected String script;

    /**
     * The script language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * The script language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * The name of the result variable where the
     * script return value is written to.
     */
    public String getResultVariable() {
        return resultVariable;
    }

    /**
     * @see #getResultVariable
     */
    public void setResultVariable(String resultVariable) {
        this.resultVariable = resultVariable;
    }

    /**
     * The actual script payload in the provided language.
     */
    public String getScript() {
        return script;
    }

    /**
     * Set the script payload in the provided language.
     */
    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public ScriptInfo clone() {
        ScriptInfo clone = new ScriptInfo();
        clone.setLanguage(this.language);
        clone.setScript(this.script);
        clone.setResultVariable(this.resultVariable);
        return clone;
    }
}
