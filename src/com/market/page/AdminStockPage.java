package com.market.page;

import com.market.api.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Inventory editor. All authority checks are enforced again on the server. */
public final class AdminStockPage extends JPanel {
    private final JTextField query = new JTextField(25);
    private final JSpinner stock = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 1));
    private final JLabel status = new JLabel("도서를 불러오는 중...");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "제목", "저자", "가격", "재고"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton search = new JButton("검색");
    private final JButton save = new JButton("선택 도서 재고 저장");

    // BM_ADMIN_NAV_V21
    private final JButton bmPrev = new JButton("Prev");
    private final JButton bmNext = new JButton("Next");
    private final JButton bmReset = new JButton("Reset");
    private final JComboBox<String> bmFilter = new JComboBox<>(new String[]{"All", "Out of stock", "Low stock (1-5)"});
    private final JLabel bmPage = new JLabel("1 / 1");
    private int bmPageNumber = 1;
    private int bmPageMax = 1;
    private int bmRequestId = 0;

    // BM_ADMIN_DASH_V4
    private final JLabel bmStats = new JLabel("Inventory overview: loading...");
    private final JButton bmStatsRefresh = new JButton("Refresh stats");
    public AdminStockPage() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel head = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("도서 재고 관리");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        head.add(title, BorderLayout.NORTH);
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("도서 검색"));
        searchRow.add(query);
        searchRow.add(search);
        searchRow.add(bmReset);
        searchRow.add(bmFilter);
        searchRow.add(bmPrev);
        searchRow.add(bmPage);
        searchRow.add(bmNext);
        head.add(searchRow, BorderLayout.CENTER);
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsRow.add(bmStats);
        statsRow.add(bmStatsRefresh);
        head.add(statsRow, BorderLayout.SOUTH);
        add(head, BorderLayout.NORTH);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        // BOOKMARKET_ADMIN_LAYOUT_V1: limit table viewport height so stock editor stays visible.
        JScrollPane bookScroll = new JScrollPane(table);
        bookScroll.setPreferredSize(new Dimension(520, 220));
        bookScroll.setMinimumSize(new Dimension(280, 100));
        bookScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        bookScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(bookScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("변경할 재고"));
        controls.add(stock);
        controls.add(save);
        bottom.add(controls, BorderLayout.NORTH);
        bottom.add(status, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        save.setEnabled(false);
        search.addActionListener(e -> { bmPageNumber = 1; loadBooks(); });
        bmReset.addActionListener(e -> { query.setText(""); bmFilter.setSelectedIndex(0); bmPageNumber = 1; loadBooks(); });
        bmFilter.addActionListener(e -> { bmPageNumber = 1; loadBooks(); });
        bmPrev.addActionListener(e -> { if (bmPageNumber > 1) { bmPageNumber--; loadBooks(); } });
        bmNext.addActionListener(e -> { if (bmPageNumber < bmPageMax) { bmPageNumber++; loadBooks(); } });
        query.addActionListener(e -> { bmPageNumber = 1; loadBooks(); });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            save.setEnabled(row >= 0);
            if (row >= 0) stock.setValue(Integer.parseInt(model.getValueAt(table.convertRowIndexToModel(row), 4).toString()));
        });
        save.addActionListener(e -> saveStock());
        bmStatsRefresh.addActionListener(e -> bmLoadStats());
        bmLoadStats();
        loadBooks();
    }

    private void loadBooks() {
        search.setEnabled(false);
        save.setEnabled(false);
        bmPrev.setEnabled(false);
        bmNext.setEnabled(false);
        status.setText("Loading...");
        final String keyword = query.getText().trim();
        final int requestedPage = bmPageNumber;
        final int filter = bmFilter.getSelectedIndex();
        // BM_ADMIN_STOCK_V3: filter before LIMIT/OFFSET on the server.
        final int requestId = ++bmRequestId;
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                String path = "/api/books?page=" + requestedPage + "&limit=50";
                if (!keyword.isEmpty()) path += "&q=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
                if (filter == 1) path += "&stockFilter=out";
                else if (filter == 2) path += "&stockFilter=low";
                return ApiClient.get(path);
            }
            @Override protected void done() {
                if (requestId != bmRequestId) return;
                search.setEnabled(true);
                try {
                    JSONObject response = get();
                    JSONArray items = response.getJSONArray("items");
                    int total = response.optInt("total", 0);
                    bmPageMax = Math.max(1, (total + 49) / 50);
                    bmPageNumber = Math.min(requestedPage, bmPageMax);
                    model.setRowCount(0);
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        int qty = item.optInt("stock", 0);
                        /* Stock filtering is server-side. */
                        /* No client-side stock filter. */
                        model.addRow(new Object[]{item.optString("id"), item.optString("title"), item.optString("author"), item.optInt("price"), qty});
                    }
                    bmPage.setText(bmPageNumber + " / " + bmPageMax);
                    bmPrev.setEnabled(bmPageNumber > 1);
                    bmNext.setEnabled(bmPageNumber < bmPageMax);
                    status.setText("Visible: " + model.getRowCount() + " / results: " + total + (filter == 0 ? "" : " (all-books filter)"));
                } catch (Exception ex) { status.setText("Load failed"); showError(ex); }
            }
        }.execute();
    }
    private void bmLoadStats() {
        bmStatsRefresh.setEnabled(false);
        bmStats.setText("Inventory overview: loading...");
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return ApiClient.get("/api/admin/books/stock");
            }
            @Override protected void done() {
                bmStatsRefresh.setEnabled(true);
                try {
                    JSONObject r = get();
                    bmStats.setText("Books: " + r.getLong("total")
                            + "  |  Out: " + r.getLong("outOfStock")
                            + "  |  Low (1-5): " + r.getLong("lowStock")
                            + "  |  Units: " + r.getLong("totalStock"));
                } catch (Exception ex) {
                    bmStats.setText("Inventory overview unavailable (login as ADMIN)");
                }
            }
        }.execute();
    }
    private void saveStock() {
        int selected = table.getSelectedRow();
        if (selected < 0) return;
        int row = table.convertRowIndexToModel(selected);
        final String bookId = model.getValueAt(row, 0).toString();
        final String bookTitle = model.getValueAt(row, 1).toString();
        final int newStock = (Integer) stock.getValue();
        int confirm = JOptionPane.showConfirmDialog(this, "'" + bookTitle + "' 재고를 " + newStock + "권으로 변경할까요?", "재고 변경 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        save.setEnabled(false);
        search.setEnabled(false);
        status.setText("재고 저장 중...");
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return ApiClient.post("/api/admin/books/stock", new JSONObject().put("bookId", bookId).put("stock", newStock));
            }
            @Override protected void done() {
                search.setEnabled(true);
                try {
                    JSONObject response = get();
                    if (!response.optBoolean("ok")) throw new IllegalStateException("서버에서 저장을 확인하지 못했습니다.");
                    status.setText("재고 저장 완료: " + bookTitle + " / " + newStock + "권");
                    loadBooks();
                } catch (Exception ex) { status.setText("재고 저장 실패"); save.setEnabled(table.getSelectedRow() >= 0); showError(ex); }
            }
        }.execute();
    }

    private void showError(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        JOptionPane.showMessageDialog(this, cause.getMessage(), "BookMarket 오류", JOptionPane.ERROR_MESSAGE);
    }
}
