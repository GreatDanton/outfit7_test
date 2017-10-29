package com.clicktracker.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import org.mindrot.jbcrypt.BCrypt;

// Create table for admin
@Entity
public class Admin {
    @Index
    @Id
    public Long id;

    // name should be unique but that is not possible in GAE?
    @Index
    public String name;
    public String password; // should be hashed
    public Boolean active; // easy way to disable access

    // for some reason we need no arg constructor??
    public Admin() {
    }

    public Admin(String name, String password, Boolean active) {
        this.name = name;
        // hash password
        this.password = BCrypt.hashpw(password, BCrypt.gensalt());
        this.active = active;
    }
}