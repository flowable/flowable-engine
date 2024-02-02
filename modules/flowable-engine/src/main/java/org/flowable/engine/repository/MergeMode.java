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

package org.flowable.engine.repository;

/**
 * @author Valentin Zickner
 */
public enum MergeMode {

    VERIFY("verify"),
    AS_NEW("as-new"),
    AS_OLD("as-old"),
    BY_DATE("by-date");

    protected String name;

    MergeMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
