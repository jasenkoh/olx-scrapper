package org.olxscrapper.repository;

import org.olxscrapper.domain.ArticleSeed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ArticleSeedRepository extends CrudRepository<ArticleSeed, Long> {
    @Query(value = "SELECT * FROM article_seed WHERE status = 'NEW' ORDER BY random() LIMIT 100", nativeQuery = true)
    List<ArticleSeed> findNewRandom();
}
