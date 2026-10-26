package com.market.page;

import com.market.api.ApiException;
import com.market.api.AuthApi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URI;

public class RegisterDialog extends JDialog {
    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField codeField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField passwordCheckField = new JPasswordField();
    private final JTextField addressField = new JTextField();
    private final JTextField detailAddressField = new JTextField();
    private final JTextField phoneField = new JTextField();

    private final JButton sendCodeButton = new JButton("인증번호 전송");
    private final JButton verifyCodeButton = new JButton("인증 확인");
    private final JButton registerButton = new JButton("회원가입");
    private final JLabel verificationLabel = new JLabel("이메일 인증이 필요합니다.");
    private final JLabel passwordGuideLabel = new JLabel("8자 이상 · 공백 없음 · 특수문자 포함");

    private boolean emailVerified;
    private String verifiedEmail;

    public RegisterDialog(JFrame parent) {
        super(parent, "회원가입", true);
        buildUi();
        bindEvents();
        setSize(680, 700);
        setMinimumSize(new Dimension(650, 650));
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(25, 32, 25, 32));
        root.setBackground(new Color(248, 250, 252));
        setContentPane(root);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("BookMarket 회원가입");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(15, 23, 42));
        JLabel subtitle = new JLabel("이메일 인증 후 계정을 생성합니다.");
        subtitle.setForeground(new Color(100, 116, 139));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(20));
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(22, 22, 22, 22)
        ));

        styleTextField(nameField);
        styleTextField(emailField);
        styleTextField(codeField);
        styleTextField(passwordField);
        styleTextField(passwordCheckField);
        styleTextField(addressField);
        styleTextField(detailAddressField);
        styleTextField(phoneField);

        verificationLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        verificationLabel.setForeground(new Color(220, 38, 38));
        passwordGuideLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        passwordGuideLabel.setForeground(new Color(100, 116, 139));

        JButton addressSearchButton = new JButton("우편번호 검색");
        JButton cancelButton = new JButton("취소");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        addRow(form, gbc, row++, "이름", nameField);

        JPanel emailPanel = new JPanel(new BorderLayout(8, 0));
        emailPanel.setOpaque(false);
        emailPanel.add(emailField, BorderLayout.CENTER);
        emailPanel.add(sendCodeButton, BorderLayout.EAST);
        addRow(form, gbc, row++, "이메일", emailPanel);

        JPanel codePanel = new JPanel(new BorderLayout(8, 0));
        codePanel.setOpaque(false);
        codePanel.add(codeField, BorderLayout.CENTER);
        codePanel.add(verifyCodeButton, BorderLayout.EAST);
        addRow(form, gbc, row++, "인증번호", codePanel);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        form.add(verificationLabel, gbc);

        addRow(form, gbc, row++, "비밀번호", passwordField);
        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(passwordGuideLabel, gbc);

        addRow(form, gbc, row++, "비밀번호 확인", passwordCheckField);

        JPanel addressPanel = new JPanel(new BorderLayout(8, 0));
        addressPanel.setOpaque(false);
        addressPanel.add(addressField, BorderLayout.CENTER);
        addressPanel.add(addressSearchButton, BorderLayout.EAST);
        addRow(form, gbc, row++, "주소", addressPanel);

        addRow(form, gbc, row++, "상세주소", detailAddressField);
        addRow(form, gbc, row++, "전화번호", phoneField);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(registerButton);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 7, 7, 7);
        form.add(buttons, gbc);

        root.add(form, BorderLayout.CENTER);

        addressSearchButton.addActionListener(e -> openZipCodeSearch());
        cancelButton.addActionListener(e -> dispose());
    }

    private void bindEvents() {
        emailField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            if (verifiedEmail == null || !verifiedEmail.equalsIgnoreCase(emailField.getText().trim())) {
                emailVerified = false;
                verificationLabel.setText("이메일 인증이 필요합니다.");
                verificationLabel.setForeground(new Color(220, 38, 38));
            }
        }));

        passwordField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePasswordGuide));

        sendCodeButton.addActionListener(e -> sendVerificationCode());
        verifyCodeButton.addActionListener(e -> verifyCode());
        registerButton.addActionListener(e -> register());
    }

    private void sendVerificationCode() {
        String email = emailField.getText().trim();
        if (!email.contains("@")) {
            showError("올바른 이메일을 입력해 주세요.");
            return;
        }

        emailVerified = false;
        verifiedEmail = null;
        setAuthButtonsBusy(true, "전송 중...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AuthApi.sendVerificationCode(email);
                return null;
            }

            @Override
            protected void done() {
                setAuthButtonsBusy(false, "인증번호 전송");
                try {
                    get();
                    verificationLabel.setText("인증번호를 전송했습니다. 10분 안에 입력하세요.");
                    verificationLabel.setForeground(new Color(2, 132, 199));
                    codeField.requestFocusInWindow();
                } catch (Exception ex) {
                    showApiError(ex, "인증번호 전송에 실패했습니다.");
                }
            }
        }.execute();
    }

    private void verifyCode() {
        String email = emailField.getText().trim();
        String code = codeField.getText().trim();
        if (!code.matches("\\d{6}")) {
            showError("6자리 인증번호를 입력해 주세요.");
            return;
        }

        verifyCodeButton.setEnabled(false);
        verifyCodeButton.setText("확인 중...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AuthApi.verifyEmail(email, code);
                return null;
            }

            @Override
            protected void done() {
                verifyCodeButton.setEnabled(true);
                verifyCodeButton.setText("인증 확인");
                try {
                    get();
                    emailVerified = true;
                    verifiedEmail = email;
                    verificationLabel.setText("✓ 이메일 인증 완료");
                    verificationLabel.setForeground(new Color(22, 163, 74));
                } catch (Exception ex) {
                    emailVerified = false;
                    verifiedEmail = null;
                    showApiError(ex, "이메일 인증에 실패했습니다.");
                }
            }
        }.execute();
    }

    private void register() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String passwordCheck = new String(passwordCheckField.getPassword());
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String detailAddress = detailAddressField.getText().trim();

        if (name.isBlank() || email.isBlank()) {
            showError("이름과 이메일을 입력해 주세요.");
            return;
        }
        if (!emailVerified || verifiedEmail == null || !verifiedEmail.equalsIgnoreCase(email)) {
            showError("이메일 인증을 먼저 완료해 주세요.");
            return;
        }
        if (!isValidPassword(password)) {
            showError("비밀번호는 8자 이상, 공백 없이 특수문자를 포함해야 합니다.");
            return;
        }
        if (!password.equals(passwordCheck)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("가입 중...");
        new SwingWorker<AuthApi.LoginResult, Void>() {
            @Override
            protected AuthApi.LoginResult doInBackground() throws Exception {
                return AuthApi.register(email, password, name, phone, address, detailAddress);
            }

            @Override
            protected void done() {
                registerButton.setEnabled(true);
                registerButton.setText("회원가입");
                try {
                    get();
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "회원가입이 완료되었습니다.\n로그인해 주세요.",
                            "회원가입 완료",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception ex) {
                    showApiError(ex, "회원가입에 실패했습니다.");
                }
            }
        }.execute();
    }

    private void updatePasswordGuide() {
        String password = new String(passwordField.getPassword());
        if (password.isBlank()) {
            passwordGuideLabel.setText("8자 이상 · 공백 없음 · 특수문자 포함");
            passwordGuideLabel.setForeground(new Color(100, 116, 139));
        } else if (isValidPassword(password)) {
            passwordGuideLabel.setText("✓ 사용 가능한 비밀번호입니다.");
            passwordGuideLabel.setForeground(new Color(22, 163, 74));
        } else {
            passwordGuideLabel.setText("8자 이상 · 공백 없음 · 특수문자를 포함해 주세요.");
            passwordGuideLabel.setForeground(new Color(220, 38, 38));
        }
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8
                && !password.contains(" ")
                && password.matches(".*[!@#$%^&*()_+=\\-\\[\\]{}|;:'\",.<>/?`~].*");
    }

    private void setAuthButtonsBusy(boolean busy, String text) {
        sendCodeButton.setEnabled(!busy);
        verifyCodeButton.setEnabled(!busy);
        sendCodeButton.setText(text);
    }

    private void openZipCodeSearch() {
        try {
            Desktop.getDesktop().browse(new URI("https://www.epost.go.kr/search.RetrieveIntegrationNewZipCdList.comm"));
        } catch (Exception e) {
            showError("우편번호 검색 페이지를 열 수 없습니다.");
        }
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
        showError(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    private void styleTextField(JTextField field) {
        field.setPreferredSize(new Dimension(320, 38));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component component) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(new Color(51, 65, 85));

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(label, gbc);

        gbc.weightx = 1;
        gbc.gridx = 1;
        gbc.gridy = row;
        panel.add(component, gbc);
    }

    @FunctionalInterface
    private interface ChangeHandler {
        void changed();
    }

    private static class SimpleDocumentListener implements DocumentListener {
        private final ChangeHandler handler;

        private SimpleDocumentListener(ChangeHandler handler) {
            this.handler = handler;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            handler.changed();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            handler.changed();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            handler.changed();
        }
    }
}
