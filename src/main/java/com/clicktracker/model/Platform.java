package com.clicktracker.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

// create entity for storing campaign platforms
@Entity
public class Platform {
    @Index
    @Id
    public Long id;

    @Index
    public String name; // platform name

    public Platform() {
    }

    public Platform(String name) {
        this.name = name;
    }
}