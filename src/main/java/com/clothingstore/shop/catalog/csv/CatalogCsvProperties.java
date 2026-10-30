package com.clothingstore.shop.catalog.csv;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "catalog.csv")
public class CatalogCsvProperties {
    private Resource template;
    private int maxFileBytes = 1_048_576;
    private int maxRows = 1_000;

    public Resource getTemplate() { return template; }
    public void setTemplate(Resource template) { this.template = template; }
    public int getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(int maxFileBytes) { this.maxFileBytes = maxFileBytes; }
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
}
