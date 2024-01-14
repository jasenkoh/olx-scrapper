package org.olxscrapper.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "article_seed", indexes = {
        @Index(name = "article_seed_id_status_idx", columnList = "status"),
        @Index(name = "article_seed_id_idx", columnList = "id")
}, uniqueConstraints = @UniqueConstraint(columnNames = "id"))
@Getter
@Setter
@NoArgsConstructor
public class ArticleSeed {
    @Id
    private Long id;
    @Enumerated(EnumType.STRING)
    private ArticleSeedStatus status;

    public ArticleSeed(Long id, ArticleSeedStatus status) {
        this.id = id;
        this.status = status;
    }
}
