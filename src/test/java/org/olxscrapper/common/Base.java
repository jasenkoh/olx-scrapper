package org.olxscrapper.common;

import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.olxscrapper.pageobject.OlxData;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Base implements SauceOnDemandSessionIdProvider {
    private static final String ARTICLE_TITLE_URL_FORMAT = "<a href=\"%s\">%s</a>";

    @Rule
    public final TestName name = new TestName() {
        public String getMethodName() {
            return String.format("%s", super.getMethodName());
        }
    };

    private String mailAccount;
    protected WebDriver driver;
    private String sessionId;

    public abstract String getSearchPageName();

    public abstract String getStorageFileName();

    @Before
    public void setUp() throws Exception {
        String browserName = System.getProperty("browserName");

        this.driver = BrowserFactory.getBrowser(browserName);
        this.mailAccount = System.getenv("OLX_EMAIL");
        this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();

        driver.manage().timeouts().implicitlyWait(Wait.EXPLICIT_WAIT, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
    }

    @Override
    public String getSessionId() {
        return this.sessionId;
    }

    public void save(String fileName, Set<String> articleIds) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(fileName, true));
        for (String articleId : articleIds)
            pw.println(articleId);

        pw.close();
    }

    public void findAndProcessNewArticles() throws IOException {
        Map<String, OlxData> newArticlesMap = new HashMap<>();
        Set<String> existingArticleIds = loadArticleIds(getStorageFileName());

        driver.findElements(By.className("artikal")).forEach(article -> {
            String articleId = article.getAttribute("id");
            if (StringUtils.isNotEmpty(articleId) && !existingArticleIds.contains(articleId)) {
                OlxData data = new OlxData(getAnchor(article), getTitle(article));
                newArticlesMap.put(articleId, data);
            }
        });

        if (!newArticlesMap.isEmpty()) {
            processArticles(newArticlesMap);
        }
    }

    public String getAnchor(WebElement element) {
        return element.findElement(By.tagName("a")).getAttribute("href");
    }

    public String getTitle(WebElement element) {
        return element.findElement(By.className("naslov")).findElement(By.tagName("a")).getText();
    }

    public Set<String> loadArticleIds(String fileName) throws IOException {
        Set<String> result;
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            result = lines.collect(Collectors.toSet());
        } catch (NoSuchFileException noEx) {
            result = Collections.emptySet();
        }

        return result;
    }

    private Session getMailSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailAccount, System.getenv("EMAIL_PASS"));
            }
        });
    }

    private void sendMail(String content) {
        try {
            Message message = new MimeMessage(getMailSession());
            message.setFrom(new InternetAddress(mailAccount));
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(mailAccount)
            );
            message.setSubject("Rezultati pretrage za: " + getSearchPageName());
            message.setContent(content, "text/html");

            Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void processArticles(Map<String, OlxData> newArticlesMap) throws FileNotFoundException {
        StringBuilder mailContent = new StringBuilder();
        mailContent
            .append(driver.findElement(By.className("brojrezultata")).getText())
            .append(", od toga je: ")
            .append("<b>")
            .append(newArticlesMap.size())
            .append("</b>")
            .append(" novih artikala")
            .append("<br/><br/>");

        newArticlesMap.keySet().forEach(articleId -> {
            OlxData olxData = newArticlesMap.get(articleId);
            mailContent
                .append("Artikal: ")
                .append(String.format(ARTICLE_TITLE_URL_FORMAT, olxData.getAnchor(), olxData.getTitle()))
                .append("<br/><br/>");
        });

        save(getStorageFileName(), newArticlesMap.keySet());
        sendMail(mailContent.toString());
    }
}