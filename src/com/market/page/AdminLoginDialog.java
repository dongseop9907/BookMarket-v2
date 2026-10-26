package com.market.page;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import com.market.config.AppConfig;

public class AdminLoginDialog extends JDialog {

    private JTextField idField;
    private JPasswordField pwField;
    public boolean isLogin = false;

    public AdminLoginDialog(JFrame frame, String str) {
        super(frame, "관리자로그인", true);

        Font ft = new Font("함초롬돋움", Font.BOLD, 15);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - 400) / 2, (screenSize.height - 300) / 2);
        setSize(400, 300);
        setLayout(null);

        JPanel titlePanel = new JPanel();
        titlePanel.setBounds(0, 20, 400, 50);
        JLabel titleLabel = new JLabel("관리자 로그인");
        titleLabel.setFont(new Font("함초롬돋움", Font.BOLD, 20));
        titlePanel.add(titleLabel);
        add(titlePanel);

        JPanel idPanel = new JPanel();
        idPanel.setBounds(0, 70, 400, 50);
        JLabel idLabel = new JLabel("아 이 디 : ");
        idLabel.setFont(ft);
        idField = new JTextField(10);
        idField.setFont(ft);
        idPanel.add(idLabel);
        idPanel.add(idField);
        add(idPanel);

        JPanel pwPanel = new JPanel();
        pwPanel.setBounds(0, 120, 400, 50);
        JLabel pwLabel = new JLabel("비밀번호 : ");
        pwLabel.setFont(ft);
        pwField = new JPasswordField(10);
        pwField.setFont(ft);
        pwPanel.add(pwLabel);
        pwPanel.add(pwField);
        add(pwPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBounds(0, 170, 400, 50);
        JButton okButton = new JButton("확인");
        okButton.setFont(ft);
        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(ft);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel);

        okButton.addActionListener(e -> {
            String id = idField.getText();
            String pw = new String(pwField.getPassword());

            String adminId = AppConfig.getRequired("admin.id");
            String adminPassword = AppConfig.getRequired("admin.password");

            if (id.equals(adminId) && pw.equals(adminPassword)) {
                isLogin = true;
                dispose();
                new AdminMainMenu();
            } else {
                JOptionPane.showMessageDialog(this, "관리자 정보가 일치하지 않습니다");
            }
        });

        cancelButton.addActionListener(e -> {
            isLogin = false;
            dispose();
        });
    }

    public static void main(String[] args) {
        AdminLoginDialog login = new AdminLoginDialog(null, "Admin Login");
        login.setVisible(true);
    }
}

class AdminMainMenu extends JFrame {
    public AdminMainMenu() {
        setTitle("관리자 메뉴");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 1, 10, 10));

        JButton bookBtn = new JButton("도서정보 등록");
        JButton userBtn = new JButton("회원관리");
        JButton closeBtn = new JButton("닫기");

        add(bookBtn);
        add(userBtn);
        add(closeBtn);

        bookBtn.addActionListener(e -> {
            JFrame bookFrame = new JFrame("도서 등록");
            bookFrame.setSize(1000, 750);
            bookFrame.setLocationRelativeTo(null);
            JPanel panel = new JPanel();
            panel.setBounds(0, 0, 1000, 750);
            bookFrame.add(new AdminPage(panel));
            bookFrame.setVisible(true);
        });

        userBtn.addActionListener(e -> {
            JFrame userFrame = new JFrame("회원 목록");
            userFrame.setSize(800, 400);
            userFrame.setLocationRelativeTo(null);
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            JTable userTable = new JTable();
            JScrollPane scrollPane = new JScrollPane(userTable);
            panel.add(scrollPane, BorderLayout.CENTER);

            userFrame.add(panel);
            userFrame.setVisible(true);

            // 실제 DB에서 회원 정보 불러오기는 AdminPage 참고
        });

        closeBtn.addActionListener(e -> dispose());

        setVisible(true);
    }
}
