package com.jobsurv.test;

import com.google.gson.*;
import com.jobsurv.Reader;
import com.jobsurv.servlets.MainServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

/**
 *
 * @author Yegor Scherbatkin.
 */
public class Utilities {
  Gson gson;
  MainServlet mainServlet;
  Reader reader;

  /**
   * Default constructor, used only once in creating singleton instance phase
   */
  public Utilities () {
    reader = Reader.getInstance();
    mainServlet = new MainServlet();
    gson = new Gson();
  }

  /**
   * Creates singleton Utilities instance when tests are loaded to jvm
   */
  public static final Utilities instance = new Utilities();

  /**
   * Get instance for singleton Utilities class
   *
   * @return returns current instance of singleton Utilities
   */
  public static Utilities getInstance() {
    return instance;
  }

  protected String makeMentorRequest(String type, String login,
                                     String token, String mentor,
                                     String vote, String target,
                                     String mentorFlag) throws IOException {
    String url = "https://localhost/mentor";
    StringBuffer buffer = new StringBuffer();
    buffer.append(url);
    StringWriter writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getRequestURL()).thenReturn(buffer);
    when(request.getParameter("type")).thenReturn(type);
    when(request.getParameter("login")).thenReturn(login);
    when(request.getParameter("token")).thenReturn(token);
    when(request.getParameter("target")).thenReturn(target);
    when(request.getParameter("mentor")).thenReturn(mentor);
    when(request.getParameter("vote")).thenReturn(vote);
    when(request.getParameter("mentorFlag")).thenReturn(mentorFlag);
    when(response.getWriter()).thenReturn(printWriter);
    mainServlet.doPost(request, response);
    String output = writer.toString();
    verify(request, atLeastOnce()).getRequestURL();
    verify(request, atLeastOnce()).getParameter("type");
    verify(request, atLeastOnce()).getParameter("login");
    verify(request, atLeastOnce()).getParameter("token");
    verify(request, atLeastOnce()).getParameter("target");
    verify(request, atLeastOnce()).getParameter("mentor");
    verify(request, atLeastOnce()).getParameter("vote");
    verify(request, atLeastOnce()).getParameter("mentorFlag");
    verify(response, atLeastOnce()).getWriter();
    return output;
  }
}
