package com.clicktracker.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class Counter {
    @Index
    @Id
    public Long id;
    // id of the campaign
    @Index
    public Long campaignID;
    // total number of clicks, we are storing total number of clicks
    // inside database, since Google Datastore does not support
    // count() for more than 1000 rows
    public Long numOfClicks;

    public Counter() {
    }

    public Counter(Long campaignID, Long numOfClicks) {
        this.campaignID = campaignID;
        this.numOfClicks = numOfClicks;
    }
}