package com.market.main;

import javax.swing.*;

public class Welcome {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new GuestWindow("BookMarket 로그인", 0, 0, 1000, 750);
        });
    }
}
