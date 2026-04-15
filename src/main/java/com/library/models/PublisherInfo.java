package com.library.models;

public class PublisherInfo {
    private String name;
    private String address;
    private String website;
    private String contactEmail;
    private String contactPhone;
    private int yearEstablished;

    public PublisherInfo() {
        this.name = "";
        this.address = "";
        this.website = "";
        this.contactEmail = "";
        this.contactPhone = "";
        this.yearEstablished = 0;
    }

    public PublisherInfo(String name, String address, String website, String contactEmail,
                         String contactPhone, int yearEstablished) {
        this.name = name;
        this.address = address;
        this.website = website;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.yearEstablished = yearEstablished;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public int getYearEstablished() { return yearEstablished; }
    public void setYearEstablished(int yearEstablished) { this.yearEstablished = yearEstablished; }

    @Override
    public String toString() {
        return String.format("Publisher: %s%s%s%s%sEstablished: %d",
                name.isEmpty() ? "" : name + "\n",
                address.isEmpty() ? "" : "Address: " + address + "\n",
                website.isEmpty() ? "" : "Website: " + website + "\n",
                contactEmail.isEmpty() ? "" : "Email: " + contactEmail + "\n",
                contactPhone.isEmpty() ? "" : "Phone: " + contactPhone + "\n",
                yearEstablished);
    }
}