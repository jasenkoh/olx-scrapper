package org.olxscrapper.service;

import org.apache.commons.lang3.StringUtils;
import org.olxscrapper.domain.Article;
import org.olxscrapper.domain.Filter;
import org.olxscrapper.repository.ArticleRepository;
import org.olxscrapper.repository.FilterRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScrapperService {
    private static final String ARTICLE_TITLE_URL_FORMAT = "<a href=\"%s\">%s</a>";
    private static final String BASE_URL = "https://www.olx.ba/pretraga?stanje=0&vrstapregleda=tabela&sort_order=desc&vrsta=samoprodaja&";
    private final WebDriver webDriver;
    private final FilterRepository filterRepository;
    private final ArticleRepository articleRepository;
    private final String mailAccount;
    private final String emailPass;

    public ScrapperService(ArticleRepository articleRepository,
        Environment environment,
        WebDriver webDriver,
        FilterRepository filterRepository) {
        this.articleRepository = articleRepository;
        this.webDriver = webDriver;
        this.filterRepository = filterRepository;
        this.mailAccount = environment.getProperty("OLX_EMAIL");
        this.emailPass = environment.getProperty("EMAIL_PASS");
    }

    public void scrapPages() {
        filterRepository.findAll().forEach(filter -> {
            System.out.println("Processing filter: " + filter.getSearchPageName());

            try {
                webDriver.get(BASE_URL + filter.getQueryParams());
                processArticle(filter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void processArticle(Filter filter) throws IOException {
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
            System.out.println("Found total: " + newArticles.size() + " articles");
            processArticles(filter, newArticles);
        }
    }

    private String getAnchor(WebElement element) {
        return element.findElement(By.tagName("a")).getAttribute("href");
    }

    private String getTitle(WebElement element) {
        return element.findElement(By.className("naslov")).findElement(By.tagName("a")).getText();
    }

    private Session getMailSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailAccount, emailPass);
            }
        });
    }

    private void sendMail(Filter filter, String content) {
        try {
            Message message = new MimeMessage(getMailSession());
            message.setFrom(new InternetAddress(mailAccount));
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(mailAccount)
            );
            message.setSubject("Rezultati pretrage za: " + filter.getSearchPageName());
            message.setContent(content, "text/html");

            Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void processArticles(Filter filter, List<Article> articles) throws FileNotFoundException {
        StringBuilder mailContent = new StringBuilder();
        mailContent
            .append(webDriver.findElement(By.className("brojrezultata")).getText())
            .append(", od toga je: ")
            .append("<b>")
            .append(articles.size())
            .append("</b>")
            .append(" novih artikala")
            .append("<br/><br/>");

        articles.forEach(article -> {
            mailContent
                .append("Artikal: ")
                .append(String.format(ARTICLE_TITLE_URL_FORMAT, article.getAnchor(), article.getTitle()))
                .append("<br/><br/>");
        });

        articleRepository.saveAll(articles);
        sendMail(filter, mailContent.toString());
    }
}
