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
package org.flowable.variable.service.impl.persistence.entity;

/**
 * A variable that knows whether its value could not be resolved when it was read leniently, e.g. by a variable query, by a query
 * that includes variables or when a REST response is created for it. Such a variable is kept with a {@code null} value instead of
 * failing the read.
 */
public interface HasUnresolvableValue {

    String getName();

    /**
     * @return whether the value of this variable could not be resolved. The value of such a variable is returned as {@code null}.
     */
    boolean isValueUnresolvable();

    void setValueUnresolvable(boolean valueUnresolvable);
}
