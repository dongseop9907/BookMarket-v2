package com.market.member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.market.jdbc.DBConnection;

public class UserDAO {

    // 회원가입
    public static boolean register(String email, String password, String name, String phone, String address, String better_address) {
        String sql = "INSERT INTO user_information (email, password, name, phone, address,better_address, reg_date, purchase, total_spent) VALUES (?, ?, ?, ?, ?, ?, now(), '', 0)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.setString(3, name);
            pstmt.setString(4, phone);
            pstmt.setString(5, address);
            pstmt.setString(6, better_address);

            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 로그인
    public static boolean login(String email, String password) {
        String sql = "SELECT * FROM user_information WHERE email = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 회원정보 조회
    public static void getUserInfo(String email) {
        String sql = "SELECT * FROM user_information WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("이메일: " + rs.getString("email"));
                System.out.println("이름: " + rs.getString("name"));
                System.out.println("연락처: " + rs.getString("phone"));
                System.out.println("주소: " + rs.getString("address"));
                System.out.println("주소: " + rs.getString("address"));
                System.out.println("쓴 돈: " + rs.getInt("total_spent"));
                System.out.println("구매내역: " + rs.getString("purchase"));
                System.out.println("가입일: " + rs.getTimestamp("reg_date"));
            } else {
                System.out.println("회원 정보가 없습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
