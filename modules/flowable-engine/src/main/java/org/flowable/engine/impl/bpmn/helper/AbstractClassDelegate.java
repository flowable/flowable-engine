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

package org.flowable.engine.impl.bpmn.helper;

import java.util.List;

import org.flowable.bpmn.model.ServiceTask;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;

/**
 * Helper class for bpmn constructs that allow class delegation.
 * 
 * This class will lazily instantiate the referenced classes when needed at runtime.
 * 
 * @author Tijs Rademakers
 */
public abstract class AbstractClassDelegate extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String serviceTaskId;
    protected String className;
    protected List<FieldDeclaration> fieldDeclarations;

    public AbstractClassDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        this.className = className;
        this.fieldDeclarations = fieldDeclarations;
    }

    public AbstractClassDelegate(Class<?> clazz, List<FieldDeclaration> fieldDeclarations) {
        this.className = clazz.getName();
        this.fieldDeclarations = fieldDeclarations;
    }

    protected Object instantiateDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        return AbstractClassDelegate.defaultInstantiateDelegate(className, fieldDeclarations);
    }

    // --HELPER METHODS (also usable by external classes)
    // ----------------------------------------

    public static Object defaultInstantiateDelegate(Class<?> clazz, List<FieldDeclaration> fieldDeclarations, ServiceTask serviceTask) {
        return defaultInstantiateDelegate(clazz.getName(), fieldDeclarations, serviceTask);
    }
    
    public static Object defaultInstantiateDelegate(Class<?> clazz, List<FieldDeclaration> fieldDeclarations) {
        return defaultInstantiateDelegate(clazz.getName(), fieldDeclarations);
    }
    
    public static Object defaultInstantiateDelegate(String className, List<FieldDeclaration> fieldDeclarations, ServiceTask serviceTask) {
        Object object = ReflectUtil.instantiate(className);
        applyFieldDeclaration(fieldDeclarations, object);
        
        if (serviceTask != null) {
            ReflectUtil.invokeSetterOrField(object, "serviceTask", serviceTask, false);
        }
        
        return object;
    }

    public static Object defaultInstantiateDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        return defaultInstantiateDelegate(className, fieldDeclarations, null);
    }

    public static void applyFieldDeclaration(List<FieldDeclaration> fieldDeclarations, Object target) {
        applyFieldDeclaration(fieldDeclarations, target, true);
    }

    public static void applyFieldDeclaration(List<FieldDeclaration> fieldDeclarations, Object target, boolean throwExceptionOnMissingField) {
        if (fieldDeclarations != null) {
            for (FieldDeclaration declaration : fieldDeclarations) {
                applyFieldDeclaration(declaration, target, throwExceptionOnMissingField);
            }
        }
    }

    public static void applyFieldDeclaration(FieldDeclaration declaration, Object target) {
        applyFieldDeclaration(declaration, target, true);
    }

    public static void applyFieldDeclaration(FieldDeclaration declaration, Object target, boolean throwExceptionOnMissingField) {
        ReflectUtil.invokeSetterOrField(target, declaration.getName(), declaration.getValue(), throwExceptionOnMissingField);
    }

    /**
     * returns the class name this {@link AbstractClassDelegate} is configured to. Comes in handy if you want to check which delegates you already have e.g. in a list of listeners
     */
    public String getClassName() {
        return className;
    }

}
