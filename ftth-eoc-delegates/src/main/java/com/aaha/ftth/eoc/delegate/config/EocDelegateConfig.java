package com.aaha.ftth.eoc.delegate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.aaha.ftth.eoc.delegate")
public class EocDelegateConfig {

    @Value("${eoc.ftth.product-spec-id:PLACEHOLDER_PRODUCT_SPEC}")
    private String productSpecId;

    @Value("${eoc.ftth.service-spec-id:PLACEHOLDER_SERVICE_SPEC}")
    private String serviceSpecId;

    @Value("${eoc.ftth.catalog-id:PLACEHOLDER_CATALOG}")
    private String catalogId;

    public String getProductSpecId() {
        return productSpecId;
    }

    public String getServiceSpecId() {
        return serviceSpecId;
    }

    public String getCatalogId() {
        return catalogId;
    }
}
