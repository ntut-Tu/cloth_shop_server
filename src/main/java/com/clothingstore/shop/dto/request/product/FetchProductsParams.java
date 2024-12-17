package com.clothingstore.shop.dto.request.product;

public class FetchProductsParams {
    private int page;
    private int pageSize;
    private String category;
    private String sort;
    private String search;

    public FetchProductsParams(int page, int pageSize, String category, String sort, String search) {
        this.page = page;
        this.pageSize = pageSize;
        this.category = category;
        this.sort = sort;
        this.search = search;
    }

    // Getters and Setters
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}

