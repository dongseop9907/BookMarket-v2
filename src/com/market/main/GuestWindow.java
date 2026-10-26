package com.market.main;

import com.market.api.ApiException;
import com.market.api.AuthApi;
import com.market.member.UserInIt;
import com.market.page.RegisterDialog;
import com.market.session.SessionContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import com.market.api.ApiClient;

public class GuestWindow extends JFrame {
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("로그인");
    private final JButton registerButton = new JButton("회원가입");
    private final JButton googleButton = new JButton("Google로 계속하기");
    private final JButton kakaoButton = new JButton("카카오로 계속하기");
    private final JButton cancelSocialButton = new JButton("소셜 로그인 취소");
    private final JLabel socialStatusLabel = new JLabel(" ");
    private SwingWorker<AuthApi.LoginResult, Void> activeSocialWorker;
    private final JLabel serverStatusLabel = new JLabel("WAS 연결 확인 중...");

    public GuestWindow(String title, int x, int y, int width, int height) {
        initContainer(title, width, height);
        checkServerStatus();
        setVisible(true);
    }

    private void initContainer(String title, int width, int height) {
        setTitle(title);
        setSize(width, height);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(new ImageIcon("./images/shop.png").getImage());

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Color.WHITE);
        setContentPane(root);

        JPanel brandPanel = new JPanel(new GridBagLayout());
        brandPanel.setBackground(new Color(30, 41, 59));
        brandPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("BOOK MARKET");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Java Swing · Tomcat · Supabase");
        subtitleLabel.setForeground(new Color(203, 213, 225));
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea description = new JTextArea(
                "기존 단일 Swing 애플리케이션을\n" +
                "Client–Server 구조로 개선한\n" +
                "온라인 도서 쇼핑몰입니다."
        );
        description.setEditable(false);
        description.setOpaque(false);
        description.setForeground(new Color(226, 232, 240));
        description.setFont(new Font("SansSerif", Font.PLAIN, 18));
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandContent.add(titleLabel);
        brandContent.add(Box.createVerticalStrut(12));
        brandContent.add(subtitleLabel);
        brandContent.add(Box.createVerticalStrut(40));
        brandContent.add(description);
        brandPanel.add(brandContent);
        root.add(brandPanel);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(248, 250, 252));
        rightPanel.setBorder(new EmptyBorder(40, 55, 40, 55));

        JPanel loginCard = new JPanel();
        loginCard.setBackground(Color.WHITE);
        loginCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(42, 42, 42, 42)
        ));
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setPreferredSize(new Dimension(390, 590));

        JLabel loginTitle = new JLabel("로그인");
        loginTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        loginTitle.setForeground(new Color(15, 23, 42));
        loginTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel loginDesc = new JLabel("BookMarket 계정으로 로그인하세요.");
        loginDesc.setFont(new Font("SansSerif", Font.PLAIN, 13));
        loginDesc.setForeground(new Color(100, 116, 139));
        loginDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        emailField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));

        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        loginButton.setFocusPainted(false);

        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        registerButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        registerButton.setFocusPainted(false);

        serverStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        serverStatusLabel.setForeground(new Color(100, 116, 139));
        serverStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginCard.add(loginTitle);
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(loginDesc);
        loginCard.add(Box.createVerticalStrut(28));
        loginCard.add(fieldLabel("이메일"));
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(emailField);
        loginCard.add(Box.createVerticalStrut(16));
        loginCard.add(fieldLabel("비밀번호"));
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(passwordField);
        loginCard.add(Box.createVerticalStrut(24));
        loginCard.add(loginButton);
        loginCard.add(Box.createVerticalStrut(10));
        loginCard.add(registerButton);
        loginCard.add(Box.createVerticalStrut(12));
        for (JButton button : new JButton[]{googleButton,kakaoButton}) {
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39));
            button.setFocusPainted(false);
            loginCard.add(button);
            loginCard.add(Box.createVerticalStrut(8));
        }
        cancelSocialButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cancelSocialButton.setEnabled(false);
        cancelSocialButton.setFocusPainted(false);
        loginCard.add(cancelSocialButton);
        socialStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        socialStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginCard.add(Box.createVerticalStrut(8));
        loginCard.add(socialStatusLabel);
        loginCard.add(Box.createVerticalStrut(10));
        loginCard.add(serverStatusLabel);

        rightPanel.add(loginCard);
        root.add(rightPanel);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> new RegisterDialog(this));
        passwordField.addActionListener(e -> login());
        googleButton.addActionListener(e -> socialLogin("google"));
        kakaoButton.addActionListener(e -> socialLogin("kakao"));
        cancelSocialButton.addActionListener(e -> cancelSocialLogin());
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(51, 65, 85));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void login() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "이메일과 비밀번호를 입력해 주세요.",
                    "로그인",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        setLoginBusy(true);
        new SwingWorker<AuthApi.LoginResult, Void>() {
            @Override
            protected AuthApi.LoginResult doInBackground() throws Exception {
                return AuthApi.login(email, password);
            }

            @Override
            protected void done() {
                setLoginBusy(false);
                try {
                    AuthApi.LoginResult user = get();
                    SessionContext.login(user);
                    UserInIt.init(user.name(), user.phone());
                    dispose();
                    new MainWindow("BookMarket", 0, 0, 1000, 750);
                } catch (Exception ex) {
                    showApiError(ex, "로그인에 실패했습니다.");
                }
            }
        }.execute();
    }

    private void socialLogin(String provider) {
        if (activeSocialWorker != null) return;
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            JOptionPane.showMessageDialog(this, "기본 브라우저를 실행할 수 없습니다.");
            return;
        }
        setLoginBusy(true);
        googleButton.setEnabled(false);
        kakaoButton.setEnabled(false);
        cancelSocialButton.setEnabled(true);
        socialStatusLabel.setForeground(new Color(71, 85, 105));
        socialStatusLabel.setText("브라우저에서 인증해 주세요 (최대 60초)");

        SwingWorker<AuthApi.LoginResult, Void> worker = new SwingWorker<>() {
            @Override protected AuthApi.LoginResult doInBackground() throws Exception {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
                if (isCancelled()) throw new InterruptedException("로그인이 취소되었습니다.");
                JSONObject start = ApiClient.post("/api/oauth/start/" + provider, new JSONObject());
                if (isCancelled()) throw new InterruptedException("로그인이 취소되었습니다.");
                Desktop.getDesktop().browse(URI.create(start.getString("authorizationUrl")));
                String ticket = start.getString("ticket");

                while (!isCancelled() && System.nanoTime() < deadline) {
                    Thread.sleep(1500);
                    if (isCancelled()) break;
                    JSONObject poll = ApiClient.post("/api/oauth/poll", new JSONObject().put("ticket", ticket));
                    if (isCancelled()) break;
                    if ("COMPLETE".equals(poll.optString("status"))) {
                        JSONObject u = poll.getJSONObject("user");
                        return new AuthApi.LoginResult(
                            u.optString("id"), u.optString("email"), u.optString("name"),
                            u.optString("phone"), u.optString("role", "USER"));
                    }
                }
                if (isCancelled()) throw new InterruptedException("로그인이 취소되었습니다.");
                throw new IllegalStateException("인증 대기 시간이 60초를 초과했습니다. 다시 시도해 주세요.");
            }

            @Override protected void done() {
                if (activeSocialWorker != this) return; // Ignore cancelled/obsolete workers.
                activeSocialWorker = null;
                setLoginBusy(false);
                googleButton.setEnabled(true);
                kakaoButton.setEnabled(true);
                cancelSocialButton.setEnabled(false);
                try {
                    AuthApi.LoginResult user = get();
                    socialStatusLabel.setText("로그인 성공");
                    SessionContext.login(user);
                    UserInIt.init(user.name(), user.phone());
                    dispose();
                    new MainWindow("BookMarket", 0, 0, 1000, 750);
                } catch (CancellationException ex) {
                    socialStatusLabel.setText("로그인이 취소되었습니다.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    socialStatusLabel.setText("로그인이 중단되었습니다.");
                } catch (ExecutionException ex) {
                    socialStatusLabel.setText("로그인 실패 · 다시 시도할 수 있습니다.");
                    showApiError(ex, "소셜 로그인에 실패했습니다.");
                } catch (Exception ex) {
                    socialStatusLabel.setText("로그인 실패 · 다시 시도할 수 있습니다.");
                    showApiError(ex, "소셜 로그인에 실패했습니다.");
                }
            }
        };
        activeSocialWorker = worker;
        worker.execute();
    }

    private void cancelSocialLogin() {
        SwingWorker<AuthApi.LoginResult, Void> worker = activeSocialWorker;
        if (worker == null) return;
        activeSocialWorker = null;
        worker.cancel(true);
        setLoginBusy(false);
        googleButton.setEnabled(true);
        kakaoButton.setEnabled(true);
        cancelSocialButton.setEnabled(false);
        socialStatusLabel.setText("로그인이 취소되었습니다. 다시 시도할 수 있습니다.");
    }

    private void checkServerStatus() {
        new SwingWorker<AuthApi.HealthResult, Void>() {
            @Override
            protected AuthApi.HealthResult doInBackground() throws Exception {
                return AuthApi.health();
            }

            @Override
            protected void done() {
                try {
                    AuthApi.HealthResult health = get();
                    if (health.isReady()) {
                        serverStatusLabel.setText("● WAS / Supabase 연결 정상");
                        serverStatusLabel.setForeground(new Color(22, 163, 74));
                    } else {
                        serverStatusLabel.setText("● WAS 응답 있음 · DB 상태: " + health.database());
                        serverStatusLabel.setForeground(new Color(217, 119, 6));
                    }
                } catch (Exception e) {
                    serverStatusLabel.setText("● Tomcat 서버 연결 안 됨");
                    serverStatusLabel.setForeground(new Color(220, 38, 38));
                }
            }
        }.execute();
    }

    private void setLoginBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
        emailField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        loginButton.setText(busy ? "로그인 중..." : "로그인");
    }

    private void showApiError(Exception exception, String fallback) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = fallback;
        if (cause instanceof ApiException apiException) {
            message = apiException.getMessage();
        } else if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            message = cause.getMessage();
        }

        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }
}
