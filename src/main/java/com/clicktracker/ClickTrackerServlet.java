package com.clicktracker;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.objectify.ObjectifyService;

// custom imports
import java.io.PrintWriter;
import org.json.simple.JSONObject;
import java.util.List;
import com.clicktracker.model.Admin;
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Click;

// Handling click tracking and redirecting users
//
// GET - is redirecting
// POST - is returning JSON object {href: redirectURL}
//
// See README.txt file for an explanation why
//
public class ClickTrackerServlet extends HttpServlet {

    String mainWebsite = "http://www.outfit7.com";

    // Obviously querying database for every GET request is a terrible thing to do
    // The proper way to handle that is via querying memcache or redis (in memory db),
    // however Google app engine claims their datastore is caching entities
    // by default that's why I left it like that. The same goes for post request
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long campaignID = Utilities.getCampaignID(req);
        if (campaignID == null) { // campaignID could not be parsed from url
            handleGet404(resp);
            return;
        }
        Campaign c = getCampaign(campaignID);
        // requested url does not exist, redirect to main page
        if (c == null) {
            handleGet404(resp);
            return;
        }

        // requested campaign id does exist in db
        // display redirect message before the user is redirected;
        PrintWriter out = resp.getWriter();
        out.print("Redirecting...");
        out.flush();

        //  redirect to url stored in db
        resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
        resp.sendRedirect(c.redirectURL);
    }

    // same as get request
    // see README.txt for explanation why I am handling POST as well
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long campaignID = Utilities.getCampaignID(req);
        if (campaignID == null) { // campaignID could not be parsed from url
            handlePost404(resp);
            return;
        }

        // check if id from url exists in database
        Campaign c = getCampaign(campaignID);
        if (c == null) {
            handlePost404(resp);
            return;
        }

        // Campaign exists in db, return redirectURL
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = resp.getWriter();
        JSONObject returnJSON = new JSONObject();
        returnJSON.put("redirectURL", c.redirectURL);
        out.print(returnJSON);
        out.flush();
    }

    // handles404 handles setting status and redirecting to main page
    // when the campaignID could not be found (in db or url).
    private void handleGet404(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.sendRedirect(mainWebsite);
    }

    // handles 404 response on click tracker post request
    private void handlePost404(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

        PrintWriter out = resp.getWriter();
        JSONObject json = new JSONObject();
        json.put("message", "This campaign does not exist");
        json.put("errorCode", 404);
        out.print(json);
        out.flush();
    }

    // storeClick is helper function for storing click request
    // into database
    private void storeClick(HttpServletRequest req) throws IOException {
        Long campaignID = Utilities.getCampaignID(req);
        if (campaignID == null) {
            return;
        }

        String userAgent = req.getHeader("User-Agent"); // info about client device
        Date date = new Date();

        String clientIP = "";
        // it's tricky to get client ip as it's not always present
        String xfwh = req.getHeader("X-Forwarded-For");
        if (xfwh == null) {
            clientIP = req.getRemoteAddr();
            System.out.println("XFWH == NULL");
            System.out.println(clientIP);
        }
        System.out.println(xfwh);

        // store click info to database
        Click click = new Click(campaignID, clientIP, userAgent, date);
        ObjectifyService.ofy().save().entity(click).now();
    }

    // getCampaign returns campaign object with chosen campaignID
    private Campaign getCampaign(Long campaignID) {
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).id(campaignID).now();
        return c;
    }
}