# Flowable REST principles

## Installation and Authentication

Flowable includes a REST API to the Flowable engine that can be installed by deploying the flowable-rest.war file to a servlet container like Apache Tomcat. However, it can also be used in another web-application by including the servlet and its mapping in your application and add all flowable-rest dependencies to the classpath.

By default the Flowable engine will connect to an in-memory H2 database. You can change the database settings in the db.properties file in the WEB-INF/classes folder. The REST API uses JSON format (http://www.json.org) and is built upon the Spring MVC (http://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html).

All REST-resources require a valid Flowable-user to be authenticated by default. Basic HTTP access authentication is used, so you should always include a _Authorization: Basic ...==_ HTTP-header when performing requests or include the username and password in the request-url (for example, _http://username:password@localhost:8080/xyz_).

*We recommend that you use Basic Authentication in combination with HTTPS.*

## Configuration

The Flowable REST web application uses Spring Java Configuration for starting the Flowable Form engine, defining the basic authentication security using Spring security, and to define the variable converters for specific variable handling.
A small number of properties can be defined by changing the engine.properties file you can find in the WEB-INF/classes folder.
If you need more advanced configuration options there's the possibility to override the default Spring beans in XML in the flowable-custom-context.xml file you can also find in the WEB-INF/classes folder.
An example configuration is already in comments in this file. This is also the place to override the default RestResponseFactory by defining a new Spring bean with the name restResponsefactory and use your custom implementation class for it.

## Usage in Tomcat

Due to [default security properties on Tomcat](http://tomcat.apache.org/tomcat-8.0-doc/security-howto.html), **escaped forward slashes (%2F and %5C) are not allowed by default (400-result is returned).** This may have an impact on the deployment resources and their data-URL, as the URL can potentially contain escaped forward slashes.

When issues are experienced with unexpected 400-results, set the following system-property:

_-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true_

It's best practice to always set the *Accept* and *Content-Type* (in case of posting/putting JSON) headers to *application/json* on the HTTP requests described below.

## Methods and return-codes

### HTTP-methods and corresponding operations

|Method|Operations
------ | ---------
|**GET**|Get a single resource or get a collection of resources.
|**POST**|Create a new resource. Also used for executing resource-queries which have a too complex request-structure to fit in the query-URL of a GET-request.
|**PUT**|Update properties of an existing resource. Also used for invoking actions on an existing resource.
|**DELETE**|Delete an existing resource.


### HTTP-methods response codes


|Response|Description
-------- | ----------
|**200 - Ok**|The operation was successful and a response has been returned (**GET** and **PUT** requests).
|**201 - Created**|The operation was successful and the entity has been created and is returned in the response-body (**POST** request).
|**204 - No content**|The operation was successful and entity has been deleted and therefore there is no response-body returned (**DELETE** request).
|**401 - Unauthorized**|The operation failed. The operation requires an Authentication header to be set. If this was present in the request, the supplied credentials are not valid or the user is not authorized to perform this operation.
|**403 - Forbidden**|The operation is forbidden and should not be re-attempted. This implies an issue with authorization not authentication, it's an operation that is not allowed. Example: deleting a task that is part of a running process is not allowed and will never be allowed, regardless of the user or process/task state.
|**404 - Not found**|The operation failed. The requested resource was not found.
|**405 - Method not allowed**|The operation failed. The method is not allowed for this resource. For example, trying to update (PUT) a deployment-resource will result in a **405** status.
|**409 - Conflict**|The operation failed. The operation causes an update of a resource that has been updated by another operation, which makes the update no longer valid. Can also indicate a resource that is being created in a collection where a resource with that identifier already exists.
|**415 - Unsupported Media Type**|The operation failed. The request body contains an unsupported media type. Also occurs when the request-body JSON contains an unknown attribute or value that doesn't have the right format/type to be accepted.
|**500 - Internal server error**|The operation failed. An unexpected exception occurred while executing the operation. The response-body contains details about the error.


The media-type of the HTTP-responses is always **application/json** unless binary content is requested (for example, deployment resource data), the media-type of the content is used.

## Error response body

When an error occurs (both client and server, 4XX and 5XX status-codes) the response body contains an object describing the error that occurred. An example for a 404-status when a task is not found:

```json
{
  "statusCode" : 404,
  "errorMessage" : "Could not find a task with id '444'."
}
```

## Request parameters

### URL fragments

Parameters that are part of the URL (for example, the deploymentId parameter in *http://host/flowable-rest/form-api/form-repository/deployments/{deploymentId}*) need to be properly escaped (see link [URL-encoding or Percent-encoding](https://en.wikipedia.org/wiki/Percent-encoding)) in case the segment contains special characters. Most frameworks have this functionality built in, but it should be taken into account. Especially for segments that can contain forward-slashes (for example, deployment resource), this is required.


### Rest URL query parameters

Parameters added as query-string in the URL (for example, the name parameter used in *http://host/flowable-rest/form-api/form-repository/deployments?name=Deployment*) can have the following types and are mentioned in the corresponding REST-API documentation:


|Type|Format
---- | -----
|String|Plain text parameters. Can contain any valid characters that are allowed in URLs. In the case of a *XXXLike* parameter, the string should contain the wildcard character **%** (properly URL-encoded). This allows you to specify the intent of the like-search. For example, '*Tas%*' matches all values, starting with 'Tas'.
|Integer|Parameter representing an integer value. Can only contain numeric non-decimal values, between -2.147.483.648 and 2.147.483.647.
|Long|Parameter representing a long value. Can only contain numeric non-decimal values, between -9.223.372.036.854.775.808 and 9.223.372.036.854.775.807.
|Boolean|Parameter representing a boolean value. Can be either *true* or *false*. All other values other than these will cause a '**405 - Bad request**' response.
|Date|Parameter representing a date value. Use the ISO-8601 date-format (see [ISO-8601 on wikipedia](http://en.wikipedia.org/wiki/ISO_8601) using both time and date-components (e.g. *2013-04-03T23:45:12Z*).


### JSON body parameters

|Type|Format
---- | -----
|String|Plain text parameters. In the case of a *XXXLike* parameter, the string should contain the wildcard character *%*. This allows you to specify the intent of the like-search. For example, '*Tas%*' matches all values, starting with 'Tas'.
|Integer|Parameter representing an integer value, using a JSON number. Can only contain numeric non-decimal values, between -2.147.483.648 and 2.147.483.647.
|Long|Parameter representing a long value, using a JSON number. Can only contain numeric non-decimal values, between -9.223.372.036.854.775.808 and 9.223.372.036.854.775.807.
|Date|Parameter representing a date value, using a JSON text. Use the ISO-8601 date-format (see [ISO-8601 on wikipedia](http://en.wikipedia.org/wiki/ISO_8601) using both time and date-components (for example, *2013-04-03T23:45Z*).



### Paging and sorting

Paging and order parameters can be added as query-string in the URL (for example, the name parameter used in *http://host/flowable-rest/form-api/form-repository/deployments?sort=name*).


|Parameter|Default value|Description
--------- | ----------- | ----------
|sort|different per query implementation|Name of the sort key, for which the default value and the allowed values are different per query implementation.
|order|asc|Sorting order which can be 'asc' or 'desc'.
|start|0|Parameter to allow for paging of the result. By default the result will start at 0.
|size|10|Parameter to allow for paging of the result. By default the size will be 10.


### JSON query variable format

```json
{
  "name" : "variableName",
  "value" : "variableValue",
  "operation" : "equals",
  "type" : "string"
}
```


### Variable query JSON parameters

|Parameter|Required|Description
--------- | ------ | ----------
|name|No|Name of the variable to include in a query. Can be empty in the case where '*equals*' is used in some queries to query for resources that have *any variable name* with the given value.
|value|Yes|Value of the variable included in the query, should include a correct format for the given type.
|operator|Yes|Operator to use in query, can have the following values: **equals, notEquals, equalsIgnoreCase, notEqualsIgnoreCase, lessThan, greaterThan, lessThanOrEquals, greaterThanOrEquals** and **like**.
|type|No|Type of variable to use. When omitted, the type will be deduced from the **value** parameter. Any JSON text-values will be considered of type **string**, JSON booleans of type **boolean**, JSON numbers of type **long** or **integer** depending on the size of the number. We recommended you include an explicit type when in doubt. Types supported out of the box are listed below.



### Default query JSON types

|Type name|Description
--------- | ----------
|string|Value is treated as and converted to a **java.lang.String**.
|short|Value is treated as and converted to a **java.lang.Integer**.
|integer|Value is treated as and converted to a **java.lang.Integer**.
|long|Value is treated as and converted to a **java.lang.Long**.
|double|Value is treated as and converted to a **java.lang.Double**.
|boolean|Value is treated as and converted to a **java.lang.Boolean**.
|date|Value is treated as and converted to a **java.util.Date**. The JSON string will be converted using ISO-8601 date format.


### Variable representation

When working with variables (execute decision), the REST API uses some common principles and JSON-format for both reading and writing. The JSON representation of a variable looks like this:

```json
{
  "name" : "variableName",
  "value" : "variableValue",
  "valueUrl" : "http://...",
  "type" : "string"
}
```

### Variable JSON attributes

|Parameter|Required|Description
--------- | ------ | ----------
|name|Yes|Name of the variable.
|value|No|Value of the variable. When writing a variable and **value** is omitted, **null** will be used as value.
|valueUrl|No|When reading a variable of type **binary** or **serializable**, this attribute will point to the URL from where the raw binary data can be fetched.
|type|No|Type of the variable. See table below for additional information on types. When writing a variable and this value is omitted, the type will be deduced from the raw JSON-attribute request type and is limited to either **string**, **double**, **integer** and **boolean**. We advise you to always include a type to make sure no wrong assumption about the type are made.



### Variable Types

|Type name|Description
--------- | ----------
|string|Value is treated as a **java.lang.String**. Raw JSON-text value is used when writing a variable.
|integer|Value is treated as a **java.lang.Integer**. When writing, JSON number value is used as base for conversion, falls back to JSON text.
|short|Value is treated as a **java.lang.Short**. When writing, JSON number value is used as base for conversion, falls back to JSON text.
|long|Value is treated as a **java.lang.Long**. When writing, JSON number value is used as base for conversion, falls back to JSON text.
|double|Value is treated as a **java.lang.Double**. When writing, JSON number value is used as base for conversion, falls back to JSON text.
|boolean|Value is treated as a **java.lang.Boolean**. When writing, JSON boolean value is used for conversion.
|date|Value is treated as a **java.util.Date**. When writing, the JSON text will be converted using ISO-8601 date format.


It's possible to support additional variable-types with a custom JSON representation (either simple value or complex/nested JSON object). By extending the **initializeVariableConverters()** method on *org.flowable.dmn.rest.service.api.DmnRestResponseFactory**, you can add additional **org.flowable.rest.variable.RestVariableConverter* classes to support converting your POJOs to a format suitable for transferring through REST and converting the REST-value back to your POJO. The actual transformation to JSON is done by Jackson.


