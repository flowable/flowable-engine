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
package org.flowable.mongodb.persistence.mapper;

import java.util.Date;

import org.bson.Document;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.mongodb.persistence.EntityToDocumentMapper;

/**
 * @author Joram Barrez
 */
public abstract class AbstractEntityToDocumentMapper<T extends Entity> implements EntityToDocumentMapper<T> {

    protected void appendIfNotNull(Document document, String field, String value) {
        if (value != null) {
            document.append(field, value);
        }
    }

    protected void appendIfNotNull(Document document, String field, Date value) {
        if (value != null) {
            document.append(field, value);
        }
    }

    protected void appendIfNotNull(Document document, String field, Integer value) {
        if (value != null) {
            document.append(field, value);
        }
    }

    protected void appendIfNotNull(Document document, String field, Boolean value) {
        if (value != null) {
            document.append(field, value);
        }
    }

    protected void appendIfNotNull(Document document, String field, Long value) {
        if (value != null) {
            document.append(field, value);
        }
    }

    protected void appendIfNotNull(Document document, String field, Double value) {
        if (value != null) {
            document.append(field, value);
        }
    }

    protected void appendIfNotNull(Document document, String field, byte[] value) {
        if (value != null) {
            document.append(field, value);
        }
    }

}
