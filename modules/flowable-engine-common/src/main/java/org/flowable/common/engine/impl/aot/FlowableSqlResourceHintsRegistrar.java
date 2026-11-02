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

import java.util.stream.Stream;

import org.springframework.aot.hint.ResourceHints;

/**
 * Register the necessary resource hints for the Flowable SQL resources.
 *
 * @author Filip Hrisafov
 */
public class FlowableSqlResourceHintsRegistrar {

    public static void registerSqlResources(String baseFolder, ResourceHints resourceHints) {
        Stream.of("create", "drop", "upgrade")
                .forEach(folder -> resourceHints.registerPattern(baseFolder + "/" + folder + "/*.sql"));
    }

}
