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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.objectify.ObjectifyService;

// custom imports
import java.io.PrintWriter;
import java.io.BufferedReader;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;
import com.clicktracker.model.Campaign;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

//import javax.json.*;

// AdminServlet handles admin pages => adding, deleting, updating and getting
// info about campaigns.
// We can access admin pages via
// /api/v1/admin/campaign/{campaignID}
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
        // TODO: 1. check admin credentials
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        String url = Utilities.getURLEnding(req);

        // if campaignID == all, fetch data about all campaigns
        if (url.equals("all")) {
            List<Campaign> campaigns = ObjectifyService.ofy().load().type(Campaign.class).list();
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

        // fetch data for only one campaign
        // campaignID is string, turning it into correct format for filtering
        Long id = Utilities.getCampaignID(req);
        // id could not be parsed from url
        if (id == null) {
            handleBadRequest(resp);
            return;
        }

        // get campaign entity from db
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).id(id).now();
        // campaign with such id does not exist, return 404
        if (c == null) {
            handleNotFound(resp);
            return;
        }

        // campaign with such id exist, display informations about campaign to admin
        resp.setStatus(HttpServletResponse.SC_OK);
        String json = new Gson().toJson(c);
        out.print(json);
        out.flush();
    }

    //
    // Post request on admin pages adds new campaign and returns ID of the
    // created campaign
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        // TODO: 1. check admin credentials

        // parse parameters from post x-www-form-urlencoded
        // if parameters are missing, return bad request (json)
        String campaignName = req.getParameter("name");
        if (campaignName == null) {
            handleBadRequest(resp);
            System.out.println("campaignName is null");
            return;
        }

        String redirectURL = req.getParameter("redirectURL");
        if (redirectURL == null) {
            handleBadRequest(resp);
            System.out.println("redirectURL is null");
            return;
        }

        String paramActive = req.getParameter("active");
        if (paramActive == null) {
            handleBadRequest(resp);
            return;
        }

        Boolean active = Boolean.parseBoolean(paramActive);
        Date createdAt = new Date();

        // if everything is all right save campaign to database
        Campaign c = new Campaign(campaignName, redirectURL, active, createdAt);
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
        // TODO: 1 check credentials

        Long campaignID = Utilities.getCampaignID(req);
        if (campaignID == null) {
            handleBadRequest(resp);
            return;
        }

        // TODO: check if campaign id actually exist
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).id(campaignID).now();
        if (c == null) {
            handleNotFound(resp);
            return;
        }

        // everything is ok delete campaign id = campaignID
        resp.setStatus(HttpServletResponse.SC_OK);
        ObjectifyService.ofy().delete().type(Campaign.class).id(campaignID).now();
    }

    // option to
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }

    // handles status code 404 not found: ex: campaign id does not exist in db
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

    // handles status code 400 ex: campaign id could not be parsed from url
    private void handleBadRequest(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();
        JsonObject msg = new JsonObject();
        msg.add("statusCode", gson.toJsonTree(400));
        msg.add("message", gson.toJsonTree("Bad request"));
        out.print(msg);
        out.flush();
    }

    // handling forbidden error message
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
    private void checkAuthentication(HttpServletRequest req) throws IOException {
        //
    }
}