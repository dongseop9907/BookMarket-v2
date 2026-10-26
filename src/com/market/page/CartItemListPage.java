package com.market.page;

import com.market.cart.Cart;
import com.market.cart.CartItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/** Modern cart presentation; reuses the existing in-memory cart and checkout flow. */
public class CartItemListPage extends JPanel {
    private static final Color BG = new Color(247, 247, 253);
    private static final Color PURPLE = new Color(124, 92, 255);
    private static final Color INK = new Color(28, 33, 58);
    private static final NumberFormat KRW = NumberFormat.getNumberInstance(Locale.KOREA);
    private static final String[] HEADERS = {"도서 ISBN", "도서명", "단가", "수량", "합계"};

    public static int mSelectRow = -1;
    private final Cart cart;
    private final DefaultTableModel model = new DefaultTableModel(HEADERS, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel total = new JLabel();
    private final JLabel counter = new JLabel();
    private final JButton remove = new JButton("선택 도서 삭제");
    private final JButton clear = new JButton("전체 비우기");

    public CartItemListPage(JPanel panel, Cart cart) {
        this.cart = cart;
        setLayout(new BorderLayout(0, 6));
        setPreferredSize(new Dimension(670, 370));
        setMaximumSize(new Dimension(670, 370));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("내 장바구니");
        title.setFont(new Font("Dialog", Font.BOLD, 24));
        title.setForeground(INK);
        heading.add(title, BorderLayout.WEST);
        counter.setForeground(new Color(117, 117, 138));
        heading.add(counter, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);

        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(239, 236, 255));
        table.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));
        table.getColumnModel().getColumn(1).setPreferredWidth(270);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(85);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mSelectRow = table.getSelectedRow();
                remove.setEnabled(mSelectRow >= 0);
            }
        });
        JScrollPane listScroll = new JScrollPane(table);
        listScroll.setPreferredSize(new Dimension(640, 190));
        listScroll.setMinimumSize(new Dimension(400, 120));
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        listScroll.setBorder(BorderFactory.createLineBorder(new Color(227, 228, 240)));
        add(listScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 7));
        footer.setOpaque(false);
        total.setFont(new Font("Dialog", Font.BOLD, 17));
        total.setForeground(INK);
        footer.add(total, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        remove.setEnabled(false);
        remove.addActionListener(e -> removeSelected());
        clear.addActionListener(e -> clearCart());
        JButton refresh = new JButton("새로고침");
        refresh.addActionListener(e -> refresh());
        buttons.add(remove);
        buttons.add(clear);
        buttons.add(refresh);
        footer.add(buttons, BorderLayout.CENTER);
        JLabel note = new JLabel("주문 전 재고와 최종 금액은 서버에서 다시 확인합니다.");
        note.setForeground(new Color(116, 117, 137));
        footer.add(note, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        refresh();
    }

    private void refresh() {
        mSelectRow = -1;
        model.setRowCount(0);
        ArrayList<CartItem> items = cart.getmCartItem();
        long sum = 0;
        int count = 0;
        for (CartItem item : items) {
            int quantity = item.getQuantity();
            long itemPrice = (long) item.getItemBook().getUnitPrice() * quantity;
            sum += itemPrice;
            count += quantity;
            model.addRow(new Object[]{item.getBookID(), item.getItemBook().getName(),
                    KRW.format(item.getItemBook().getUnitPrice()), quantity, KRW.format(itemPrice)});
        }
        total.setText("총 상품 금액   " + KRW.format(sum) + "원");
        counter.setText(count + "권 / " + items.size() + "종");
        remove.setEnabled(false);
        clear.setEnabled(!items.isEmpty());
        table.clearSelection();
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "삭제할 상품을 선택해 주세요.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "선택한 도서를 장바구니에서 삭제할까요?", "삭제 확인",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (row >= cart.getmCartItem().size()) return;
        cart.removeCart(row);
        refresh();
    }

    private void clearCart() {
        if (cart.getmCartItem().isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "장바구니를 모두 비울까요?", "비우기 확인",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        cart.deleteBook();
        refresh();
    }
}
