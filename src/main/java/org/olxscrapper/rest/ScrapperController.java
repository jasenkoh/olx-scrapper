package org.olxscrapper.rest;

import org.olxscrapper.service.ScrapperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScrapperController {

    private final ScrapperService scrapperService;

    public ScrapperController(ScrapperService scrapperService) {
        this.scrapperService = scrapperService;
    }

    @GetMapping("/scrap-pages")
    public ResponseEntity<Void> scrapPages() {
        scrapperService.scrapPages();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/visit-article")
    public ResponseEntity<Void> visitArticle() {
        scrapperService.processArticlesInChunks();
        return ResponseEntity.ok().build();
    }
}
