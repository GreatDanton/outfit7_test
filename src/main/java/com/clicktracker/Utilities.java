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
        if (StringUtils.isNumeric(urlString)) {
            Long id = Long.parseLong(urlString);
            return id;
        }
        return null;
    }

    // turn string to Long num if that is possible, otherwise returns null
    public static Long stringToLong(String str) throws IOException {
        if (StringUtils.isNumeric(str)) {
            Long id = Long.parseLong(str);
            return id;
        }
        return null;
    }

    //
    public static List<Long> getPlatforms(String platformsString) throws IOException {
        String[] platforms = platformsString.split(",");
        List<Long> platformIDS = new ArrayList<Long>();
        for (String platform : platforms) {
            platform = platform.trim();
            Platform p = ObjectifyService.ofy().load().type(Platform.class).filter("name", platform).first().now();
            platformIDS.add(p.id);
        }
        return platformIDS;
    }
}