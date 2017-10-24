package com.clicktracker;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.googlecode.objectify.ObjectifyService;

// custom imports
import java.io.PrintWriter;
import java.util.List;
import java.util.Date;

import com.clicktracker.model.Campaign;
import com.clicktracker.model.Admin;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Counter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

//import javax.json.*;

// AdminServlet handles admin pages => adding, deleting, updating and getting
// info about campaigns.
// We can access admin pages via
// /api/v1/admin/campaign/{campaignID/all/platform}
//
// Actions are executed depending on the type of request:
// GET: get additional info about campaigns
// POST: create new campaign
// PUT: update existing campaign
// DELETE: delete campaign
//
public class AdminServlet extends HttpServlet {

    // Get request on admin pages returns informations about that campaign
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        // check if admin is logged in
        Boolean ok = checkCredentials(req);
        if (!ok) {
            handleForbidden(resp);
            return;
        }

        String url = Utilities.getURLEnding(req);

        if (url.equals("all")) { // display all campaigns
            displayAllCampaigns(req, resp);
            return;
        } else if (url.equals("platforms")) { // display all platforms
            displayAllPlatforms(req, resp);
            return;
        }

        // fetch data about campaign with id parsed from url
        // campaignID is string, turning it into correct format for filtering
        Long id = Utilities.getCampaignID(req);
        // id could not be parsed from url
        if (id == null) {
            String msg = String.format("campaign with id %s does not exist", String.valueOf(id));
            handleBadRequest(resp, msg);
            return;
        }

        // get campaign entity from db
        Campaign campaign = ObjectifyService.ofy().load().type(Campaign.class).id(id).now();
        // campaign with such id does not exist, return 404
        if (campaign == null) {
            handleNotFound(resp);
            return;
        }

        Counter counter = ObjectifyService.ofy().load().type(Counter.class).filter("campaignID", id).first().now();
        Long clicks = 0L;
        if (counter != null) {
            clicks = counter.numOfClicks;
        }

