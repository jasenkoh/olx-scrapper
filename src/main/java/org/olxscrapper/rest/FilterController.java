package org.olxscrapper.rest;

import org.olxscrapper.domain.Filter;
import org.olxscrapper.repository.FilterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class FilterController {
    private final FilterRepository filterRepository;

    public FilterController(FilterRepository filterRepository) {
        this.filterRepository = filterRepository;
    }

    @PostMapping("/filters")
    public ResponseEntity<Filter> addFilter(@RequestBody Filter filter) {
        filterRepository.save(filter);

        return ResponseEntity.ok(filter);
    }

    @DeleteMapping("/filters/{filterId}")
    public ResponseEntity<Void> removeFilter(@PathVariable("filterId") int filterId) {
        Optional<Filter> existingFilter = filterRepository.findById(filterId);

        if (!existingFilter.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        filterRepository.delete(existingFilter.get());
        return ResponseEntity.ok().build();
    }
}
