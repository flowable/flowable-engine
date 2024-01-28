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
package org.flowable.common.engine.impl.aot;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.ClassPathResource;

/**
 * Register the necessary resource hints for the Flowable SQL resources.
 *
 * @author Filip Hrisafov
 */
public class FlowableMyBatisResourceHintsRegistrar {

    public static void registerMappingResources(String baseFolder, RuntimeHints runtimeHints, ClassLoader classLoader) {
        ResourceHints resourceHints = runtimeHints.resources();
        String mappingsPath = baseFolder + "/mappings.xml";
        ClassPathResource mappingsResource = new ClassPathResource(mappingsPath);
        resourceHints.registerResource(mappingsResource);
        try (InputStream mappingsStream = mappingsResource.getInputStream()) {
            XPathParser parser = createParser(mappingsStream);

            List<XNode> mappers = parser.evalNodes("/configuration/mappers/mapper");
            for (XNode mapper : mappers) {
                registerMapper(mapper.getStringAttribute("resource"), runtimeHints, classLoader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read mappings " + mappingsPath, e);
        }
    }

    public static void registerMapper(String mapperPath, RuntimeHints hints, ClassLoader classLoader) {
        ResourceHints resourceHints = hints.resources();
        ClassPathResource mapperResource = new ClassPathResource(mapperPath);
        resourceHints.registerResource(mapperResource);

        ReflectionHints reflectionHints = hints.reflection();
        MemberCategory[] memberCategories = MemberCategory.values();
        try (InputStream mapperStream = mapperResource.getInputStream()) {
            XPathParser parser = createParser(mapperStream);
            XNode mapper = parser.evalNode("/mapper");
            // The xpath resolving is similar like what MyBatis does in XMLMapperBuilder#parse
            for (XNode resultMap : mapper.evalNodes("/mapper/resultMap")) {
                String type = resultMap.getStringAttribute("type");
                if (type != null) {
                    reflectionHints.registerType(TypeReference.of(type), memberCategories);
                }
            }

            for (XNode statement : mapper.evalNodes("select|insert|update|delete")) {
                String parameterType = statement.getStringAttribute("parameterType");
                if (parameterType != null) {
                    if (parameterType.startsWith("org.flowable") || parameterType.startsWith("java.")) {
                        reflectionHints.registerType(TypeReference.of(parameterType), memberCategories);
                    } else if (parameterType.equals("map")) {
                        reflectionHints.registerType(Map.class, memberCategories);
                    }
                }

                String resultType = statement.getStringAttribute("resultType");
                if (resultType != null) {
                    if (resultType.equals("long")) {
                        reflectionHints.registerType(long.class, memberCategories);
                        reflectionHints.registerType(Long.class, memberCategories);
                    } else if (resultType.equals("string")) {
                        reflectionHints.registerType(String.class, memberCategories);
                    } else if (resultType.equals("map")) {
                        reflectionHints.registerType(HashMap.class, memberCategories);
                    }
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read mapper from " + mapperPath, e);
        }
    }

    protected static XPathParser createParser(InputStream stream) {
        return new XPathParser(stream, false, null, new XMLMapperEntityResolver());
    }

}
