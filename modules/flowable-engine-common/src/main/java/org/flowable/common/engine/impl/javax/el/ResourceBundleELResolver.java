/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.javax.el;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

public class ResourceBundleELResolver extends ELResolver {

	public ResourceBundleELResolver() {
		super();
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context);

		if (base instanceof ResourceBundle) {
			context.setPropertyResolved(base, property);

			if (property != null) {
				try {
					return ((ResourceBundle) base).getObject(property.toString());
				} catch (MissingResourceException mre) {
					return "???" + property.toString() + "???";
				}
			}
	}

		return null;
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context);

		if (base instanceof ResourceBundle) {
			context.setPropertyResolved(base, property);
			/*
			 * ResourceBundles are always read-only so fall-through to return null
			 */
		}

		return null;
	}

	@Override
	public void setValue(ELContext context, Object base, Object property, Object value) {
		Objects.requireNonNull(context);

		if (base instanceof ResourceBundle) {
			context.setPropertyResolved(base, property);
			throw new PropertyNotWritableException("ELResolver not writable for type '" + base.getClass().getName() + "'");
			}
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		Objects.requireNonNull(context);

		if (base instanceof ResourceBundle) {
			context.setPropertyResolved(base, property);
		return true;
	}

		return false;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		if (base instanceof ResourceBundle) {
			return String.class;
		}
		return null;
	}

}
