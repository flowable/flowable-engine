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
package org.flowable.spring.boot;

/**
 * Interface to be implemented by a bean that does some extra configuration of a Flowable engines. If such a bean is defined, it will be called when the
 * specific engine configuration is created and the default values have been set.
 *
 * @author Filip Hrisafov
 */
public interface EngineConfigurationConfigurer<T> {

    /**
     * Configure the provided engine. For example to provide a custom service implementation for a certain engine.
     */
    void configure(T engineConfiguration);
}
