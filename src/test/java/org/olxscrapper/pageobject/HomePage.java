package org.olxscrapper.pageobject;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.util.Objects;

public class HomePage {
    private final WebDriver driver;
    private final String BASE_URL = "https://www.olx.ba/pretraga?stanje=0&vrstapregleda=tabela&sort_order=desc&vrsta=samoprodaja&";

    public HomePage(final WebDriver driver) {
        Objects.requireNonNull(driver);

        this.driver = driver;
        PageFactory.initElements(this.driver, this);
    }

    public void loadHomePage() {
        driver.get(BASE_URL);
    }

    public void search(String searchText) {
        Objects.requireNonNull(searchText);
        driver.get(BASE_URL + searchText);
    }
}
