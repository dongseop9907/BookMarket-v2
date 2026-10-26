package com.market.page;

import com.market.api.OrderApi;
import com.market.cart.Cart;
import com.market.cart.CartItem;
import com.market.member.UserInIt;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class CartShippingPage extends JPanel {
    private static final Color BACKGROUND = new Color(247, 247, 253);
    private static final Color INK = new Color(27, 35, 68);
    private static final Color PURPLE = new Color(124, 92, 255);
    private final Cart cart;
    private final JPanel contentHost;
    private final JPanel form = new JPanel(new GridBagLayout());
    private final JTextField nameField = new JTextField(25);
    private final JTextField phoneField = new JTextField(25);
    private final JTextField addressField = new JTextField(25);
    private final JRadioButton useMember = new JRadioButton("회원 정보 사용", true);
    private final JRadioButton useOther = new JRadioButton("다른 수령인", false);
    private final JButton order = new JButton("주문 접수하기");
    private final NumberFormat won = NumberFormat.getIntegerInstance(Locale.KOREA);

    public CartShippingPage(JPanel parent, Cart cart) {
        this.cart = cart;
        this.contentHost = parent;
        setLayout(new BorderLayout());
        setBackground(new Color(247, 247, 253));
        JPanel canvas = new JPanel(new BorderLayout(0, 11));
        canvas.setPreferredSize(new Dimension(640, 440));
        canvas.setBackground(BACKGROUND);
        canvas.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("배송지 입력 및 주문 확인");
        title.setFont(new Font("Dialog", Font.BOLD, 23));
        title.setForeground(INK);
        heading.add(title, BorderLayout.WEST);
        JLabel step = new JLabel("주문 · 결제 전");
        step.setForeground(new Color(106, 108, 136));
        heading.add(step, BorderLayout.EAST);
        canvas.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 9));
        center.setOpaque(false);
        JPanel info = new JPanel(new BorderLayout(0, 9));
        info.setBorder(new EmptyBorder(8, 12, 8, 12));
        info.setBackground(Color.WHITE);

        JPanel radioBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        radioBar.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        group.add(useMember);
        group.add(useOther);
        useMember.setOpaque(false);
        useOther.setOpaque(false);
        radioBar.add(useMember);
        radioBar.add(useOther);
        info.add(radioBar, BorderLayout.NORTH);

        form.setOpaque(false);
        addField("수령인", nameField, 0);
        addField("연락처", phoneField, 1);
        addField("배송 주소", addressField, 2);
        info.add(form, BorderLayout.CENTER);
        center.add(info, BorderLayout.NORTH);

        JPanel summary = new JPanel(new BorderLayout(0, 6));
        summary.setBackground(Color.WHITE);
        summary.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel heading2 = new JLabel("주문 상품 요약");
        heading2.setForeground(INK);
        heading2.setFont(new Font("Dialog", Font.BOLD, 15));
        summary.add(heading2, BorderLayout.NORTH);
        int pieces = 0;
        long total = 0;
        for (CartItem item : cart.mCartItem) {
            pieces += item.getQuantity();
            total += (long) item.getTotalPrice();
        }
        JLabel totalLabel = new JLabel("상품 " + pieces + "권     총 " + won.format(total) + "원");
        totalLabel.setFont(new Font("Dialog", Font.BOLD, 17));
        totalLabel.setForeground(INK);
        summary.add(totalLabel, BorderLayout.CENTER);
        JLabel pending = new JLabel("※ 실제 결제는 진행되지 않으며 주문은 PENDING(결제 전)으로 접수됩니다.");
        pending.setForeground(new Color(113, 113, 130));
        summary.add(pending, BorderLayout.SOUTH);
        center.add(summary, BorderLayout.CENTER);
        canvas.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        footer.setOpaque(false);
        order.setBackground(PURPLE);
        order.setForeground(Color.WHITE);
        order.setOpaque(true);
        order.setFocusPainted(false);
        order.setPreferredSize(new Dimension(170, 36));
        footer.add(order);
        canvas.add(footer, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 0, 0);
        wrap.add(canvas, c);
        add(wrap, BorderLayout.CENTER);

        populateMember();
        useMember.addActionListener(e -> populateMember());
        useOther.addActionListener(e -> { nameField.setText(""); phoneField.setText(""); });
        order.addActionListener(e -> placeOrder());
    }

    private void addField(String caption, JTextField field, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(7, 4, 7, 12);
        JLabel label = new JLabel(caption);
        label.setForeground(INK);
        form.add(label, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        field.setPreferredSize(new Dimension(365, 31));
        form.add(field, c);
    }

    private void populateMember() {
        try {
            if (UserInIt.getmUser() != null) {
                nameField.setText(safe(UserInIt.getmUser().getName()));
                phoneField.setText(String.valueOf(UserInIt.getmUser().getPhone()));
            }
        } catch (Exception ignored) {
            // Social accounts may not have profile/phone info.
        }
    }

    private String safe(String text) { return text == null ? "" : text; }

    private void placeOrder() {
        String address = addressField.getText().trim();
        if (cart.mCartItem.isEmpty()) {
            JOptionPane.showMessageDialog(this, "장바구니가 비었습니다.");
            return;
        }
        if (nameField.getText().trim().isEmpty() || phoneField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "수령인과 연락처를 입력하세요.");
            return;
        }
        String receiver = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        if (receiver.length() < 2 || receiver.length() > 60) {
            JOptionPane.showMessageDialog(this, "수령인 이름을 2~60자로 입력하세요.");
            nameField.requestFocusInWindow();
            return;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (!digits.matches("0[0-9]{8,10}")) {
            JOptionPane.showMessageDialog(this, "연락처를 정확히 입력하세요. 예: 010-1234-5678");
            phoneField.requestFocusInWindow();
            return;
        }
        if (address.length() < 8 || address.length() > 500 || !address.matches(".*[^0-9\\s].*")) {
            JOptionPane.showMessageDialog(this, "배송 주소를 8~500자로 입력하세요. 상세 주소를 포함해 주세요.");
            addressField.requestFocusInWindow();
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "주문을 접수하시겠습니까? 실제 결제는 진행되지 않습니다.",
                "주문 확인", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        order.setEnabled(false);
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception { return OrderApi.place(cart, address); }
            @Override protected void done() {
                order.setEnabled(true);
                try {
                    JSONObject result = get();
                    String id = result.getString("orderId");
                    int price = result.getInt("totalPrice");
                    cart.deleteBook();
                    showCompletion(id, price);
                } catch (Exception failure) {
                    Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
                    JOptionPane.showMessageDialog(CartShippingPage.this,
                            "주문 실패: " + cause.getMessage() + "\n장바구니는 유지됩니다.",
                            "주문 실패", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    private void showCompletion(String id, int price) {
        removeAll();
        setLayout(new GridBagLayout());
        setBackground(new Color(247, 247, 253));
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setPreferredSize(new Dimension(630, 375));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("주문 접수 완료");
        title.setFont(new Font("Dialog", Font.BOLD, 25));
        title.setForeground(INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        JLabel subtitle = new JLabel("결제는 진행되지 않았습니다. 주문 내역에서 상태를 확인하세요.");
        subtitle.setForeground(new Color(108, 110, 132));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(3, 2, 10, 14));
        details.setBackground(Color.WHITE);
        details.setBorder(new EmptyBorder(20, 18, 20, 18));
        details.add(detailLabel("주문번호"));
        JTextField number = new JTextField(id);
        number.setEditable(false);
        number.setBorder(null);
        number.setBackground(Color.WHITE);
        number.setForeground(INK);
        details.add(number);
        details.add(detailLabel("주문 금액"));
        JLabel amount = new JLabel(won.format(price) + "원");
        amount.setFont(new Font("Dialog", Font.BOLD, 17));
        amount.setForeground(INK);
        details.add(amount);
        details.add(detailLabel("주문 상태"));
        JLabel state = new JLabel("PENDING · 결제 전");
        state.setForeground(new Color(124, 92, 255));
        state.setFont(new Font("Dialog", Font.BOLD, 14));
        details.add(state);
        panel.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton copy = new JButton("주문번호 복사");
        copy.addActionListener(e -> {
            java.awt.datatransfer.StringSelection selection =
                new java.awt.datatransfer.StringSelection(id);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(this, "주문번호를 복사했습니다.");
        });
        JButton history = new JButton("주문 내역 확인");
        history.setBackground(PURPLE);
        history.setForeground(Color.WHITE);
        history.setFocusPainted(false);
        history.addActionListener(e -> {
            contentHost.removeAll();
            contentHost.add("OrderHistory", new OrderHistoryPage());
            contentHost.revalidate();
            contentHost.repaint();
        });
        actions.add(copy);
        actions.add(history);
        panel.add(actions, BorderLayout.SOUTH);
        add(panel);
        revalidate();
        repaint();
    }

    private JLabel detailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Dialog", Font.BOLD, 13));
        label.setForeground(new Color(108, 110, 132));
        return label;
    }

}
