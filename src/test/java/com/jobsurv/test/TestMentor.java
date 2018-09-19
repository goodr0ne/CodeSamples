package com.jobsurv.test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jobsurv.Reader;
import com.jobsurv.servlets.MainServlet;
import junit.framework.TestCase;
import org.apache.commons.lang.RandomStringUtils;

/**
 * Unit Test class for mentoring interactions
 * @author Yegor Scherbatkin.
 */
public class TestMentor extends TestCase {
  Gson gson;
  MainServlet mainServlet;
  Reader reader;
  Utilities utils;

  /**
   * sets test variables
   * @throws Exception
   */
  public void setUp() throws Exception{
    gson = new Gson();
    mainServlet = new MainServlet();
    reader = Reader.getInstance();
    utils = Utilities.getInstance();
  }

  /**
   * This test checks voting by unauthorized user
   * @throws Exception
   */
  public void testMentorVoteUnauthorized() throws Exception {
    String userLogin = "SOME_CORRECT_USER_LOGIN";
    String targetLogin = "SOME_CORRECT_TARGET_USER_LOGIN";
    String userToken = RandomStringUtils.randomAlphanumeric(18);
    String type = "add_vote";
    String output = utils.makeMentorRequest(type, userLogin, userToken,
            targetLogin, "1", "", "");
    boolean condition;
    String error;
    try {
      JsonObject outputObj = gson.fromJson(output,
              JsonElement.class).getAsJsonObject();
      error = outputObj.get("error").getAsString();
      condition = error.equals("authentication fail.");
    } catch (Exception e) {
      condition = false;
    }
    assertTrue(condition);
  }

  /**
   * This test checks voting for nonexistent user
   * @throws Exception
   */
  public void testMentorVoteUserNotExist() throws Exception {
    String userLogin = "SOME_CORRECT_USER_LOGIN";
    String targetLogin = RandomStringUtils.randomAlphanumeric(18);
    String userToken = utils.retrieveToken(userLogin);
    String type = "add_vote";
    assertTrue(!reader.loginExistCheck(targetLogin));
    String output = utils.makeMentorRequest(type, userLogin, userToken,
            targetLogin, "1", "", "");
    boolean condition;
    String error;
    try {
      JsonObject outputObj = gson.fromJson(output,
              JsonElement.class).getAsJsonObject();
      error = outputObj.get("error").getAsString();
      condition = error.equals("target user doesn't exist");
    } catch (Exception e) {
      condition = false;
    }
    assertTrue(condition);
  }

  /**
   * This test perform user self-deleting of mentoring
   * @throws Exception
   */
  public void testMentorSelfVote() throws Exception {
    String userLogin = "SOME_CORRECT_USER_LOGIN";
    String userToken = utils.retrieveToken(userLogin);
    String type = "add_vote";
    String output = utils.makeMentorRequest(type, userLogin, userToken,
            userLogin, "1", "", "");
    boolean condition;
    String error;
    try {
      JsonObject outputObj = gson.fromJson(output,
              JsonElement.class).getAsJsonObject();
      error = outputObj.get("error").getAsString();
      condition = error.equals("you cannot vote for yourself");
    } catch (Exception e) {
      condition = false;
    }
    assertTrue(condition);
  }
}
