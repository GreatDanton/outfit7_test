/**
 * Copyright 2014-2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//[START all]
package com.clicktracker;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import javax.servlet.ServletContextListener;

import javax.servlet.ServletContextEvent;
import org.mindrot.jbcrypt.BCrypt;

// json imports
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Click;
import com.clicktracker.model.Admin;
import java.util.Date;

/**
 * OfyHelper, a ServletContextListener, is setup in web.xml to run before a JSP is run.  This is
 * required to let JSP's access Ofy.
 **/
public class OfyHelper implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        // This will be invoked as part of a warmup request, or the first user request if no warmup
        // request.
        // custom register
        ObjectifyService.register(Campaign.class);
        ObjectifyService.register(Platform.class);
        ObjectifyService.register(Click.class);
        ObjectifyService.register(Admin.class);

        ObjectifyService.begin();

        registerAdmin();
        createCampaign();
    }

    // helper function for registering administrator of the click
    // tracking service
    private void registerAdmin() {
        // NOTE: the proper way of dealing with admins would be
        // adding the functionality to add, remove, update or disable them
        // via active table field.
        // .... I believe this is not in the scope of this project/test so I am
        // staying with only one admin for now.
        //
        System.out.println("######### APPLICATION WARMUP ##########");

        Admin ad = ObjectifyService.ofy().load().type(Admin.class).first().now();
        System.out.println(ad);

        // If admin does not exist create one
        // .... EXTRA NOTE: the proper way of creating new admin would be via configuration
        // file, but I am not sure if GAE supports reading & writing to file system
        // so we are stuck with redeploying the whole app for each admin password change
        if (ad == null) {
            String pass = "outfit7$Test321";
            // random salt for bcrypt
            String salt = BCrypt.gensalt();
            String passHash = BCrypt.hashpw(pass, salt);

            // save admin to admin table
            Admin admin = new Admin("Admin", passHash, true);
            ObjectifyService.ofy().save().entity(admin).now();
        }
    }

    // createCampaign creates first campaign in the database,
    // if no other campaign exist
    private void createCampaign() {
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).first().now();
        // if campaign does not exist in the database
        if (c == null) {
            String name = "My first campaign";
            String url = "http://www.google.com";
            Boolean active = true;
            Date date = new Date();
            Campaign campaign = new Campaign(name, url, active, date);
            ObjectifyService.ofy().save().entity(campaign).now();
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        // App Engine does not currently invoke this method.
    }
}
//[END all]
