package com.clicktracker.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

// Create table for admin
@Entity
public class Admin {
    @Index
    @Id
    public Long id;

    // name should be unique but that is not possible in GAE?
    public String name;
    public String password; // should be hashed
    public Boolean active; // easy way to disable access

    // for some reason we need no arg constructor??
    public Admin() {
    }

    // Admin storing data?
    public Admin(String name, String password, Boolean active) {
        this.name = name;
        this.password = password;
        this.active = active;
    }
}