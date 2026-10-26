package com.market.page;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MockPaymentDialog extends JDialog {

    public MockPaymentDialog(JFrame parent) {
        super(parent, "모의 결제창", true);
        setLayout(new GridLayout(5, 2, 10, 10));
        setSize(400, 250);
        setLocationRelativeTo(parent);

        JTextField cardNumberField = new JTextField();
        JTextField expiryField = new JTextField();
        JTextField cvcField = new JTextField();
        JPasswordField pinField = new JPasswordField();

        // 카드번호: 16자리 숫자 제한, 4자리마다 공백 삽입
        ((AbstractDocument) cardNumberField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null) return;

                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String cleaned = (current.substring(0, offset) + text + current.substring(offset + length))
                        .replaceAll("\\D", "");

                if (cleaned.length() > 16) return;

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cleaned.length(); i++) {
                    if (i > 0 && i % 4 == 0) sb.append(" ");
                    sb.append(cleaned.charAt(i));
                }

                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, sb.toString(), attrs);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String cleaned = (current.substring(0, offset) + current.substring(offset + length))
                        .replaceAll("\\D", "");

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cleaned.length(); i++) {
                    if (i > 0 && i % 4 == 0) sb.append(" ");
                    sb.append(cleaned.charAt(i));
                }

                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, sb.toString(), null);
            }
        });

        // 유효기간 MM/YY 자동 입력
        expiryField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String text = expiryField.getText().replaceAll("\\D", "");
                if (text.length() > 4) text = text.substring(0, 4);
                if (text.length() > 2) {
                    text = text.substring(0, 2) + "/" + text.substring(2);
                }
                expiryField.setText(text);
            }
        });

        // CVC: 숫자 3자리
        ((AbstractDocument) cvcField.getDocument()).setDocumentFilter(new DocumentFilter() {
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("\\d+") && fb.getDocument().getLength() + string.length() <= 3) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("\\d*") && fb.getDocument().getLength() - length + text.length() <= 3) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        // 카드 비밀번호: 숫자 4자리, 마스킹 처리
        ((AbstractDocument) pinField.getDocument()).setDocumentFilter(new DocumentFilter() {
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("\\d+") && fb.getDocument().getLength() + string.length() <= 4) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("\\d*") && fb.getDocument().getLength() - length + text.length() <= 4) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        // 결제 버튼
        JButton payBtn = new JButton("결제하기");
        JButton cancelBtn = new JButton("취소");

        payBtn.addActionListener(e -> {
            String pin = new String(pinField.getPassword());
            String encrypted = encryptSHA256(pin);
            JOptionPane.showMessageDialog(this, "✅ 결제 완료 ");
            dispose();
        });

        cancelBtn.addActionListener(e -> dispose());

        // UI 배치
        add(new JLabel("카드 번호:"));
        add(cardNumberField);
        add(new JLabel("유효기간 (MM/YY):"));
        add(expiryField);
        add(new JLabel("CVC:"));
        add(cvcField);
        add(new JLabel("비밀번호 (4자리):"));
        add(pinField);
        add(payBtn);
        add(cancelBtn);
    }

    // SHA-256 암호화 함수
    private String encryptSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] result = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16) + "..."; // 보기 좋게 앞 16자리만
        } catch (NoSuchAlgorithmException e) {
            return "암호화 오류";
        }
    }
}
