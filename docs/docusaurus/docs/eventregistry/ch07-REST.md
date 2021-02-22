---
id: ch07-REST
title: REST API
---

## General Flowable REST principles

### Installation and Authentication

Flowable includes a REST API to the Flowable engine that can be installed by deploying the flowable-rest.war file to a servlet container like Apache Tomcat. However, it can also be used in another web-application by including the servlet (and/or its mappings) in your application and add all flowable-rest dependencies to the classpath.

By default the Flowable engine will connect to an in-memory H2 database. You can change the database settings in the flowable-app.properties file in the *WEB-INF/META-INF/flowable-app* folder. The REST API uses JSON format (<http://www.json.org>) and is built upon the Spring MVC (<http://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html>).

All REST-resources require a valid user with the *rest-access-api* privilege to be authenticated by default. If any valid user should be able to call the REST API, the *flowable.rest.app.authentication-mode* can be set to *any-user* (this was the default in older versions of Flowable).

A default user that can access the REST API can be configured by settings the following properties:

    flowable.rest.app.admin.user-id=rest-admin
    flowable.rest.app.admin.password=test
    flowable.rest.app.admin.first-name=Rest
    flowable.rest.app.admin.last-name=Admin

When the REST app boots up, the user is created if it doesn’t exist or fetched otherwise. This user will be given the *access-rest-api* privilege which is needed by default to access the REST API. **Do not forget to change the password of this user afterwards**. If the *flowable.rest.app.admin.user-id* is not set, no users or privileges will be created. So, after the initial setup, removing this property will not remove the user nor the privilege that has been configured before.

Basic HTTP access authentication is used, so you should always include a *Authorization: Basic …​==* HTTP-header when performing requests or include the username and password in the request-url (for example, *<http://username:password@localhost:8080/xyz>*).

**We recommend that you use Basic Authentication in combination with HTTPS.**

### Configuration

The Flowable REST web application uses Spring Java Configuration for starting the Flowable Form engine, defining the basic authentication security using Spring security, and to define the variable converters for specific variable handling.
A small number of properties can be defined by changing the flowable-app.properties file you can find in the WEB-INF/META-INF/flowable-app/flowable-app.properties folder.
If you need more advanced configuration options there’s the possibility to override the default Spring beans in XML in the flowable-custom-context.xml file you can also find in the WEB-INF/classes folder.
An example configuration is already in comments in this file. This is also the place to override the default RestResponseFactory by defining a new Spring bean with the name restResponsefactory and use your custom implementation class for it.

### Usage in Tomcat

Due to [default security properties on Tomcat](http://tomcat.apache.org/tomcat-8.0-doc/security-howto.html), **escaped forward slashes (%2F and %5C) are not allowed by default (400-result is returned).** This may have an impact on the deployment resources and their data-URL, as the URL can potentially contain escaped forward slashes.

When issues are experienced with unexpected 400-results, set the following system-property:

*-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW\_ENCODED\_SLASH=true*

It’s best practice to always set the **Accept** and **Content-Type** (in case of posting/putting JSON) headers to **application/json** on the HTTP requests described below.

### Methods and return-codes

<table>
<caption>HTTP-methods and corresponding operations</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Method</th>
<th>Operations</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>GET</p></td>
<td><p>Get a single resource or get a collection of resources.</p></td>
</tr>
<tr class="even">
<td><p>POST</p></td>
<td><p>Create a new resource. Also used for executing resource-queries which have a too complex request-structure to fit in the query-URL of a GET-request.</p></td>
</tr>
<tr class="odd">
<td><p>PUT</p></td>
<td><p>Update properties of an existing resource. Also used for invoking actions on an existing resource.</p></td>
</tr>
<tr class="even">
<td><p>DELETE</p></td>
<td><p>Delete an existing resource.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>HTTP-methods response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200 - Ok</p></td>
<td><p>The operation was successful and a response has been returned (GET and PUT requests).</p></td>
</tr>
<tr class="even">
<td><p>201 - Created</p></td>
<td><p>The operation was successful and the entity has been created and is returned in the response-body (POST request).</p></td>
</tr>
<tr class="odd">
<td><p>204 - No content</p></td>
<td><p>The operation was successful and entity has been deleted and therefore there is no response-body returned (DELETE request).</p></td>
</tr>
<tr class="even">
<td><p>401 - Unauthorized</p></td>
<td><p>The operation failed. The operation requires an Authentication header to be set. If this was present in the request, the supplied credentials are not valid or the user is not authorized to perform this operation.</p></td>
</tr>
<tr class="odd">
<td><p>403 - Forbidden</p></td>
<td><p>The operation is forbidden and should not be re-attempted. This implies an issue with authorization not authentication, it’s an operation that is not allowed. Example: deleting a task that is part of a running process is not allowed and will never be allowed, regardless of the user or process/task state.</p></td>
</tr>
<tr class="even">
<td><p>404 - Not found</p></td>
<td><p>The operation failed. The requested resource was not found.</p></td>
</tr>
<tr class="odd">
<td><p>405 - Method not allowed</p></td>
<td><p>The operation failed. The method is not allowed for this resource. For example, trying to update (PUT) a deployment-resource will result in a 405 status.</p></td>
</tr>
<tr class="even">
<td><p>409 - Conflict</p></td>
<td><p>The operation failed. The operation causes an update of a resource that has been updated by another operation, which makes the update no longer valid. Can also indicate a resource that is being created in a collection where a resource with that identifier already exists.</p></td>
</tr>
<tr class="odd">
<td><p>415 - Unsupported Media Type</p></td>
<td><p>The operation failed. The request body contains an unsupported media type. Also occurs when the request-body JSON contains an unknown attribute or value that doesn’t have the right format/type to be accepted.</p></td>
</tr>
<tr class="even">
<td><p>500 - Internal server error</p></td>
<td><p>The operation failed. An unexpected exception occurred while executing the operation. The response-body contains details about the error.</p></td>
</tr>
</tbody>
</table>

The media-type of the HTTP-responses is always application/json unless binary content is requested (for example, deployment resource data), the media-type of the content is used.

### Error response body

When an error occurs (both client and server, 4XX and 5XX status-codes) the response body contains an object describing the error that occurred. An example for a 404-status when a task is not found:

    {
      "statusCode" : 404,
      "errorMessage" : "Could not find a task with id '444'."
    }

### Request parameters

#### URL fragments

Parameters that are part of the URL (for example, the deploymentId parameter in http://host/flowable-rest/form-api/form-repository/deployments/{deploymentId})
need to be properly escaped (see [URL-encoding or Percent-encoding](https://en.wikipedia.org/wiki/Percent-encoding)) in case the segment contains special characters. Most frameworks have this functionality built in, but it should be taken into account. Especially for segments that can contain forward-slashes (for example, deployment resource), this is required.

#### Rest URL query parameters

Parameters added as query-string in the URL (for example, the name parameter used in http://host/flowable-rest/form-api/form-repository/deployments?name=Deployment) can have the following types and are mentioned in the corresponding REST-API documentation:

<table>
<caption>URL query parameter types</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Type</th>
<th>Format</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>String</p></td>
<td><p>Plain text parameters. Can contain any valid characters that are allowed in URLs. In the case of a XXXLike parameter, the string should contain the wildcard character % (properly URL-encoded). This allows you to specify the intent of the like-search. For example, 'Tas%' matches all values, starting with 'Tas'.</p></td>
</tr>
<tr class="even">
<td><p>Integer</p></td>
<td><p>Parameter representing an integer value. Can only contain numeric non-decimal values, between -2.147.483.648 and 2.147.483.647.</p></td>
</tr>
<tr class="odd">
<td><p>Long</p></td>
<td><p>Parameter representing a long value. Can only contain numeric non-decimal values, between -9.223.372.036.854.775.808 and 9.223.372.036.854.775.807.</p></td>
</tr>
<tr class="even">
<td><p>Boolean</p></td>
<td><p>Parameter representing a boolean value. Can be either true or false. All other values other than these will cause a '405 - Bad request' response.</p></td>
</tr>
<tr class="odd">
<td><p>Date</p></td>
<td><p>Parameter representing a date value. Use the ISO-8601 date-format (see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO-8601 on wikipedia</a>) using both time and date-components (e.g. 2013-04-03T23:45Z).</p></td>
</tr>
</tbody>
</table>

#### JSON body parameters

<table>
<caption>JSON parameter types</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Type</th>
<th>Format</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>String</p></td>
<td><p>Plain text parameters. In the case of a XXXLike parameter, the string should contain the wildcard character %. This allows you to specify the intent of the like-search. For example, 'Tas%' matches all values, starting with 'Tas'.</p></td>
</tr>
<tr class="even">
<td><p>Integer</p></td>
<td><p>Parameter representing an integer value, using a JSON number. Can only contain numeric non-decimal values, between -2.147.483.648 and 2.147.483.647.</p></td>
</tr>
<tr class="odd">
<td><p>Long</p></td>
<td><p>Parameter representing a long value, using a JSON number. Can only contain numeric non-decimal values, between -9.223.372.036.854.775.808 and 9.223.372.036.854.775.807.</p></td>
</tr>
<tr class="even">
<td><p>Date</p></td>
<td><p>Parameter representing a date value, using a JSON text. Use the ISO-8601 date-format (see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO-8601 on wikipedia</a>) using both time and date-components (for example, 2013-04-03T23:45Z).</p></td>
</tr>
</tbody>
</table>

#### Paging and sorting

Paging and order parameters can be added as query-string in the URL (for example, the name parameter used in http://host/flowable-rest/form-api/form-repository/deployments?sort=name).

<table>
<caption>Variable query JSON parameters</caption>
<colgroup>
<col style="width: 33%" />
<col style="width: 33%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Default value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>sort</p></td>
<td><p>different per query implementation</p></td>
<td><p>Name of the sort key, for which the default value and the allowed values are different per query implementation.</p></td>
</tr>
<tr class="even">
<td><p>order</p></td>
<td><p>asc</p></td>
<td><p>Sorting order which can be 'asc' or 'desc'.</p></td>
</tr>
<tr class="odd">
<td><p>start</p></td>
<td><p>0</p></td>
<td><p>Parameter to allow for paging of the result. By default the result will start at 0.</p></td>
</tr>
<tr class="even">
<td><p>size</p></td>
<td><p>10</p></td>
<td><p>Parameter to allow for paging of the result. By default the size will be 10.</p></td>
</tr>
</tbody>
</table>

> **Note**
>
> Bear in mind that the start parameter is used as the offset of the query. For example, to get tasks in three pages of three items each (9 items), we would use:
>
>     GET /runtime/tasks?start=0&size=3
>     GET /runtime/tasks?start=3&size=3
>     GET /runtime/tasks?start=6&size=3

## Deployment

**When using Tomcat, please read [Usage in Tomcat](#usage-in-tomcat).**

### List of Event Registry Deployments

    GET event-registry-repository/deployments

<table>
<caption>URL query parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with the given name.</p></td>
</tr>
<tr class="even">
<td><p>nameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with a name like the given name.</p></td>
</tr>
<tr class="odd">
<td><p>category</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with the given category.</p></td>
</tr>
<tr class="even">
<td><p>categoryNotEquals</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments which don’t have the given category.</p></td>
</tr>
<tr class="odd">
<td><p>parentDeploymentId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with the given parent deployment id.</p></td>
</tr>
<tr class="even">
<td><p>parentDeploymentIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with a parent deployment id like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>tenantId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with the given tenantId.</p></td>
</tr>
<tr class="even">
<td><p>tenantIdLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return deployments with a tenantId like the given value.</p></td>
</tr>
<tr class="odd">
<td><p>withoutTenantId</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>If true, only returns deployments without a tenantId set. If false, the withoutTenantId parameter is ignored.</p></td>
</tr>
<tr class="even">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>'id' (default), 'name', 'deploytime' or 'tenantId'</p></td>
<td><p>Property to sort on, to be used together with the 'order'.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>REST Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the request was successful.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
        {
          "id": "03ab310d-c1de-11e6-a4f4-62ce84ef239e",
          "name": null,
          "deploymentTime": "2016-12-14T10:16:37.000+01:00",
          "category": null,
          "url": "http://localhost:8080/flowable-rest/event-registry-api/event-registry-repository/deployments/03ab310d-c1de-11e6-a4f4-62ce84ef239e",
          "parentDeploymentId": "17510",
          "tenantId": ""
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "id",
      "order": "asc",
      "size": 1
    }

### Get an Event Registry deployment

    GET event-registry-repository/deployments/{deploymentId}

<table>
<caption>Get a deployment - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the deployment to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a deployment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the deployment was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id": "03ab310d-c1de-11e6-a4f4-62ce84ef239e",
      "name": null,
      "deploymentTime": "2016-12-14T10:16:37.000+01:00",
      "category": null,
      "url": "http://localhost:8080/flowable-rest/event-registry-api/event-registry-repository/deployments/03ab310d-c1de-11e6-a4f4-62ce84ef239e",
      "parentDeploymentId": "17510",
      "tenantId": ""
    }

### Create a new Event Registry deployment

    POST event-registry-repository/deployments

**Request body:**

The request body should contain data of type *multipart/form-data*. There should be exactly one file in the request: any additional files will be ignored. The deployment name is the name of the file-field passed in.

An additional parameter (form-field) can be passed in the request body with name tenantId. The value of this field will be used as the identifier of the tenant in which this deployment is done.

<table>
<caption>Create a new Event deployment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the deployment was created.</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates there was no content present in the request body or the content mime-type is not supported for deployment. The status-description contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id": "03ab310d-c1de-11e6-a4f4-62ce84ef239e",
      "name": "newDeployment1",
      "deploymentTime": "2016-12-14T10:16:37.000+01:00",
      "category": null,
      "url": "http://localhost:8080/flowable-rest/event-registry-api/event-registry-repository/deployments/03ab310d-c1de-11e6-a4f4-62ce84ef239e",
      "parentDeploymentId": "17510",
      "tenantId" : "myTenant"
    }

### Delete an Event Registry deployment

    DELETE event-registry-repository/deployments/{deploymentId}

<table>
<caption>Delete an Event Registry deployment - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the deployment to delete.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Delete an Event Registry deployment - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>204</p></td>
<td><p>Indicates the deployment was found and has been deleted. Response-body is intentionally empty.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found.</p></td>
</tr>
</tbody>
</table>

### Get an Event Registry deployment resource content

    GET event-registry-repository/deployments/{deploymentId}/resourcedata/{resourceId}

<table>
<caption>Get a deployment resource content - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the deployment the requested resource is part of.</p></td>
</tr>
<tr class="even">
<td><p>resourceId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the resource to get the data for. <strong>Make sure you URL-encode the resourceId in case it contains forward slashes. For example, use 'events%2Fmy-event.event' instead of 'events/my-event.event'.</strong></p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a deployment resource content - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates both deployment and resource have been found and the resource data has been returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested deployment was not found or there is no resource with the given identifier present in the deployment. The status-description contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

The response body will contain the binary resource-content for the requested resource. The response content-type will be the same as the type returned in the resources 'mimeType' property. Also, a content-disposition header is set, allowing browsers to download the file instead of displaying it.

## Event Definitions

### List of event definitions

    GET event-registry-repository/event-definitions

<table>
<caption>List of event definitions - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>version</p></td>
<td><p>No</p></td>
<td><p>integer</p></td>
<td><p>Only return event definitions with the given version.</p></td>
</tr>
<tr class="even">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with the given name.</p></td>
</tr>
<tr class="odd">
<td><p>nameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with a name like the given name.</p></td>
</tr>
<tr class="even">
<td><p>key</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with the given key.</p></td>
</tr>
<tr class="odd">
<td><p>keyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with a key like the given key.</p></td>
</tr>
<tr class="even">
<td><p>resourceName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with the given resource name.</p></td>
</tr>
<tr class="odd">
<td><p>resourceNameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with a name like the given resource name.</p></td>
</tr>
<tr class="even">
<td><p>category</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with the given category.</p></td>
</tr>
<tr class="odd">
<td><p>categoryLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions with a category like the given name.</p></td>
</tr>
<tr class="even">
<td><p>categoryNotEquals</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions which don’t have the given category.</p></td>
</tr>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return event definitions which are part of a deployment with the given identifier.</p></td>
</tr>
<tr class="even">
<td><p>latest</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Only return the latest event definition versions. Can only be used together with 'key' and 'keyLike' parameters, using any other parameter will result in a 400-response.</p></td>
</tr>
<tr class="odd">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>'name' (default), 'id', 'key', 'category', 'deploymentId' and 'tenantId'</p></td>
<td><p>Property to sort on, to be used together with the 'order'.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of event definitions - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the event definitions are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format or that 'latest' is used with other parameters other than 'key' and 'keyLike'. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
            {
          "id": "46b0379c-c0a1-11e6-bc93-6ab56fad108a",
          "url": "http://localhost:8080/flowable-rest/event-registry-api/event-registry-repository/event-definitions/46b0379c-c0a1-11e6-bc93-6ab56fad108a",
          "category": null,
          "name": "Event Definition One",
          "key": "EventDefinitionOne",
          "description": null,
          "version": 3,
          "resourceName": "EventDefinitionOne.event",
          "deploymentId": "46aa6b3a-c0a1-11e6-bc93-6ab56fad108a",
          "parentDeploymentId": "5001",
          "tenantId": ""
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Get an event definition

    GET event-registry-repository/event-definitions/{eventDefinitionId}

<table>
<caption>Get an event definition - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>eventDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the event definition to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get an event definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the event definition was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested event definition was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id": "46b0379c-c0a1-11e6-bc93-6ab56fad108a",
      "url": "http://localhost:8080/flowable-rest/event-registry-api/event-registry-repository/event-definitions/46b0379c-c0a1-11e6-bc93-6ab56fad108a",
      "category": null,
      "name": "Event Definition One",
      "key": "EventDefinitionOne",
      "description": null,
      "version": 3,
      "resourceName": "EventDefinitionOne.event",
      "deploymentId": "46aa6b3a-c0a1-11e6-bc93-6ab56fad108a",
      "parentDeploymentId": "5001",
      "tenantId": ""
    }

### Get an event definition resource content

    GET event-registry-repository/event-definitions/{eventDefinitionId}/resourcedata

<table>
<caption>Get an event definition resource content - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>eventDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the event definition to get the resource data for.</p></td>
</tr>
</tbody>
</table>

**Response:**

Exactly the same response codes as GET event-registry-repository/deployments/{deploymentId}/resourcedata/{resourceId}.

### Get an event definition model

    GET event-registry-repository/event-definitions/{eventDefinitionId}/model

<table>
<caption>Get an event definition model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>eventDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the event definition to get the model for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get an event definition model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the event definition was found and the model is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested event definition was not found.</p></td>
</tr>
</tbody>
</table>

**Response body:**
The response body is a JSON representation of the org.flowable.eventregistry.model.EventModel.

    {
       "key": "myEvent",
       "name": "My event",
       "correlationParameters": [
          {
            "name": "customerId",
            "type": "string"
          }
       ],
       "payload": [
          {
            "name": "payload1",
            "type": "string"
          },
          {
            "name": "payload2",
            "type": "integer"
          }
       ]
    }
    
## Channel Definitions

### List of channel definitions

    GET event-registry-repository/channel-definitions

<table>
<caption>List of channel definitions - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>version</p></td>
<td><p>No</p></td>
<td><p>integer</p></td>
<td><p>Only return channel definitions with the given version.</p></td>
</tr>
<tr class="even">
<td><p>name</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with the given name.</p></td>
</tr>
<tr class="odd">
<td><p>nameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with a name like the given name.</p></td>
</tr>
<tr class="even">
<td><p>key</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with the given key.</p></td>
</tr>
<tr class="odd">
<td><p>keyLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with a key like the given key.</p></td>
</tr>
<tr class="even">
<td><p>resourceName</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with the given resource name.</p></td>
</tr>
<tr class="odd">
<td><p>resourceNameLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with a name like the given resource name.</p></td>
</tr>
<tr class="even">
<td><p>category</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with the given category.</p></td>
</tr>
<tr class="odd">
<td><p>categoryLike</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions with a category like the given name.</p></td>
</tr>
<tr class="even">
<td><p>categoryNotEquals</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions which don’t have the given category.</p></td>
</tr>
<tr class="odd">
<td><p>deploymentId</p></td>
<td><p>No</p></td>
<td><p>String</p></td>
<td><p>Only return channel definitions which are part of a deployment with the given identifier.</p></td>
</tr>
<tr class="even">
<td><p>latest</p></td>
<td><p>No</p></td>
<td><p>Boolean</p></td>
<td><p>Only return the latest channel definition versions. Can only be used together with 'key' and 'keyLike' parameters, using any other parameter will result in a 400-response.</p></td>
</tr>
<tr class="odd">
<td><p>sort</p></td>
<td><p>No</p></td>
<td><p>'name' (default), 'id', 'key', 'category', 'deploymentId' and 'tenantId'</p></td>
<td><p>Property to sort on, to be used together with the 'order'.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>List of channel definitions - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates request was successful and the channel definitions are returned</p></td>
</tr>
<tr class="even">
<td><p>400</p></td>
<td><p>Indicates a parameter was passed in the wrong format or that 'latest' is used with other parameters other than 'key' and 'keyLike'. The status-message contains additional information.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "data": [
            {
          "id": "46b0379c-c0a1-11e6-bc93-6ab56fad108a",
          "url": "http://localhost:8080/flowable-rest/event-registry-api/event-registry-repository/channel-definitions/46b0379c-c0a1-11e6-bc93-6ab56fad108a",
          "category": null,
          "name": "Channel Definition One",
          "key": "ChannelDefinitionOne",
          "description": null,
          "version": 3,
          "resourceName": "ChannelDefinitionOne.channel",
          "deploymentId": "46aa6b3a-c0a1-11e6-bc93-6ab56fad108a",
          "parentDeploymentId": "5001",
          "tenantId": ""
        }
      ],
      "total": 1,
      "start": 0,
      "sort": "name",
      "order": "asc",
      "size": 1
    }

### Get a channel definition

    GET event-registry-repository/channel-definitions/{channelDefinitionId}

<table>
<caption>Get a channel definition - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>channelDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the channel definition to get.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a channel definition - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the channel definition was found and returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested channel definition was not found.</p></td>
</tr>
</tbody>
</table>

**Success response body:**

    {
      "id": "46b0379c-c0a1-11e6-bc93-6ab56fad108a",
      "url": "http://localhost:8080/flowable-rest/event-registry-api/event-registry-repository/channel-definitions/46b0379c-c0a1-11e6-bc93-6ab56fad108a",
      "category": null,
      "name": "Channel Definition One",
      "key": "ChannelDefinitionOne",
      "description": null,
      "version": 3,
      "resourceName": "ChannelDefinitionOne.channel",
      "deploymentId": "46aa6b3a-c0a1-11e6-bc93-6ab56fad108a",
      "parentDeploymentId": "5001",
      "tenantId": ""
    }

### Get a channel definition resource content

    GET event-registry-repository/channel-definitions/{channelDefinitionId}/resourcedata

<table>
<caption>Get a channel definition resource content - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>channelDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the channel definition to get the resource data for.</p></td>
</tr>
</tbody>
</table>

**Response:**

Exactly the same response codes as GET event-registry-repository/deployments/{deploymentId}/resourcedata/{resourceId}.

### Get a channel definition model

    GET event-registry-repository/channel-definitions/{channelDefinitionId}/model

<table>
<caption>Get a channel definition model - URL parameters</caption>
<colgroup>
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
<col style="width: 25%" />
</colgroup>
<thead>
<tr class="header">
<th>Parameter</th>
<th>Required</th>
<th>Value</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>channelDefinitionId</p></td>
<td><p>Yes</p></td>
<td><p>String</p></td>
<td><p>The identifier of the channel definition to get the model for.</p></td>
</tr>
</tbody>
</table>

<table>
<caption>Get a channel definition model - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the channel definition was found and the model is returned.</p></td>
</tr>
<tr class="even">
<td><p>404</p></td>
<td><p>Indicates the requested channel definition was not found.</p></td>
</tr>
</tbody>
</table>

**Response body:**
The response body is a JSON representation of the org.flowable.eventregistry.model.ChannelModel.

    {
       "key": "myChannel",
       "category": "channel",
       "name": "My channel",
       "description": "My channel description",
       "channelType": "inbound",
       "type": "jms",
       "destination": "testQueue",
       "deserializerType": "json",
       "channelEventKeyDetection": {
          "fixedValue": "myEvent"
       }
    }

## Event Registry Service

### Send an event

    POST event-registry-runtime/event-instances

**Request body:**

   {
      "eventDefinitionKey": "myEvent",
      "eventPayload": {
          "customerName": "John Doe",
          "productName": "Laptop"
      },
      "tenantId": "tenant1"
   }
   
Instead of an event definition key it's also possible to use an event definition id.   

There's no response body when sending an event.

<table>
<caption>Send an event - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>201</p></td>
<td><p>Indicates the event has been sent.</p></td>
</tr>
</tbody>
</table>

## Engine

### Get Event Registry engine info

    GET event-registry-management/engine

Returns a read-only view of the Event Registry engine that is used in this REST-service.

**Success response body:**

    {
       "name":"default",
       "version":"6.6.0"
    }

<table>
<caption>Get engine information - Response codes</caption>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th>Response code</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>200</p></td>
<td><p>Indicates the engine information has been returned.</p></td>
</tr>
</tbody>
</table>
