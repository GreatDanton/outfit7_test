package com.clicktracker.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Cache;

// custom imports
import java.util.Date;
import java.util.List;

// Create table for storing campaigns
@Entity
@Cache
public class Campaign {
    @Index
    @Id
    public Long id;

    public String name;
    public String redirectURL;

    public List<Long> platforms;
    // active field => an easy way to disable campaign links without deleting
    // campaign from the database
    @Index
    public Boolean active;
    public Date createdAt;

    public Campaign() {
    }

    public Campaign(String name, String redirectURL, List<Long> platforms, Boolean active, Date createdAt) {
        this.name = name;
        this.redirectURL = redirectURL;
        this.platforms = platforms;
        this.active = active;
        this.createdAt = createdAt;
    }
}