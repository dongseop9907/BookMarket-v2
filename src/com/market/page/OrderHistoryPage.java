package com.market.page;

import com.market.api.OrderApi;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Lists the signed-in user's orders; server checks ownership on cancellation. */
public final class OrderHistoryPage extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"주문번호", "주문일시", "총금액(원)", "상태"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton refresh = new JButton("새로고침");
    private final JButton cancel = new JButton("선택 주문 취소");
    private final JLabel status = new JLabel("주문 내역을 불러오는 중...");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public OrderHistoryPage() {
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(247, 247, 253));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("내 주문 내역");
        title.setFont(new Font("Dialog", Font.BOLD, 24));
        title.setForeground(new Color(28, 33, 58));
        top.add(title, BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        table.setRowHeight(36);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(239, 236, 255));
        table.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(620, 330));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(227, 228, 240)));
        add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(new JLabel("취소 가능 상태: PENDING (실제 결제 없음)"));
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        cancel.setEnabled(false);
        refresh.addActionListener(e -> load());
        cancel.addActionListener(e -> cancelSelected());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateCancelState();
        });
        load();
    }

    private void updateCancelState() {
        int view = table.getSelectedRow();
        cancel.setEnabled(refresh.isEnabled() && view >= 0 &&
                "PENDING".equals(model.getValueAt(table.convertRowIndexToModel(view), 3)));
    }
    private void load() {
        refresh.setEnabled(false);
        cancel.setEnabled(false);
        status.setText("서버에서 주문 내역 조회 중...");
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception { return OrderApi.list(); }
            @Override protected void done() {
                refresh.setEnabled(true);
                try {
                    JSONObject response = get();
                    JSONArray items = response.getJSONArray("items");
                    model.setRowCount(0);
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject row = items.getJSONObject(i);
                        String date = row.optString("createdAt", "");
                        try { date = DATE.format(Instant.parse(date)); } catch (Exception ignored) {}
                        model.addRow(new Object[]{row.optString("orderId", ""), date,
                                row.optLong("totalPrice", 0), row.optString("status", "")});
                    }
                    status.setText("주문 " + items.length() + "건 표시 (최근 최대 50건)");
                    updateCancelState();
                } catch (Exception ex) {
                    status.setText("주문 조회 실패");
                    showError(ex);
                }
            }
        }.execute();
    }
    private void cancelSelected() {
        int view = table.getSelectedRow();
        if (view < 0) return;
        int row = table.convertRowIndexToModel(view);
        if (!"PENDING".equals(model.getValueAt(row, 3))) return;
        String id = model.getValueAt(row, 0).toString();
        int yes = JOptionPane.showConfirmDialog(this,
                "주문 " + id + "을(를) 취소하고 재고를 복구할까요?",
                "주문 취소 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (yes != JOptionPane.YES_OPTION) return;
        cancel.setEnabled(false);
        refresh.setEnabled(false);
        status.setText("주문 취소 요청 중...");
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception { return OrderApi.cancel(id); }
            @Override protected void done() {
                refresh.setEnabled(true);
                try {
                    JSONObject response = get();
                    if (!response.optBoolean("ok") || !"CANCELLED".equals(response.optString("status")))
                        throw new IllegalStateException("서버에서 취소 완료를 확인하지 못했습니다.");
                    JOptionPane.showMessageDialog(OrderHistoryPage.this,
                            "주문이 취소되었습니다. 재고 복구 결과는 목록과 관리자 화면에서 확인하세요.");
                    load();
                } catch (Exception ex) {
                    status.setText("취소 실패 - 주문 내역을 다시 확인하세요");
                    updateCancelState();
                    showError(ex);
                }
            }
        }.execute();
    }
    private void showError(Exception e) {
        Throwable c = e;
        while (c.getCause() != null) c = c.getCause();
        JOptionPane.showMessageDialog(this, String.valueOf(c.getMessage()),
                "BookMarket 주문 오류", JOptionPane.ERROR_MESSAGE);
    }
}
