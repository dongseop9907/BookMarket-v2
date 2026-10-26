package com.market.server.dao;

import com.market.server.config.DatabaseConfig;
import com.market.server.model.AuthUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class AuthDao {

    public boolean emailExists(String email) throws SQLException {
        String sql = "select 1 from public.users where lower(email) = lower(?) limit 1";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<AuthUser> findByEmail(String email) throws SQLException {
        String sql = """
                select id, email, password_hash, name, phone, role
                from public.users
                where lower(email) = lower(?)
                limit 1
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AuthUser(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("role")
                ));
            }
        }
    }

    public AuthUser createUser(String email,
                               String passwordHash,
                               String name,
                               String phone,
                               String address,
                               String detailAddress) throws SQLException {
        String insertSql = """
                insert into public.users
                    (email, password_hash, name, phone, address, detail_address, role)
                values (?, ?, ?, ?, ?, ?, 'USER')
                returning id, email, password_hash, name, phone, role
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!isEmailVerified(connection, email)) {
                    throw new IllegalStateException("EMAIL_NOT_VERIFIED");
                }

                AuthUser user;
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setString(1, email);
                    ps.setString(2, passwordHash);
                    ps.setString(3, name);
                    ps.setString(4, nullIfBlank(phone));
                    ps.setString(5, nullIfBlank(address));
                    ps.setString(6, nullIfBlank(detailAddress));

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        user = new AuthUser(
                                rs.getObject("id", UUID.class),
                                rs.getString("email"),
                                rs.getString("password_hash"),
                                rs.getString("name"),
                                rs.getString("phone"),
                                rs.getString("role")
                        );
                    }
                }

                try (PreparedStatement cleanup = connection.prepareStatement(
                        "delete from public.email_verifications where lower(email) = lower(?)")) {
                    cleanup.setString(1, email);
                    cleanup.executeUpdate();
                }

                connection.commit();
                return user;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void saveVerificationCode(String email, String code, Instant expiresAt) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement(
                    "delete from public.email_verifications where lower(email) = lower(?)")) {
                delete.setString(1, email);
                delete.executeUpdate();

                try (PreparedStatement insert = connection.prepareStatement("""
                        insert into public.email_verifications
                            (email, verification_code, expires_at, verified)
                        values (?, ?, ?, false)
                        """)) {
                    insert.setString(1, email);
                    insert.setString(2, code);
                    insert.setTimestamp(3, Timestamp.from(expiresAt));
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean verifyEmailCode(String email, String code) throws SQLException {
        String sql = """
                update public.email_verifications
                set verified = true
                where lower(email) = lower(?)
                  and verification_code = ?
                  and expires_at > now()
                  and verified = false
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            return ps.executeUpdate() == 1;
        }
    }

    private boolean isEmailVerified(Connection connection, String email) throws SQLException {
        String sql = """
                select 1
                from public.email_verifications
                where lower(email) = lower(?)
                  and verified = true
                  and expires_at > now()
                order by created_at desc
                limit 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
