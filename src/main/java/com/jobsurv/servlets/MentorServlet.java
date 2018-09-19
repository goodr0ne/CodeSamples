package com.jobsurv.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jobsurv.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * Servlet class for mentor interactions
 * Author: Yegor Scherbatkin.
 */
public class MentorServlet {
  Serializer serializer = Serializer.getInstance();
  Reader reader = Reader.getInstance();
  ServletUtils utils = ServletUtils.getInstance();
  Gson gson = new Gson();
  Statter stats = Statter.getInstance();

  /**
   * Add vote interaction. Adds vote for or against (+1 or -1 integer)
   * to specified user from interaction user, Checks and rewrites current vote
   * for specified user from interaction user. String is parsed to int and
   * transformed to +1/0/-1 value.
   *
   * @param login      user login
   * @param token      user authorization token
   * @param target     user, who vote rating will be changed
   * @param voteRating parsed to int string with vote for or against user
   * @return resultJson JSONObject with possible error, error_code values
   */
  public JsonObject addVote(String login, String token, String target,
                            String voteRating) throws IOException {
    /*

      Refactoring notes: this function is overgrown.
    */
    JsonObject object = utils.authCheck(login, token);
    if (!StringUtils.isBlank(object.get("error").getAsString())) {
      return object;
    }
    if (StringUtils.isBlank(target)) {
      object.addProperty("error", "target user parameter is blank");
      return object;
    }
    if (!reader.loginExistCheck(target)) {
      object.addProperty("error", "target user doesn't exist");
      return object;
    }
    if (target.equals(login)) {
      object.addProperty("error", "you cannot vote for yourself");
      return object;
    }
    int voteRate;
    if (StringUtils.isBlank(voteRating)) {
      voteRate = 1;
    } else {
      try {
        voteRate = Integer.parseInt(voteRating);
      } catch (Exception e) {
        voteRate = 1;
      }
    }
    if (voteRate < 0) {
      voteRate = -1;
    } else {
      voteRate = 1;
    }
    JsonArray featured = serializer.getFeaturedMentors(login);
    boolean isFeatured = false;
    if (featured.size() > 0) {
      for (int i = 0; i < featured.size(); i++) {
        if (featured.get(i).getAsString().equals(target)) {
          isFeatured = true;
          break;
        }
      }
    }
    if (isFeatured) {
      if (voteRate == -1) {
        serializer.deleteFeaturedMentor(login, target);
        utils.addFlash(login, "user " + target + " unfeatured", "this " +
                "user is removed from featured list",
                "user featured deleted");
      }
    } else {
      if (voteRate == 1) {
        serializer.addFeaturedMentor(login, target);
        utils.addFlash(login, "user " + target + " featured", "this user " +
                "is added to featured list", "user featured deleted");
        stats.produceUserStatsIncreasing(login, "users_interesting", 1);
      }
    }
    JsonObject vote = new JsonObject();
    vote.addProperty("login", login);
    vote.addProperty("vote", voteRate);
    JsonArray votes = serializer.getVotes(target);
    if (votes.size() > 0) {
      for (int i = 0; i < votes.size(); i++) {
        JsonObject oldVote = votes.get(i).getAsJsonObject();
        if (oldVote.get("login").getAsString().equals(login)) {
          int old = Integer.parseInt(oldVote.get("vote").getAsString());
          if (old == voteRate) {
            if (voteRate == 1) {
              object.addProperty("error", "you already voted for this user, " +
                      "but you can vote against");
              utils.addFlash(login, "already voted for " + target,
                      object.get("error").getAsString(), "vote failed");
            } else {
              object.addProperty("error", "you already voted against this " +
                      "user, but you can vote for him");
              utils.addFlash(login, "already voted against " + target,
                      object.get("error").getAsString(), "vote failed");
            }
            return object;
          }
        }
      }
    }
    serializer.addVote(target, vote);
    String type = "new vote";
    String title = login + " voted for you";
    String description = "You have received a thumbs up vote.";
    if (!(voteRate == 1)) {
      title = login + " voted against you";
      description = "You have received a thumbs down vote. Please contact " +
              "us if you feel this is an error or want to dispute.  ";
    }
    utils.addFlash(target, title, description, type);
    if (voteRate == 1) {
      stats.produceUserStatsIncreasing(login, "votes_for", 1);
      title = "Your thumbs up vote for " + target + " was received.";
    } else {
      stats.produceUserStatsIncreasing(login, "votes_against", 1);
      title = "Your thumbs down vote for " + target + " was received.";
    }
    description = "you always can change your vote";
    utils.addFlash(login, title, description, type);
    stats.produceUserStatsIncreasing(login, "time_action",
            System.currentTimeMillis());
    stats.writeUserAction(login, "vote added", "success",
            "target - " + target + ", vote - " + voteRate);
    return object;
  }
}
