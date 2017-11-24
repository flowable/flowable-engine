package org.flowable.variable.service;

import java.util.Date;

import org.flowable.variable.service.impl.persistence.entity.SealMetadataList;

public interface ISealMetadata {

	boolean containsString(String str);
	
	boolean containsRegex(String str);
	
	boolean containsNumber(Number number);
	
	boolean containsNumberNotEquals(Number number);
	
	boolean containsNumberLessThan(Number number);
	
	boolean containsNumberGreaterThan(Number number);
	
	boolean containsNumberLessThanOrEquals(Number number);
	
	boolean containsNumberGreaterThanOrEquals(Number number);
	
	boolean containsDateEquals(Date date);
	
	boolean containsDateNotEquals(Date date);
	
	boolean containsDateLessThan(Date date);
	
	boolean containsDateGreaterThan(Date date);
	
	boolean containsDateLessThanOrEquals(Date date);
	
	boolean containsDateGreaterThanOrEquals(Date date);
	
	SealMetadataList append(Object obj);
	
	SealMetadataList removeObject(Object obj);
	
	SealMetadataList clearMetadata();
	
}
