package com.agricultural.servlet;

import com.agricultural.util.DatabaseUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

@WebServlet("/main")
@MultipartConfig(  // Enables multipart handling without external libs
    fileSizeThreshold = 1024 * 1024,  // 1MB
    maxFileSize = 1024 * 1024 * 10,   // 10MB
    maxRequestSize = 1024 * 1024 * 15 // 15MB
)
public class AgricultureServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String UPLOAD_DIR = "uploads";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        // Get error/message from query params
        String error = request.getParameter("error");
        String msg = request.getParameter("msg");
        String mode = request.getParameter("mode");  // "add" or "view" (default: view)

        HttpSession session = request.getSession(false);
        boolean isFarmerLoggedIn = (session != null && session.getAttribute("userPhone") != null);

        out.println("<html><head><title>Agriculture E-Commerce</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; background-color: #f0f8e7; color: #2e7d32; margin: 0; padding: 20px; }");
        out.println("h1, h2 { color: #1b5e20; text-align: center; }");
        out.println(".container { max-width: 1200px; margin: auto; position: relative; }");
        out.println(".form-section { background-color: #e8f5e8; padding: 20px; border-radius: 8px; margin-bottom: 20px; border: 2px solid #4caf50; max-width: 600px; margin: auto; }");
        out.println("input[type='text'], input[type='file'] { padding: 8px; margin: 5px 0; width: 100%; border: 1px solid #4caf50; border-radius: 4px; }");
        out.println("input[type='submit'], button { background-color: #4caf50; color: white; padding: 10px; border: none; border-radius: 4px; cursor: pointer; margin: 5px; }");
        out.println("input[type='submit']:hover, button:hover { background-color: #388e3c; }");
        out.println(".crops-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-top: 20px; }");
        out.println("@media (max-width: 768px) { .crops-grid { grid-template-columns: repeat(2, 1fr); } }");
        out.println("@media (max-width: 480px) { .crops-grid { grid-template-columns: 1fr; } }");
        out.println(".crop-card { background-color: #ffffff; border: 2px solid #4caf50; border-radius: 8px; padding: 15px; text-align: center; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        out.println(".crop-card img { max-width: 100%; height: auto; border-radius: 4px; }");
        out.println(".crop-card button { background-color: #4caf50; color: white; padding: 8px 12px; border: none; border-radius: 4px; cursor: pointer; margin: 5px; }");
        out.println(".crop-card button:hover { background-color: #388e3c; }");
        out.println(".delete-btn { background-color: #f44336; }");
        out.println(".delete-btn:hover { background-color: #d32f2f; }");
        out.println(".add-btn { position: absolute; top: 20px; right: 20px; background-color: #4caf50; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; }");
        out.println(".add-btn:hover { background-color: #388e3c; }");
        out.println(".error { color: #d32f2f; text-align: center; }");
        out.println(".msg { color: #1b5e20; text-align: center; }");
        out.println(".mode-buttons { text-align: center; margin: 10px 0; }");
        out.println("</style>");
        out.println("</head><body>");
        out.println("<div class='container'>");
        out.println("<h1>Agriculture Marketplace</h1>");

        if (error != null) out.println("<p class='error'>" + error + "</p>");
        if (msg != null) out.println("<p class='msg'>" + msg + "</p>");

        if (isFarmerLoggedIn) {
            if ("add".equals(mode)) {
                // Add Mode: Show only the form
                out.println("<div class='form-section'>");
                out.println("<h2>Add Your Crop</h2>");
                out.println("<form action='main?mode=add' method='post' enctype='multipart/form-data'>");
                out.println("Crop Name: <input type='text' name='cropName' required><br>");
                out.println("Quantity: <input type='text' name='quantity' required><br>");
                out.println("Location: <input type='text' name='location' required><br>");
                out.println("Phone Number: <input type='text' name='phoneNumber' required><br>");
                out.println("Price: <input type='text' name='price' required><br>");
                out.println("Image: <input type='file' name='image' accept='image/*' required><br>");
                out.println("<div class='mode-buttons'>");
                out.println("<input type='submit' name='action' value='Add'>");
                out.println("<input type='submit' name='action' value='Add Another'>");
                out.println("<button type='button' onclick=\"window.location.href='main?mode=view'\">View Products</button>");
                out.println("</div>");
                out.println("</form>");
                out.println("<p><a href='index.html'>Logout</a></p>");
                out.println("</div>");
            } else {
                // View Mode (default): Show grid and add button
                out.println("<button class='add-btn' onclick=\"window.location.href='main?mode=add'\">Add Crop</button>");
                out.println("<h2>Available Crops</h2>");
                out.println("<div class='crops-grid'>");
                try (Connection conn = DatabaseUtil.getConnection()) {
                    String sql = "SELECT id, farmer_id, crop_name, quantity, location, phone_number,image_path,price FROM products";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery();
                    boolean hasProducts = false;
                    while (rs.next()) {
                        hasProducts = true;
                        int productId = rs.getInt("id");
                        int productFarmerId = rs.getInt("farmer_id");
                        boolean canDelete = productFarmerId == getFarmerIdByPhone(session.getAttribute("userPhone").toString(), conn);

                        out.println("<div class='crop-card'>");
                        out.println("<img src='" + rs.getString("image_path") + "' alt='Crop Image'><br>");
                        out.println("<strong>Crop:</strong> " + rs.getString("crop_name") + "<br>");
                        out.println("<strong>Quantity:</strong> " + rs.getString("quantity") + "<br>");
                        out.println("<strong>Location:</strong> " + rs.getString("location") + "<br>");
                        out.println("<strong>Phone:</strong> " + rs.getString("phone_number") + "<br>");
                        out.println("<strong>Price:</strong> " + rs.getString("price") + "<br>");
                        out.println("<button onclick=\"alert('Purchase initiated! Contact: " + rs.getString("phone_number") + "');\">Buy Now</button>");
                        if (canDelete) {
                            out.println("<form action='main' method='post' style='display:inline;'>");
                            out.println("<input type='hidden' name='action' value='delete'>");
                            out.println("<input type='hidden' name='productId' value='" + productId + "'>");
                            out.println("<button type='submit' class='delete-btn' onclick=\"return confirm('Are you sure you want to delete this crop?');\">Delete</button>");
                            out.println("</form>");
                        }
                        out.println("</div>");
                    }
                    if (!hasProducts) out.println("<p>No products available yet.</p>");
                } catch (SQLException e) {
                    out.println("<p class='error'>Error loading products: " + e.getMessage() + "</p>");
                }
                out.println("</div>");  // End crops-grid
                out.println("<p><a href='index.html'>Logout</a></p>");
            }
        } else {
            // Not logged in: Show view mode with login prompt
            out.println("<p><a href='login.html'>Login as Farmer</a> to add products.</p>");
            out.println("<h2>Available Crops</h2>");
            out.println("<div class='crops-grid'>");
            try (Connection conn = DatabaseUtil.getConnection()) {
                String sql = "SELECT crop_name, quantity, location, phone_number, image_path,price FROM products";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                boolean hasProducts = false;
                while (rs.next()) {
                    hasProducts = true;
                    out.println("<div class='crop-card'>");
                    out.println("<img src='" + rs.getString("image_path") + "' alt='Crop Image'><br>");
                    out.println("<strong>Crop:</strong> " + rs.getString("crop_name") + "<br>");
                    out.println("<strong>Quantity:</strong> " + rs.getString("quantity") + "<br>");
                    out.println("<strong>Location:</strong> " + rs.getString("location") + "<br>");
                    out.println("<strong>Phone:</strong> " + rs.getString("phone_number") + "<br>");
                    out.println("<strong>Price:</strong> " + rs.getString("price") + "<br>");
                    out.println("<button onclick=\"alert('Purchase initiated! Contact: " + rs.getString("phone_number") + "');\">Buy Now</button>");
                    out.println("</div>");
                }
                if (!hasProducts) out.println("<p>No products available yet.</p>");
            } catch (SQLException e) {
                out.println("<p class='error'>Error loading products: " + e.getMessage() + "</p>");
            }
            out.println("</div>");  // End crops-grid
        }
        out.println("</div>");  // End container
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userPhone") == null) {
            response.sendRedirect("login.html");
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            handleDelete(request, response, session);
            return;
        }

        String userPhone = (String) session.getAttribute("userPhone");

        // Fetch farmer_id
        int farmerId = -1;
        try (Connection conn = DatabaseUtil.getConnection()) {
            farmerId = getFarmerIdByPhone(userPhone, conn);
            if (farmerId == -1) {
                response.sendRedirect("main?error=Farmer not found");
                return;
            }
        } catch (SQLException e) {
            response.sendRedirect("main?error=Database error");
            return;
        }

        // Handle multipart parts for add product
        String cropName = null, quantity = null, location = null, phoneNumber = null, price=null, imagePath = null;
        Collection<Part> parts = request.getParts();
        for (Part part : parts) {
            String fieldName = part.getName();
            if (part.getContentType() == null) {  // Text field
                String value = new BufferedReader(new InputStreamReader(part.getInputStream())).readLine();
                switch (fieldName) {
                    case "cropName" -> cropName = value;
                    case "quantity" -> quantity = value;
                    case "location" -> location = value;
                    case "phoneNumber" -> phoneNumber = value;
                    case "price" -> price = value;
                }
            } else {  // File
                String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
                String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIR;
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) uploadDir.mkdir();

                Path filePath = Paths.get(uploadPath, fileName);
                Files.copy(part.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                imagePath = UPLOAD_DIR + "/" + fileName;
            }
        }

        // Save to DB and redirect based on action
        if (cropName != null && quantity != null && location != null && phoneNumber != null && imagePath != null && price != null ) {
            try (Connection conn = DatabaseUtil.getConnection()) {
                String sql = "INSERT INTO products (farmer_id, crop_name, quantity, location, phone_number, image_path,price) VALUES (?, ?, ?, ?, ?, ?,?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, farmerId);
                stmt.setString(2, cropName);
                stmt.setString(3, quantity);
                stmt.setString(4, location);
                stmt.setString(5, phoneNumber);
                
                stmt.setString(6, imagePath);
                stmt.setString(7,price);
                stmt.executeUpdate();
                if ("Add Another".equals(action)) {
                    response.sendRedirect("main?mode=add&msg=Product added! Add another.");
                } else {
                    response.sendRedirect("main?mode=view&msg=Product added successfully");
                }
            } catch (SQLException e) {
                response.sendRedirect("main?mode=add&error=Failed to add product");
            }
        } else {
            response.sendRedirect("main?mode=add&error=All fields are required");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        String productIdStr = request.getParameter("productId");
        if (productIdStr == null) {
            response.sendRedirect("main?error=Invalid delete request");
            return;
        }

        int productId = Integer.parseInt(productIdStr);
        String userPhone = (String) session.getAttribute("userPhone");

        try (Connection conn = DatabaseUtil.getConnection()) {
            int farmerId = getFarmerIdByPhone(userPhone, conn);
            if (farmerId == -1) {
                response.sendRedirect("main?error=Farmer not found");
                return;
            }

            // Check ownership and delete
            String checkSql = "SELECT farmer_id FROM products WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, productId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt("farmer_id") == farmerId) {
                String deleteSql = "DELETE FROM products WHERE id = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                deleteStmt.setInt(1, productId);
                deleteStmt.executeUpdate();
                response.sendRedirect("main?mode=view&msg=Product deleted successfully");
            } else {
                response.sendRedirect("main?mode=view&error=You can only delete your own products");
            }
        } catch (SQLException e) {
            response.sendRedirect("main?mode=view&error=Delete failed");
        }
    }

    private int getFarmerIdByPhone(String phone, Connection conn) throws SQLException {
        String sql = "SELECT id FROM farmer WHERE phone = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, phone);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt("id") : -1;
    }
}
