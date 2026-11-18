
package com.example.messloc.data.model.admin;

public class CreateAdminRequest {

    private String name;
    private String email;
    private String password;
    private String phone;

    public CreateAdminRequest(
            String name,
            String email,
            String password,
            String phone
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }
}
