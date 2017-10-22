package com.clicktracker.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Cache;

// custom imports
import java.util.Date;

// Create table for storing campaigns
@Entity
@Cache
public class Campaign {
    @Index
    @Id
    public Long id;

    public String name;
    public String redirectURL;
    // add platforms_id as a string or
    // just add one row for each id

    // an easy way to disable campaign links without deleting all content
    // from the database
    public Boolean active;
    public Date createdAt;

    public Campaign() {
    }

    public Campaign(String name, String redirectURL, Boolean active, Date createdAt) {
        this.name = name;
        this.redirectURL = redirectURL;
        this.active = active;
        this.createdAt = createdAt;
    }
}