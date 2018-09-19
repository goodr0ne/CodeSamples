package com.jobsurv.servlets;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * MainServlet class is a primal Servlet class, which maintain all incoming
 * requests for jobsurv.com site. Checks specified servlet type and redirect
 * request to this JobsurvServlet class.
 *
 * Author: Yegor Scherbatkin.
 */
public class MainServlet extends HttpServlet {
  ServletUtils utils = ServletUtils.getInstance();
  MentorServlet mentorServlet = new MentorServlet();

  /**
   * Called by the server (via the service method) to allow a servlet to handle
   * a GET request.
   *
   * @param req an {@link javax.servlet.http.HttpServletRequest} object
   * @param res an {@link javax.servlet.http.HttpServletResponse} object
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    this.process(req, res);
  }

  /**
   * Called by the server (via the service method) to allow a servlet to handle
   * a POST request.
   *
   * @param req an {@link javax.servlet.http.HttpServletRequest} object
   * @param res an {@link javax.servlet.http.HttpServletResponse} object
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    this.process(req, res);
  }

  /**
   * Main servlet function. Processes POST and GET requests.
   * Parameter "type" declares type of interaction (case in switch operator).
   *
   * Interactions: get profiles, delete, promote, demote, event log, mail log,
   * clear logs.
   *
   * @param request  An {@link javax.servlet.http.HttpServletRequest}
   *                 object that of the servlet.
   * @param response An {@link javax.servlet.http.HttpServletResponse}
   *                 object that contains the
   *                 response the servlet sends to the client.
   */
  private void process(HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
    try {
      long time = System.currentTimeMillis();
      boolean isConfirmation = false;
      Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
      response.setStatus(200);
      PrintWriter out = response.getWriter();
      response.setContentType("text/html; charset=UTF-8");
      response.setHeader("Cache-Control","no-cache");
      response.setHeader("Pragma","no-cache");
      response.setHeader("Cache-Control","no-store");
      String url = request.getRequestURL().toString();
      String[] urlWords = url.split("/");
      String servletType = urlWords[urlWords.length - 1];
      JsonObject object = utils.servletTypeCheck(servletType);
      if (StringUtils.isBlank(object.get("error").getAsString())) {
        switch (ServletType.valueOf(servletType.toUpperCase())) {
          case MENTOR:
            object = mentorServlet.process(request.getParameter("type"),
                    request.getParameter("login"),
                    request.getParameter("token"),
                    request.getParameter("mentor"),
                    request.getParameter("vote"),
                    request.getParameter("target"),
                    request.getParameter("mentorFlag"));
            utils.increaseCounter("User");
            break;
        }
      }
      object = utils.addPing(object, time);
      if (!StringUtils.isBlank(request.getParameter("pretty"))) {
        out.println(prettyGson.toJson(object));
      } else {
        out.println(object);
      }
      out.close();
    }
    catch (Exception e) {
      response.setStatus(200);
      PrintWriter out = response.getWriter();
      response.setContentType("text/html; charset=UTF-8");
      JsonObject object = new JsonObject();
      object.add("error", new Gson().toJsonTree(e));
      utils.increaseCounter("Error");
      out.println(object);
      out.close();
    }
  }
}
