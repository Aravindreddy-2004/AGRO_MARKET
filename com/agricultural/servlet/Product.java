package com.agricultural.servlet;  // Or com.agricultural.model

public class Product {
    private final int id;
    private final int farmerId;
    private final String cropName;
    private final String quantity;
    private final String location;
    private final String phoneNumber;
    private final String price;
    private final String imagePath;

    // Constructor
    public Product(int id, int farmerId, String cropName, String quantity, String location, String phoneNumber, String price, String imagePath) {
        this.id = id;
        this.farmerId = farmerId;
        this.cropName = cropName;
        this.quantity = quantity;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.price = price;
        this.imagePath = imagePath;
    }

    // Getters (add setters if needed)
    public int getId() { return id; }
    public int getFarmerId() { return farmerId; }
    public String getCropName() { return cropName; }
    public String getQuantity() { return quantity; }
    public String getLocation() { return location; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPrice() { return price; }
    public String getImagePath() { return imagePath; }
}