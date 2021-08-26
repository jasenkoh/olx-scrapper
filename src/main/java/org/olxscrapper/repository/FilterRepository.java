package org.olxscrapper.repository;

import org.olxscrapper.domain.Filter;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FilterRepository extends CrudRepository<Filter, Integer> {
    List<Filter> findByActiveTrue();
    List<Filter> findAll();
}
