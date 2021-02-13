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
package org.flowable.cmmn.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.javacrumbs.jsonunit.providers.Jackson2ObjectMapperProvider;

public class JavaTimeObjectMapperProvider implements Jackson2ObjectMapperProvider {

    private final ObjectMapper objectMapper;
    private final ObjectMapper lenientObjectMapper;

    public JavaTimeObjectMapperProvider() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        lenientObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        lenientObjectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        lenientObjectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        lenientObjectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    @Override
    public ObjectMapper getObjectMapper(boolean lenient) {
        return lenient ? lenientObjectMapper : objectMapper;
    }

}
