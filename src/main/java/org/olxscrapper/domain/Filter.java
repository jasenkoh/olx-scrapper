package org.olxscrapper.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Filter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String queryParams;

    @Column(unique = true)
    private String searchPageName;

    @Column(name = "isActive", columnDefinition = "boolean default true")
    private boolean active;

    public Filter(Integer id, String queryParams, String searchPageName) {
        this.id = id;
        this.queryParams = queryParams;
        this.searchPageName = searchPageName;
    }
}
