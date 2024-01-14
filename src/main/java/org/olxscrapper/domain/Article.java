package org.olxscrapper.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "article", uniqueConstraints = @UniqueConstraint(columnNames = "external_id"))
@Getter
@Setter
@NoArgsConstructor
public class Article {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", length = 36)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    private String anchor;

    private String title;

    public Article(Long externalId, String anchor, String title) {
        this.id = UUID.randomUUID();
        this.externalId = externalId;
        this.anchor = anchor;
        this.title = title;
    }
}
