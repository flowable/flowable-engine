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

package org.flowable.engine.impl.form;

import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.FormType;

/**
 * @author Tom Baeyens
 */
public class FormPropertyImpl implements FormProperty {

    protected String id;
    protected String name;
    protected FormType type;
    protected boolean isRequired;
    protected boolean isReadable;
    protected boolean isWritable;

    protected String value;

    public FormPropertyImpl(FormPropertyHandler formPropertyHandler) {
        this.id = formPropertyHandler.getId();
        this.name = formPropertyHandler.getName();
        this.type = formPropertyHandler.getType();
        this.isRequired = formPropertyHandler.isRequired();
        this.isReadable = formPropertyHandler.isReadable();
        this.isWritable = formPropertyHandler.isWritable();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FormType getType() {
        return type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public boolean isReadable() {
        return isReadable;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean isWritable() {
        return isWritable;
    }
}
