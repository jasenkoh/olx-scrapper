package org.olxscrapper.service;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.olxscrapper.config.WebDriverFactory;
import org.olxscrapper.domain.Article;
import org.olxscrapper.domain.ArticleSeed;
import org.olxscrapper.domain.ArticleSeedStatus;
import org.olxscrapper.domain.Filter;
import org.olxscrapper.repository.ArticleRepository;
import org.olxscrapper.repository.ArticleSeedRepository;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ScrapperService {
    private static final Logger logger = LoggerFactory.getLogger(ScrapperService.class);
    private static final String ARTICLE_TITLE_URL_FORMAT = "<a href=\"%s\">%s</a>";
    private static final String BASE_URL = "https://www.olx.ba/pretraga?stanje=0&vrstapregleda=tabela&sort_order=desc&vrsta=samoprodaja&";

    private final FilterRepository filterRepository;
    private final ArticleRepository articleRepository;
    private final ArticleSeedRepository articleSeedRepository;
    private final MailService mailService;


    public ScrapperService(ArticleRepository articleRepository,
                           MailService mailService,
                           ArticleSeedRepository articleSeedRepository,
                           FilterRepository filterRepository) {
        this.articleRepository = articleRepository;
        this.filterRepository = filterRepository;
        this.mailService = mailService;
        this.articleSeedRepository = articleSeedRepository;
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


    private final String BASE_ARTICLE_URL = "https://olx.ba/artikal/";

    /**
     * This method will process articles in chunks of 100 articles fetched from database.
     * It will process 8 chunks in parallel.
     * It will wait 500ms between each chunk to avoid overloading the database and update status of seed articles.
     */
    public void processArticlesInChunks() {
        int threadPoolSize = 8;

        ExecutorService executorService = Executors.newFixedThreadPool(8);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        for (int i = 0; i < threadPoolSize; i++) {
            scheduledExecutorService.schedule(() -> {
                executorService.submit(() -> processChunk(articleSeedRepository.findNewRandom()));
            }, i * 500, TimeUnit.MILLISECONDS);
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processChunk(List<ArticleSeed> articleSeeds)  {
        if (articleSeeds.isEmpty()) {
            logger.info("No articles found");
            return;
        }

        articleSeedRepository.saveAll(articleSeeds.stream().peek(articleSeed -> articleSeed.setStatus(ArticleSeedStatus.IN_PROGRESS)).collect(Collectors.toList()));

        try {
            for (ArticleSeed articleSeed : articleSeeds) {
                String articleUrl = BASE_ARTICLE_URL + articleSeed.getId();
                logger.info("Processing article: " + articleUrl);
                processArticlePage(articleUrl, articleSeed.getId());
            }
            logger.info("Finished visiting articles...");
        } catch (Exception e) {
            logger.info("Unhandled exception occurred for url: " + e);
        }

        processChunk(articleSeedRepository.findNewRandom());
    }

    private void processArticlePage(String url, Long id) {
        String title = "unknown";
        Article article = new Article(id, url, title);
        try {
            Document document = Jsoup.connect(url).get();
            title = document.title();

            if (title.toLowerCase().contains("opine")) {
                logger.info("Found article: " + title + " at " + url);
            }
            article.setTitle(title);
            articleRepository.save(article);
            articleSeedRepository.save(new ArticleSeed(id, ArticleSeedStatus.DONE));
        } catch (NoSuchElementException noSuchElementException) {
            logger.info("Article not found at: " + url);
            articleRepository.save(article);
        } catch (HttpStatusException e) {
            logger.info("Article not found at: " + url);
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
