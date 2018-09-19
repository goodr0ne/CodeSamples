package com.jobsurv.test.selenium;

import junit.framework.TestCase;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class TestSeleniumHello extends TestCase {

  public void testTitle() {
    System.setProperty("webdriver.chrome.driver","/usr/local/bin/chromedriver");
    ChromeOptions opts = new ChromeOptions();
    opts.addArguments("--no-sandbox");
    WebDriver driver = new ChromeDriver(opts);
    String baseUrl = "https://192.168.0.105";
    String expectedTitle = "Jobsurv";
    driver.get(baseUrl);
    try {
      Thread.sleep(5000);
    } catch (Exception ignored) {}
    String actualTitle = driver.getTitle();
    assertTrue(actualTitle.contentEquals(expectedTitle));
    driver.close();
  }
}