        // campaign with such id exist, display informations about campaign to admin
        resp.setStatus(HttpServletResponse.SC_OK);
        Gson gson = new Gson();
        JsonElement jsonEl = gson.toJsonTree(campaign);
        jsonEl.getAsJsonObject().addProperty("clicks", clicks);
        out.print(gson.toJson(jsonEl));
        out.flush();
    }

    // displayAllCampaigns is used to display all campaigns for admin users
    //url: /api/v1/admin/campaigns/all
    //
    // This could be handled in separate file
    public void displayAllCampaigns(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();

        List<Campaign> campaigns = ObjectifyService.ofy().load().type(Campaign.class).list();
        if (campaigns == null) {
            out.print(new Gson().toJson(""));
            out.flush();
            return;
        }
        // push json array to client
        resp.setStatus(HttpServletResponse.SC_OK);
        JsonArray camp = new Gson().toJsonTree(campaigns).getAsJsonArray();
        JsonObject wrapper = new JsonObject();
        wrapper.add("campaigns:", camp);
        String json = new Gson().toJson(wrapper);
        out.print(json);
        out.flush();
        return;
    }

    // displayAllPlatforms is displaying data about all platforms
    // make sure to authenticate admin in outer function
    public void displayAllPlatforms(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        List<Platform> platforms = ObjectifyService.ofy().load().type(Platform.class).list();
        if (platforms == null) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            out.print(new Gson().toJson(""));
            out.flush();
            return;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        JsonArray platf = new Gson().toJsonTree(platforms).getAsJsonArray();
        JsonObject wrapper = new JsonObject();
        wrapper.add("platforms:", platf);
        out.print(wrapper);
        out.flush();
        return;
    }

    //
    // Post request on admin pages adds new campaign and returns ID of the
    // created campaign
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        Boolean ok = checkCredentials(req);
        if (!ok) {
            handleForbidden(resp);
            return;
        }

        // parse parameters from POST request
        // if parameters are missing, return bad request (json)
        String campaignName = req.getParameter("name");
        if (campaignName == null) {
            handleBadRequest(resp, "name parameter was not provided");
            return;
        }

        String redirectURL = req.getParameter("redirectURL");
        if (redirectURL == null) {
            handleBadRequest(resp, "redirectURL parameter was not provided");
            return;
        }

        String paramActive = req.getParameter("active");
        if (paramActive == null) {
            handleBadRequest(resp, "active parameter was not provided");
            return;
        }
        String plat = req.getParameter("platform");
        if (plat == null) {
            handleBadRequest(resp, "platform parameter was not provided");
            return;
        }

        List<Long> platforms = Utilities.getPlatforms(plat);

        Boolean active = Boolean.parseBoolean(paramActive);
        Date createdAt = new Date();

        // if everything is all right save campaign to database
        Campaign c = new Campaign(campaignName, redirectURL, platforms, active, createdAt);
        ObjectifyService.ofy().save().entity(c).now();
        Long cID = c.id;

        // return created campaign id
        resp.setStatus(HttpServletResponse.SC_CREATED);
        JsonObject json = new JsonObject();
        json.add("id", new Gson().toJsonTree(cID));
        out.print(json);
        out.flush();
    }

    // handling campaign delete request
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        Boolean ok = checkCredentials(req);
        if (!ok) {
            handleForbidden(resp);
            return;
        }

        Long campaignID = Utilities.getCampaignID(req);
        if (campaignID == null) {
            String msg = String.format("campaign with campaignID: %s does not exist", campaignID);
            handleBadRequest(resp, msg);
            return;
        }

        // check if campaign id actually exist
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).id(campaignID).now();
        if (c == null) {
            handleNotFound(resp);
            return;
        }

        // everything is ok delete campaign id = campaignID
        resp.setStatus(HttpServletResponse.SC_OK);
        ObjectifyService.ofy().delete().type(Campaign.class).id(campaignID).now();
    }

    // handling admin campaign updates
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Boolean ok = checkCredentials(req);
        if (!ok) {
            handleForbidden(resp);
            return;
        }

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        Long campaignID = Utilities.getCampaignID(req);
        Campaign campaign = ObjectifyService.ofy().load().type(Campaign.class).id(campaignID).now();
        String newName = req.getParameter("name");
        if (newName != null) {
            campaign.name = newName;
        }

        String newRedirectURL = req.getParameter("redirectURL");
        if (newRedirectURL != null) {
            campaign.redirectURL = newRedirectURL;
        }

        String newPlat = req.getParameter("platforms");
        if (newPlat != null) {
            List<Long> newPlatforms = Utilities.getPlatforms(newPlat);
            campaign.platforms = newPlatforms;
        }

        String active = req.getParameter("active");
        if (active != null) {
            campaign.active = Boolean.parseBoolean(active);
        }

        // saving campaign to db
        ObjectifyService.ofy().save().entity(campaign).now();

        // after successful patch return campaign data back to admin
        out.print(new Gson().toJson(campaign));
        out.flush();
    }

    // helper function for handling status code 404 not found:
    //ex: campaign id does not exist in db
    private void handleNotFound(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();
        JsonObject msg = new JsonObject();
        msg.add("statusCode", gson.toJsonTree(404));
        msg.add("message", gson.toJsonTree("This campaign does not exist"));
        out.print(new Gson().toJson(msg));
        out.flush();
    }

    // helper function fot handling status code 400
    // ex: campaign id could not be parsed from url
    private void handleBadRequest(HttpServletResponse resp, String errorMsg) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();
        JsonObject msg = new JsonObject();
        msg.add("statusCode", gson.toJsonTree(400));
        msg.add("message", gson.toJsonTree(errorMsg));
        out.print(msg);
        out.flush();
    }

    // handling forbidden error message, when admin cookie session is not
    // present
    private void handleForbidden(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);

        PrintWriter out = resp.getWriter();
        JsonObject json = new JsonObject();
        Gson gson = new Gson();
        json.add("statusCode", gson.toJsonTree(403));
        json.add("message", gson.toJsonTree("Forbidden"));
        out.print(json);
        out.flush();
    }

    // checkAuthenticaion checks if admin sent data are valid
    // ex. (username, password) combination
    private Boolean checkCredentials(HttpServletRequest req) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return false;
        }

        Object adminID = session.getAttribute("userID");
        if (adminID == null) {
            return false;
        }
        final Long AdminID = Long.parseLong(adminID.toString());

        Admin dbAdmin = ObjectifyService.ofy().load().type(Admin.class).id(AdminID).now();
        if (dbAdmin == null) {
            return false;
        }

        return true;
    }
}