package org.olxscrapper.domain;

import javax.persistence.*;

@Entity
public class Filter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String queryParams;

    @Column(unique = true)
    private String searchPageName;

    public Filter() {
    }

    public Filter(Integer id, String queryParams, String searchPageName) {
        this.id = id;
        this.queryParams = queryParams;
        this.searchPageName = searchPageName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public String getSearchPageName() {
        return searchPageName;
    }

    public void setSearchPageName(String searchPageName) {
        this.searchPageName = searchPageName;
    }
}
