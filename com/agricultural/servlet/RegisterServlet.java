package com.agricultural.servlet;

import com.agricultural.util.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final String INSERT_QUERY = 
        "INSERT INTO farmer (name, email, phone, password) VALUES (?, ?, ?, ?)";

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("=== RegisterServlet LOADED: Mapped to /register ===");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        System.out.println("=== RegisterServlet: doPost HIT for registration ===");
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        request.setAttribute("name", name);

        // Server-side validation (complements HTML client-side)
        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty() ||
            phone == null || phone.trim().isEmpty() || password == null || password.length() < 6 ||
            confirmPassword == null || !password.equals(confirmPassword)) {
            String error = (!password.equals(confirmPassword)) ? 
                           "Passwords do not match" : 
                           (password.length() < 6 ? "Password must be at least 6 characters" : "All fields are required");
            String contextPath = request.getContextPath();
            response.sendRedirect(contextPath + "/register.html?error= bbbbbb" + URLEncoder.encode(error, "UTF-8"));
            return;
        }

        // TODO: Hash password (e.g., import org.mindrot.jbcrypt.BCrypt; String hashed = BCrypt.hashpw(password, BCrypt.gensalt());)
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(INSERT_QUERY);
            pstmt.setString(1, name.trim());
            pstmt.setString(2, email.trim().toLowerCase());
            pstmt.setString(3, phone.trim());
            pstmt.setString(4, password);  // Use hashed in production

            int rows = pstmt.executeUpdate();
            System.out.println("=== RegisterServlet: Inserted " + rows + " rows into DB ===");
            if (rows > 0) {
                String contextPath = request.getContextPath();
                response.sendRedirect(contextPath + "/register.html?msg=" + 
                    URLEncoder.encode("Registration successful! Please login.", "UTF-8"));
            } else {
                String contextPath = request.getContextPath();
                response.sendRedirect(contextPath + "/register.html?error=      bbbbbb" + 
                    URLEncoder.encode("Registration failed", "UTF-8"));
            }
        } catch (SQLException e) {
            String errorMsg = "Database error occurred";
            if (e.getSQLState() != null && e.getSQLState().startsWith("23") || e.getMessage().contains("Duplicate")) {
                errorMsg = "Phone or email already exists";
            }
            System.out.println("=== RegisterServlet: SQL Error - " + e.getMessage() + " ===");
            String contextPath = request.getContextPath();
            response.sendRedirect(contextPath + "/register.html?error=bbbbbb" + 
                URLEncoder.encode(errorMsg, "UTF-8"));
        } finally {
            DatabaseUtil.close(conn, pstmt, null);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        System.out.println("=== RegisterServlet: doGet HIT for /register (showing form) ===");
        response.setContentType("text/html;charset=UTF-8");

        // Try forward to separate register.html first
        try {
            request.getRequestDispatcher("/register.html").forward(request, response);
            System.out.println("=== RegisterServlet: Forward to register.html SUCCESS ===");
        } catch (ServletException | IOException e) {
            System.out.println("=== RegisterServlet: Forward FAILED (" + e.getMessage() + ") - Using embedded HTML fallback ===");
        }

    }
}