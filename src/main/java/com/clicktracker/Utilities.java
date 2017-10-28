package com.clicktracker;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.ArrayList;
import com.clicktracker.model.Platform;

import com.googlecode.objectify.ObjectifyService;

// Utilities class contains useful methods used in all other servlets classes
public class Utilities {
    // getURLEnding returns last part from requested url string
    public static String getURLEnding(HttpServletRequest req) throws IOException {
        // get id from request and remove first slash
        String campaignID = req.getPathInfo().substring(1);
        return campaignID;
    }

    // getCampaignID returns campaign id parsed from url, or null if that
    // is not possible
    public static Long getCampaignID(HttpServletRequest req) throws IOException {
        String urlString = getURLEnding(req);
        Long id = stringToLong(urlString);
        return id;
    }

    // turn string to Long num if that is possible, otherwise returns null
    public static Long stringToLong(String str) throws IOException {
        if (StringUtils.isNumeric(str)) {
            Long id = Long.parseLong(str);
            return id;
        }
        return null;
    }

    // getPlatforms handles parsing platform ids or names from the url query. Admin
    // is able to provide:
    // - whole id of the platform (ex: 5066549580791808,6192449487634432) or
    // - name of the platform (ex: "iphone, android")
    //
    // both inputs are checked against the database to ensure the input data is valid
    //
    // returns: list of platform ids that are representing string argument
    // ex: argument: "android", returns [2132133] // id of platform with name = android
    public static List<Long> getPlatforms(String platformsString) throws IOException {
        String[] platforms = platformsString.split(",");
        List<Long> platformIDS = new ArrayList<Long>();
        for (String platform : platforms) {
            platform = platform.trim().toLowerCase();
            // if platform is num admin provided platforms as string of platform ids
            Long id = stringToLong(platform);
            if (id != null) {
                Platform p = ObjectifyService.ofy().load().type(Platform.class).id(id).now();
                // check if platform actually exist in database
                if (p != null) {
                    platformIDS.add(p.id);
                }

                // admin used platform names in query: platforms="iphone, android";
            } else {
                Platform p = ObjectifyService.ofy().load().type(Platform.class).filter("name", platform).first().now();
                // check if platform exist in db
                if (p != null) {
                    platformIDS.add(p.id);
                }
            }
        }

        if (platformIDS.size() > 0) {
            return platformIDS;
        } else { // if platform ids are not valid (do not exist in db)
            return null;
        }
    }
}