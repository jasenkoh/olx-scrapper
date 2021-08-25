package org.olxscrapper.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.olxscrapper.config.WebDriverFactory;
import org.olxscrapper.domain.Article;
import org.olxscrapper.domain.Filter;
import org.olxscrapper.repository.ArticleRepository;
import org.olxscrapper.repository.FilterRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScrapperService {
    private static final String ARTICLE_TITLE_URL_FORMAT = "<a href=\"%s\">%s</a>";
    private static final String BASE_URL = "https://www.olx.ba/pretraga?stanje=0&vrstapregleda=tabela&sort_order=desc&vrsta=samoprodaja&";

    private final FilterRepository filterRepository;
    private final ArticleRepository articleRepository;
    private final MailService mailService;

    private WebDriver webDriver;

    public ScrapperService(ArticleRepository articleRepository,
        MailService mailService,
        FilterRepository filterRepository) {
        this.articleRepository = articleRepository;
        this.filterRepository = filterRepository;
        this.mailService = mailService;
    }

    public void scrapPages() {
        webDriver = WebDriverFactory.initWebDriver();

        filterRepository.findByActiveTrue().forEach(filter -> {
            log.info("Processing filter: " + filter.getSearchPageName() + " at " + LocalDateTime.now());

            try {
                webDriver.get(BASE_URL + filter.getQueryParams());
                processArticle(filter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        log.info("Finished scrapping pages...");
        webDriver.quit();
    }

    private void processArticle(Filter filter) throws IOException {
        List<Article> newArticles = new ArrayList<>();
        List<Article> existingArticles = articleRepository.findAll();

        Set<String> existingArticleIds = existingArticles
            .stream()
            .map(Article::getExternalId)
            .collect(Collectors.toSet());

        webDriver.findElements(By.className("artikal")).forEach(article -> {
            String articleId = article.getAttribute("id");
            if (StringUtils.isNotEmpty(articleId) && !existingArticleIds.contains(articleId)) {
                final Article newArticle = new Article(articleId, getAnchor(article), getTitle(article));
                newArticles.add(newArticle);
            }
        });

        if (!newArticles.isEmpty()) {
            log.info("Found total: " + newArticles.size() + " articles");
            buildAndSendEmail(filter, newArticles);

            articleRepository.saveAll(newArticles);
        } else {
            log.info("No articles found");
        }
    }

    private String getAnchor(WebElement element) {
        return element.findElement(By.tagName("a")).getAttribute("href");
    }

    private String getTitle(WebElement element) {
        return element.findElement(By.className("naslov")).findElement(By.tagName("a")).getText();
    }

    private void buildAndSendEmail(Filter filter, List<Article> articles) {
        StringBuilder mailContent = new StringBuilder();
        mailContent
            .append(webDriver.findElement(By.className("brojrezultata")).getText())
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
