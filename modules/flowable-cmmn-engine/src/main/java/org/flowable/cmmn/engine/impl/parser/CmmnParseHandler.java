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
package org.flowable.cmmn.engine.impl.parser;

import java.util.Collection;

import org.flowable.cmmn.model.BaseElement;

/**
 *  Allows to hook into the parsing of one or more elements during the parsing of a CMMN case definition.
 *
 * Instances of this class can be injected into the {@link org.flowable.cmmn.engine.CmmnEngineConfiguration}
 * The handler will then be called whenever a CMMN element is parsed that matches the types returned by the
 *
 * @author Joram Barrez
 */
public interface CmmnParseHandler {

    /**
     * The types for which this handler must be called during case definition parsing.
     */
    Collection<Class<? extends BaseElement>> getHandledTypes();

    /**
     * The actual delegation method.
     * The parser will calls this method on a match with the {@link #getHandledTypes()} return value.
     */
    void parse(CmmnParser cmmnParser, CmmnParseResult cmmnParseResult, BaseElement element);

}
