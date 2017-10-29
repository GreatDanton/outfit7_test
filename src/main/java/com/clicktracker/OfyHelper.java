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

import com.googlecode.objectify.ObjectifyService;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

// json imports
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Click;
import com.clicktracker.model.Counter;
import com.clicktracker.model.Admin;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

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
        ObjectifyService.register(Counter.class);
        ObjectifyService.register(Admin.class);

        ObjectifyService.begin();

        // 1 admin, 2 platforms and one campaign is created when the
        // software is started (if they do not exist already).
        registerAdmin();
        createPlatforms();
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
        System.out.println("######### APPLICATION SETUP ##########");

        Admin ad = ObjectifyService.ofy().load().type(Admin.class).first().now();
        System.out.println(ad);

        // If admin does not exist create one
        if (ad == null) {
            String adminName = "";
            String adminPass = "";

            // read config file
            try {
                File configFile = new File("WEB-INF/config.properties");
                FileReader reader = new FileReader(configFile);
                Properties props = new Properties();
                props.load(reader);
                adminName = props.getProperty("adminName");
                adminPass = props.getProperty("password");
            } catch (FileNotFoundException ex) {
                System.out.println("config.properties: file not found");
                ex.printStackTrace();
                System.exit(0);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(0);
            }

            // save admin to admin table
            Admin admin = new Admin(adminName, adminPass, true);
            ObjectifyService.ofy().save().entity(admin).now();
        }
    }

    // createCampaign creates first campaign in the database,
    // if no other campaign exist
    private void createCampaign() {
        System.out.println("#### Creating Campaign ####");

        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).first().now();
        // if campaign does not exist in the database
        if (c == null) {
            Platform iphone = ObjectifyService.ofy().load().type(Platform.class).filter("name", "iphone").first().now();
            Platform android = ObjectifyService.ofy().load().type(Platform.class).filter("name", "android").first()
                    .now();

            if (iphone == null) {
                System.out.println("iphone does not exist in platform db");
                System.exit(0);
            }
            if (android == null) {
                System.out.println("android does not exist in platform db");
                System.exit(0);
            }

            String name = "My first campaign";
            String url = "http://www.google.com";
            Boolean active = true;
            Date date = new Date();
            List<Long> p1 = new ArrayList<Long>();
            p1.add(iphone.id);
            p1.add(android.id);
            Campaign campaign = new Campaign(name, url, p1, active, date);
            ObjectifyService.ofy().save().entity(campaign).now();
        }
    }

    // create iphone, android rows in platforms table
    private void createPlatforms() {
        System.out.println("#### Add platforms to db ####");
        List<Platform> p = ObjectifyService.ofy().load().type(Platform.class).list();
        // If there is no platform in the db, add two
        if (p.size() < 1) {
            Platform p1 = new Platform("iphone");
            Platform p2 = new Platform("android");
            ObjectifyService.ofy().save().entity(p1).now();
            ObjectifyService.ofy().save().entity(p2).now();
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        // App Engine does not currently invoke this method.
    }
}
//[END all]
