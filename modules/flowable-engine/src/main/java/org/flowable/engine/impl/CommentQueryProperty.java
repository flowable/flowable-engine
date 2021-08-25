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
package org.flowable.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;

/**
 * Contains the possible properties which can be used in a {@link org.flowable.engine.task.CommentQuery}.
 *
 * @author David Lamas
 */
public class CommentQueryProperty implements QueryProperty {
    private static final long serialVersionUID = 1L;

    private static final Map<String, CommentQueryProperty> properties = new HashMap<>();

    public static final CommentQueryProperty ID = new CommentQueryProperty("RES.ID_");
    public static final CommentQueryProperty TIME = new CommentQueryProperty("RES.TIME_");
    public static final CommentQueryProperty USER_ID = new CommentQueryProperty("RES.USER_ID_");
    public static final CommentQueryProperty TYPE = new CommentQueryProperty("RES.TYPE_");
    public static final CommentQueryProperty TASK_ID = new CommentQueryProperty("RES.TASK_ID_");
    public static final CommentQueryProperty PROCESS_INSTANCE_ID = new CommentQueryProperty("RES.PROC_INST_ID_");

    private String name;

    public CommentQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static CommentQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }
}
