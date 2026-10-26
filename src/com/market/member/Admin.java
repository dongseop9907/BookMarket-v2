package com.market.member;

import com.market.config.AppConfig;

public class Admin extends Person {
    private final String id = AppConfig.getRequired("admin.id");
    private final String password = AppConfig.getRequired("admin.password");

    public Admin(String name, String phone) {
        super(name, phone);
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }
}
