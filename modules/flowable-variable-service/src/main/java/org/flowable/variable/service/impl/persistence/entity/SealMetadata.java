package org.flowable.variable.service.impl.persistence.entity;

import java.io.Serializable;

public class SealMetadata implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String value;
	private String type;
	private String origin;
	private SealAttribute[] attributes;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public SealAttribute[] getAttributes() {
		return attributes;
	}

	public void setAttributes(SealAttribute[] attributes) {
		this.attributes = attributes;
	}

	public boolean containsRegex(String str) {
		if (this.getValue().matches(str)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			if (value.equals(obj)) {
				return true;
			} 
		} else if (obj instanceof SealMetadata) {
			return value.equals(((SealMetadata)obj).getValue()); 
		}
		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
