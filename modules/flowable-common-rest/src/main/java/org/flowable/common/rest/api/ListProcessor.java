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
package org.flowable.common.rest.api;

import java.util.List;
import java.util.function.Function;

/**
 * A processor for converting List of a certain type into list of a different type.
 *
 * @param <REQ> The type of the elements that need to be processed
 * @param <RES> The type of the elements that will be created
 * @author Filip Hrisafov
 */
@FunctionalInterface
public interface ListProcessor<REQ, RES> extends Function<List<REQ>, List<RES>> {

    @Override
    default List<RES> apply(List<REQ> res) {
        return processList(res);
    }

    List<RES> processList(List<REQ> list);
}
