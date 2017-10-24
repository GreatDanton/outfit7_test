package com.clicktracker;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.objectify.ObjectifyService;

// custom imports
import java.io.PrintWriter;
import java.util.List;
import java.util.StringTokenizer;
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Click;
import com.clicktracker.model.Counter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

// Handling click tracking and redirecting users
//
// GET - is redirecting
// POST - is returning JSON object {href: redirectURL}
//
// See README.txt file for an explanation why
//
public class ClickTrackerServlet extends HttpServlet {

    final String mainWebsite = "http://www.outfit7.com";

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

        // store client click in db
        storeClick(req);

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

        // campaign exists in db, store client click into db
        storeClick(req);

        // return redirectURL
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = resp.getWriter();
        JsonObject json = new JsonObject();
        json.add("redirectURL", new Gson().toJsonTree(c.redirectURL));
        out.print(json);
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
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        json.add("message", gson.toJsonTree("This campaign does not exist"));
        json.add("statusCode", gson.toJsonTree(404));
        json.add("redirectURL", gson.toJsonTree(mainWebsite));
        out.print(json);
        out.flush();
    }

    // storeClick is helper function for storing click request
    // into database
    private void storeClick(HttpServletRequest req) throws IOException {
        final Long campaignID = Utilities.getCampaignID(req);
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
        } else {
            clientIP = new StringTokenizer(xfwh, ",").nextToken().trim();
        }

        // TODO: Code bellow should be handled inside transaction
        // ex: if one of the db actions fail the whole transaction should fail
        // as well. So far I couldn't figure out how to build a transaction with
        // GAE datastore so I am leaving it like that for now.

        // store click info to database
        Click click = new Click(campaignID, clientIP, userAgent, date);
        ObjectifyService.ofy().save().entity(click);

        // add counter to counter table (GAE does not support PUT, so we have
        // to delete old row and insert new counter row)
        Counter counter = ObjectifyService.ofy().load().type(Counter.class).filter("campaignID", campaignID).first()
                .now();

        // if counter row does not exist add new row (that way we do not have
        // to insert new counter on each campaign insert)
        if (counter == null) {
            Counter newCounter = new Counter(campaignID, 1L);
            ObjectifyService.ofy().save().entity(newCounter);
            return;
        }

        // counter already exists, add one to numOfClick
        // update is only possible via new save of the same entity
        counter.numOfClicks = counter.numOfClicks + 1;
        ObjectifyService.ofy().save().entity(counter);
    }

    // getCampaign returns campaign object with chosen campaignID
    private Campaign getCampaign(Long campaignID) {
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).id(campaignID).now();
        return c;
    }
}