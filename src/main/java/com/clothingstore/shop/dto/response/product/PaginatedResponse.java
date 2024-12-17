package com.clothingstore.shop.dto.response.product;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> items;
    private int totalRecords;

    public PaginatedResponse(List<T> items, int totalRecords) {
        this.items = items;
        this.totalRecords = totalRecords;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }
}

