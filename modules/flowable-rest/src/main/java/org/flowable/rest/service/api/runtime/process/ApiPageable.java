package org.flowable.rest.service.api.runtime.process;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ApiImplicitParams({
        @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", paramType = "query"),
        @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", allowableValues = "asc,desc", paramType = "query"),
        @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
        @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query")
})
public @interface ApiPageable {
    
}