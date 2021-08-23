package org.olxscrapper.test;

import org.junit.Test;
import org.olxscrapper.common.Base;
import org.olxscrapper.pageobject.HomePage;

public class TuzlaLandSearchTest extends Base {
    private static final String LAND_QUERY_PARAMS = "&kanton=3&grad%5B%5D=4944&kategorija=29&id=2&stanje=0";

    @Override
    public String getSearchPageName() {
        return "Zemljiste Tuzla";
    }

    @Override
    public String getStorageFileName() {
        return "tuzla_land_list";
    }

    @Test
    public void findLandInTuzla() throws Exception {
        HomePage homePage = new HomePage(driver);

        homePage.loadHomePage();
        homePage.search(LAND_QUERY_PARAMS);

        findAndProcessNewArticles();
    }
}
