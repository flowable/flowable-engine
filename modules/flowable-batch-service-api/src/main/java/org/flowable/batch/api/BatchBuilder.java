package org.flowable.batch.api;

public interface BatchBuilder {

    BatchBuilder batchType(String batchType);
    
    BatchBuilder searchKey(String searchKey);
    
    BatchBuilder searchKey2(String searchKey2);
    
    BatchBuilder status(String status);
    
    BatchBuilder batchDocumentJson(String batchDocumentJson);
    
    BatchBuilder tenantId(String tenantId);
    
    Batch create();
    
    String getBatchType();
    
    String getSearchKey();
    
    String getSearchKey2();
    
    String getStatus();
    
    String getBatchDocumentJson();
    
    String getTenantId();
}
