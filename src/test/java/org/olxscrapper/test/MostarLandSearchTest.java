package org.olxscrapper.test;

import org.junit.Test;
import org.olxscrapper.common.Base;
import org.olxscrapper.pageobject.HomePage;

public class MostarLandSearchTest extends Base {
    private static final String LAND_QUERY_PARAMS = "&kanton=7&grad%5B%5D=3017&kategorija=29&id=2&stanje=0";

    @Override
    public String getSearchPageName() {
        return "Zemljiste Mostar";
    }

    @Override
    public String getStorageFileName() {
        return "mostar_land_list";
    }

    @Test
    public void findLandInMostar() throws Exception {
        HomePage homePage = new HomePage(driver);

        homePage.loadHomePage();
        homePage.search(LAND_QUERY_PARAMS);

        findAndProcessNewArticles();
    }
}
