package com.clicktracker;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;

// custom imports
import java.io.PrintWriter;
import java.util.List;
import java.util.StringTokenizer;
import com.clicktracker.model.Admin;
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Click;
import com.clicktracker.model.Counter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

// AdminAuthServlet is taking care of authenticating admin client
// which happens via post request on:
//
// /api/v1/admin/auth/{login/logout}
public class AdminAuthServlet extends HttpServlet {

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String url = Utilities.getURLEnding(req);
        PrintWriter out = resp.getWriter();
        System.out.println(req.getSession());
        resp.setContentType("application/json");

        if (url.equals("login")) {
            Boolean ok = credentialsCheck(req, resp);
            if (!ok) {
                handleBadRequest(resp);
                return;
            }
            // TODO:
            // 1. set up cookie && create session in the database
            // return htto

            resp.setStatus(HttpServletResponse.SC_OK);
            out.print(new Gson().toJson(url));
            out.flush();
            return;
        }
    }

    // check admin credentials against credentials stored in the database
    public Boolean credentialsCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        if (name == null) {
            return false;
        }
        String pass = req.getParameter("password");
        if (pass == null) {
            return false;
        }

        // name and password are present, check in db for match
        Admin admin = ObjectifyService.ofy().load().type(Admin.class).filter("name", name).first().now();
        if (admin == null) { // admin does not exist
            return false;
        }
        if (!admin.active) { // admin is not active
            return false;
        }
        if (!admin.name.equals(name)) { // names do not match
            return false;
        }

        Boolean passHash = BCrypt.checkpw(pass, admin.password);
        if (!passHash) { // passwords do not match
            return false;
        }

        // sent credentials match to those in db; everything is ok.
        return true;
    }

    // handleBadRequest is helper function that returns bad request in json object
    // I am specifically returning generic error messages, so the potential attacker
    // do not know which admin username is active
    private void handleBadRequest(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.add("message", gson.toJsonTree("Bad request"));
        json.add("statusCode", gson.toJsonTree(400));
        out.print(json);
        out.flush();
    }
}