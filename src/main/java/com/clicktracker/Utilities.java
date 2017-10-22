package com.clicktracker;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

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
}