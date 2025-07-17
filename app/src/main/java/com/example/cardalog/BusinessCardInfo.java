package com.example.cardalog;

import java.io.Serializable;

public class BusinessCardInfo implements Serializable {
    public String name;
    public String phoneNumber;
    public String email;
    public String address;
    public String businessName;
    public String jobTitle;

    public String website;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public BusinessCardInfo(String name, String jobTitle, String businessName, String phoneNumber, String email, String website, String address) {
        this.name = name;
        this.jobTitle = jobTitle;
        this.businessName = businessName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.website = website;
        this.address = address;
    }

}
