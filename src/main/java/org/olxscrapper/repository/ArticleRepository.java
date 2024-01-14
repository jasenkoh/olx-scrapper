package org.olxscrapper.repository;

import org.olxscrapper.domain.Article;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ArticleRepository extends CrudRepository<Article, Long> {
    List<Article> findAll();
    @Query(value = "SELECT * FROM article ORDER BY external_id DESC LIMIT 1", nativeQuery = true)
    Article findDistinctTopByExternalId();

    @Query(value = "SELECT * FROM article WHERE external_id BETWEEN ?1 AND ?2 ORDER BY external_id DESC LIMIT 1", nativeQuery = true)
    Article findDistinctTopBetweenExternalId(Long start, Long end);
}
