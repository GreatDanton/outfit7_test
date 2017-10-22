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
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.List;
import java.util.Date;
import com.clicktracker.model.Campaign;
import com.google.gson.Gson;

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
        // 1. check admin credentials
        // 2. if everything is allright get campaign id

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        String url = Utilities.getURLEnding(req);

        // if campaignID == all, fetch data about all campaigns
        if (url.equals("all")) {
            List<Campaign> campaigns = ObjectifyService.ofy().load().type(Campaign.class).list();
            // push json array to client
            String json = new Gson().toJson(campaigns);
            out.print(json);
            out.flush();
            return;
        }

        // fetch data for only one campaign
        // campaignID is string, turning it into correct format for filtering
        Long id = Long.parseLong(url);
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).id(id).now();
        if (c == null) { // campaign with such id does not exist, return error
            // TODO: handle error
        }
        String json = new Gson().toJson(c);
        out.print(json);
        out.flush();
    }

    // Post request on admin pages adds new campaign and returns ID of the
    // created campaign
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
    }

    // checkAuthenticaion checks if admin sent data are valid
    // ex. (username, password) combination
    private void checkAuthentication(HttpServletRequest req) throws IOException {
        //
    }
}