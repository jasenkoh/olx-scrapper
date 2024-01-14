package org.olxscrapper.service;

import org.apache.commons.lang3.StringUtils;
import org.olxscrapper.config.WebDriverFactory;
import org.olxscrapper.domain.Article;
import org.olxscrapper.domain.Filter;
import org.olxscrapper.domain.IndexChunk;
import org.olxscrapper.repository.ArticleRepository;
import org.olxscrapper.repository.FilterRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ScrapperService {
    private static final Logger logger = LoggerFactory.getLogger(ScrapperService.class);
    private static final String ARTICLE_TITLE_URL_FORMAT = "<a href=\"%s\">%s</a>";
    private static final String BASE_URL = "https://www.olx.ba/pretraga?stanje=0&vrstapregleda=tabela&sort_order=desc&vrsta=samoprodaja&";

    private final FilterRepository filterRepository;
    private final ArticleRepository articleRepository;
    private final MailService mailService;


    public ScrapperService(ArticleRepository articleRepository,
                           MailService mailService,
                           FilterRepository filterRepository) {
        this.articleRepository = articleRepository;
        this.filterRepository = filterRepository;
        this.mailService = mailService;
    }

    public void scrapPages() {
        WebDriver webDriver = WebDriverFactory.initWebDriver();

        filterRepository.findByActiveTrue().forEach(filter -> {
            logger.info("Processing filter: " + filter.getSearchPageName() + " at " + LocalDateTime.now());

            try {
                webDriver.get(BASE_URL + filter.getQueryParams());
                processArticle(filter, webDriver);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        logger.info("Finished scrapping pages...");
        webDriver.quit();
    }


    public void processArticlesInChunks() {
        Long endArticleId = 56126734L;
        Long baseArticleId = 50000000L;
        int chunkSize = 1000000;

        List<IndexChunk> chunks = splitIntoChunks(baseArticleId, endArticleId, chunkSize);
        ExecutorService executorService = Executors.newFixedThreadPool(chunks.size());

        for (IndexChunk chunk : chunks) {
            executorService.submit(() -> {
                try {
                    processChunk(chunk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static List<IndexChunk> splitIntoChunks(Long startIndex, Long endIndex, long chunkSize) {
        List<IndexChunk> chunks = new ArrayList<>();

        for (long i = startIndex; i <= endIndex; i += chunkSize) {
            long end = Math.min(i + chunkSize - 1, endIndex);
            chunks.add(new IndexChunk(i, end));
        }

        return chunks;
    }

    private void processChunk(IndexChunk chunk) throws IOException {
        WebDriver webDriver = WebDriverFactory.initWebDriver();
        Article lastArticle = articleRepository.findDistinctTopBetweenExternalId(chunk.getStart(), chunk.getEnd());
        long startArticleId = lastArticle != null ? lastArticle.getExternalId() + 1 : chunk.getStart();
        try {
            for (long i = startArticleId; i <= chunk.getEnd(); i++) {
                String BASE_ARTICLE_URL = "https://olx.ba/artikal/";
                String articleUrl = BASE_ARTICLE_URL + i;
                logger.info("Processing article: " + articleUrl);
                webDriver.get(articleUrl);
                processArticlePage(webDriver, articleUrl, i);
            }
            logger.info("Finished visiting articles...");
        } finally {
            webDriver.quit();
        }
    }

    private void processArticlePage(WebDriver webDriver, String url, Long id) {
        String title = "unknown";
        Article article = new Article(id, url, title);
        try {
            title = webDriver.findElement(By.className("main-title-listing")).getText();
            if (title.toLowerCase().contains("opine")) {
                logger.info("Found article: " + title + " at " + url);
            }
            article.setTitle(title);
            articleRepository.save(article);
        } catch (NoSuchElementException noSuchElementException) {
            logger.info("Article not found at: " + url);
            articleRepository.save(article);
        } catch (Exception e) {
            logger.info("Unhandled exception occurred for url: " + url, e);
        }
    }

    private void processArticle(Filter filter, WebDriver webDriver) throws IOException {
        List<Article> newArticles = new ArrayList<>();
        List<Article> existingArticles = articleRepository.findAll();

        Set<Long> existingArticleIds = existingArticles
                .stream()
                .map(Article::getExternalId)
                .collect(Collectors.toSet());

        webDriver.findElements(By.className("artikal")).forEach(article -> {
            String articleId = article.getAttribute("id");
            if (StringUtils.isNotEmpty(articleId) && !existingArticleIds.contains(Long.valueOf(articleId))) {
                final Article newArticle = new Article(Long.valueOf(articleId), getAnchor(article), getTitle(article));
                newArticles.add(newArticle);
            }
        });

        if (!newArticles.isEmpty()) {
            logger.info("Found total: " + newArticles.size() + " articles");
            String numberOfElements = webDriver.findElement(By.className("brojrezultata")).getText();
            buildAndSendEmail(filter, newArticles, numberOfElements);

            articleRepository.saveAll(newArticles);
        } else {
            logger.info("No articles found");
        }
    }

    private String getAnchor(WebElement element) {
        return element.findElement(By.tagName("a")).getAttribute("href");
    }

    private String getTitle(WebElement element) {
        return element.findElement(By.className("naslov")).findElement(By.tagName("a")).getText();
    }

    private void buildAndSendEmail(Filter filter, List<Article> articles, String numberOfElements) {
        StringBuilder mailContent = new StringBuilder();
        mailContent
                .append(numberOfElements)
                .append(", od toga je: ")
                .append("<b>")
                .append(articles.size())
                .append("</b>")
                .append(" novih artikala")
                .append("<br/><br/>");

        articles.forEach(article -> mailContent
                .append("Artikal: ")
                .append(String.format(ARTICLE_TITLE_URL_FORMAT, article.getAnchor(), article.getTitle()))
                .append("<br/><br/>"));

        mailService.sendMail("Rezultati pretrage za: " + filter.getSearchPageName(), mailContent.toString());
    }
}
