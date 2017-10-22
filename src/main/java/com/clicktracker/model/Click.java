package com.clicktracker.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

// custom imports
import java.util.Date;

// Create entity for storing client clicks
@Entity
public class Click {
    @Id
    public Long id;
    // id of the campaign
    public Long campaignID;
    // storing ip for each request in case we are interested in statistics later
    // ex. how many unique visitors we had this month
    public String ip;
    // we might be interested in exact browser version for each click
    // I believe the assignment expects just to log platform id depending on
    // the user-agent ??
    public String userAgent;
    // when the click happened
    public Date createdAt;

    public Click() {
    }

    public Click(Long campaignID, String ip, String userAgent, Date createdAt) {
        this.campaignID = campaignID;
        this.ip = ip;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }
}