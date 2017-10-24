package com.clicktracker;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
        resp.setContentType("application/json");

        // handling login
        if (url.equals("login")) {
            Credentials credentials = credentialsCheck(req, resp);
            if (!credentials.ok) {
                handleBadRequest(resp);
                return;
            }

            HttpSession session = req.getSession(true);
            Object userID = session.getAttribute("userID");
            if (userID == null) { // userID does not exist
                session.setAttribute("userID", credentials.adminId);
            }
            session.setMaxInactiveInterval(24 * 60 * 60);

            // return success msg to client
            resp.setStatus(HttpServletResponse.SC_OK);
            JsonObject json = new JsonObject();
            json.add("message", new Gson().toJsonTree("Successfully logged in"));
            out.print(json);
            out.flush();
            return;

            // handling logout
        } else if (url.equals("logout")) {
            resp.setContentType("application/json");
            HttpSession session = req.getSession(false);
            if (session == null) {
                handleBadRequest(resp);
                return;
            }

            session.invalidate();
            resp.setStatus(HttpServletResponse.SC_OK);
            JsonObject json = new JsonObject();
            json.add("message", new Gson().toJsonTree("Successfully logged out"));
            out.print(json);
            out.flush();
            return;
        }
    }

    // class for returning admin id and credentials bool from function
    final class Credentials {
        Long adminId;
        Boolean ok;

        public Credentials(Long adminId, Boolean ok) {
            this.adminId = adminId;
            this.ok = ok;
        }
    }

    // check admin credentials against credentials stored in the database
    public Credentials credentialsCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        Credentials credentialsFail = new Credentials(0L, false);

        if (name == null) {
            return credentialsFail;
        }
        String pass = req.getParameter("password");
        if (pass == null) {
            return credentialsFail;
        }

        // name and password are present, check in db for match
        Admin admin = ObjectifyService.ofy().load().type(Admin.class).filter("name", name).first().now();
        if (admin == null) { // admin does not exist
            return credentialsFail;
        }
        if (!admin.active) { // admin is not active
            return credentialsFail;
        }
        if (!admin.name.equals(name)) { // names do not match
            return credentialsFail;
        }

        Boolean passHash = BCrypt.checkpw(pass, admin.password);
        if (!passHash) { // passwords do not match
            return credentialsFail;
        }

        // sent credentials match to those in db; everything is ok.
        return new Credentials(admin.id, true);
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