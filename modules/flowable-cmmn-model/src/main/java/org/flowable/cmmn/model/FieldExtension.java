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
package org.flowable.cmmn.model;

/**
 * @author Tijs Rademakers
 */
public class FieldExtension extends BaseElement {

    protected String fieldName;
    protected String stringValue;
    protected String expression;

    public FieldExtension() {
    }

    public FieldExtension(String fieldName, String stringValue, String expression) {
        this.fieldName = fieldName;
        this.stringValue = stringValue;
        this.expression = expression;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public FieldExtension clone() {
        FieldExtension clone = new FieldExtension();
        clone.setValues(this);
        return clone;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldExtension)) return false;

        FieldExtension that = (FieldExtension) o;

        if (!getFieldName().equals(that.getFieldName())) return false;
        if (getStringValue() != null ? !getStringValue().equals(that.getStringValue()) : that.getStringValue() != null)
            return false;
        return getExpression() != null ? getExpression().equals(that.getExpression()) : that.getExpression() == null;
    }

    @Override
    public int hashCode() {
        int result = getFieldName().hashCode();
        result = 31 * result + (getStringValue() != null ? getStringValue().hashCode() : 0);
        result = 31 * result + (getExpression() != null ? getExpression().hashCode() : 0);
        return result;
    }

    public void setValues(FieldExtension otherExtension) {
        setFieldName(otherExtension.getFieldName());
        setStringValue(otherExtension.getStringValue());
        setExpression(otherExtension.getExpression());
    }

    @Override
    public String toString() {
        return "\nFieldExtension{" +
                "fieldName='" + fieldName + '\'' +
                ", stringValue='" + stringValue + '\'' +
                ", expression='" + expression + '\'' +
                ", id='" + id + '\'' +
                ", xmlRowNumber=" + xmlRowNumber +
                ", xmlColumnNumber=" + xmlColumnNumber +
                ", extensionElements=" + extensionElements +
                ", attributes=" + attributes +
                "}\n";
    }
}
