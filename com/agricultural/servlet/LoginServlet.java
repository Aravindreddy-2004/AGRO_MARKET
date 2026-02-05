package com.agricultural.servlet;

import com.agricultural.util.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final String CHECK_USER_QUERY = "SELECT password FROM farmer WHERE phone = ?";
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        

        if (null == action) {
            response.sendRedirect("login.html?error=Invalid action");
        } else switch (action) {
            case "sendOtp" -> handleSendOtp(request, response);
            case "verifyOtp" -> handleVerifyOtp(request, response);
            case "passwordLogin" -> handlePasswordLogin(request, response);
            default -> response.sendRedirect("login.html?error=Invalid action");
        }
    }

    private void handleSendOtp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String phone = request.getParameter("phone");
        
        if (phone == null || phone.trim().isEmpty() || phone.length() < 10) { // Basic phone validation
            response.sendRedirect("login.html?error=Valid phone number is required (at least 10 digits)");
            return;
        }

        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        HttpSession session = request.getSession();
        session.setAttribute("otp", otp);
        session.setAttribute("otpPhone", phone);
        session.setAttribute("otpTime", System.currentTimeMillis());

        // Simulate SMS (log to console/server logs for debugging)
        System.out.println("OTP " + otp + " generated for phone: " + phone + " (Sent via SMS in production)");

        // Redirect with OTP value for testing popup (remove otpValue in production!)
        String msg = "OTP sent to " + phone + "!";
        response.sendRedirect("login.html?msg=" + java.net.URLEncoder.encode(msg, "UTF-8") + "&otpValue=" + otp);
    }

    private void handleVerifyOtp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String enteredOtp = request.getParameter("otp");
        String name=request.getParameter("name");
        if (enteredOtp == null || enteredOtp.trim().isEmpty()) {
            response.sendRedirect("login.html?error=OTP is required");
            return;
        }

        HttpSession session = request.getSession();
        Integer storedOtp = (Integer) session.getAttribute("otp");
        String storedPhone = (String) session.getAttribute("otpPhone");
        Long otpTime = (Long) session.getAttribute("otpTime");

        if (storedOtp == null || storedPhone == null || otpTime == null || 
            (System.currentTimeMillis() - otpTime) > (OTP_EXPIRY_MINUTES * 60 * 1000L)) {
            // Clear expired session
            session.removeAttribute("otp");
            session.removeAttribute("otpPhone");
            session.removeAttribute("otpTime");
            response.sendRedirect("login.html?error=OTP expired or not generated. Request a new one.");
            return;
        }

        if (enteredOtp.equals(storedOtp.toString())) {
            // Success: Clear OTP session, set user session
            session.removeAttribute("otp");
            session.removeAttribute("otpPhone");
            session.removeAttribute("otpTime");
            session.setAttribute("userPhone", storedPhone);
            String msg = "OTP verified successfully! Logged in as " + storedPhone;
            response.sendRedirect("main?msg=" + java.net.URLEncoder.encode(msg, "UTF-8") + "&type=otp");
        } else {
            response.sendRedirect("login.html?error=Invalid OTP. Try again.");
        }
    }

    private void handlePasswordLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String phone = request.getParameter("phone");
        String name=request.getParameter("name");
        String password = request.getParameter("password");

        if (phone == null || password == null || phone.trim().isEmpty() || password.trim().isEmpty()) {
            response.sendRedirect("login.html?error=Phone and password are required");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(CHECK_USER_QUERY);
            pstmt.setString(1, phone);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                if (password.equals(dbPassword)) { // Hash and compare in production
                    HttpSession session = request.getSession();
                    session.setAttribute("userPhone", phone);
                    String msg = "Password login successful! Welcome " + phone;
                    response.sendRedirect("main?msg=" + java.net.URLEncoder.encode(msg, "UTF-8") + "&type=password");
                } else {
                    response.sendRedirect("login.html?error=Invalid password");
                }
            } else {
                response.sendRedirect("login.html?error=Phone number not registered. Please register first.");
            }
        } catch (SQLException e) {
            response.sendRedirect("login.html?error=Login failed due to database error");
        } finally {
            DatabaseUtil.close(conn, pstmt, rs);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.getRequestDispatcher("/login.html").forward(request, response);
    }
}