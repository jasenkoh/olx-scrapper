package org.olxscrapper.rest;

import org.olxscrapper.domain.Filter;
import org.olxscrapper.repository.FilterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/filters")
public class FilterController {
    private final FilterRepository filterRepository;

    public FilterController(FilterRepository filterRepository) {
        this.filterRepository = filterRepository;
    }

    @GetMapping
    public ResponseEntity<List<Filter>> getAllFilters() {
        return ResponseEntity.ok(filterRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Filter> addFilter(@RequestBody Filter filter) {
        filterRepository.save(filter);

        return ResponseEntity.ok(filter);
    }

    @PostMapping("/{filterId}/{status}")
    public ResponseEntity<Void> toggleFilter(@PathVariable("filterId") int filterId, @PathVariable("status") boolean active) {
        Filter existingFilter = filterRepository.findById(filterId).orElse(null);

        if (existingFilter == null) {
            return ResponseEntity.notFound().build();
        }

        existingFilter.setActive(active);
        filterRepository.save(existingFilter);

        return ResponseEntity.ok().build();
    }
}
