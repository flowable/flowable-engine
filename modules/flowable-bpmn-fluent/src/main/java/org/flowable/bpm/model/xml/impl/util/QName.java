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
package org.flowable.bpm.model.xml.impl.util;

public class QName {

    private final String qualifier;
    private final String localName;

    public QName(String qualifier, String localName) {
        this.localName = localName;
        this.qualifier = qualifier;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getLocalName() {
        return localName;
    }

    public static QName parseQName(String identifier) {
        String qualifier;
        String localName;

        String[] split = identifier.split(":", 2);
        if (split.length == 2) {
            qualifier = split[0];
            localName = split[1];
        } else {
            qualifier = null;
            localName = split[0];
        }
        return new QName(qualifier, localName);
    }

    @Override
    public String toString() {
        return combine(qualifier, localName);
    }

    public static String combine(String qualifier, String localName) {
        return qualifier == null || qualifier.isEmpty() ? localName : qualifier + ':' + localName;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((localName == null) ? 0 : localName.hashCode());
        result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        QName other = (QName) obj;
        if (localName == null) {
            if (other.localName != null) {
                return false;
            }
        } else if (!localName.equals(other.localName)) {
            return false;
        }
        if (qualifier == null) {
            if (other.qualifier != null) {
                return false;
            }
        } else if (!qualifier.equals(other.qualifier)) {
            return false;
        }
        return true;
    }
}
