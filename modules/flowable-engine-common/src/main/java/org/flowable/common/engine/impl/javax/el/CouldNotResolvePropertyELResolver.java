package org.flowable.common.engine.impl.javax.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

/**
 * Throws an exception in the case when resolver is asked to get/set value.
 * The last resolver in the row, when we can not decide what to do.
 *
 * @author martin.grofcik
 */
public class CouldNotResolvePropertyELResolver extends ELResolver {
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return Object.class;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return Object.class;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base != null) {
            throw new PropertyNotFoundException("Could not find property " + property + " in " + base.getClass());
        }
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        throw new PropertyNotWritableException("Cannot write property: " + property);
    }
}
