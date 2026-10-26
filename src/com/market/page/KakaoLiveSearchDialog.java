package com.market.page;

import com.market.api.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/** Kakao live search: read-only display; not a BookMarket inventory item. */
public final class KakaoLiveSearchDialog extends JDialog {
    private static final Color BG = new Color(247, 247, 252);
    private static final Color PURPLE = new Color(112, 84, 225);
    private final JTextField keyword = new JTextField(22);
    private final JButton search = new JButton("검색");
    private final JButton previous = new JButton("이전");
    private final JButton next = new JButton("다음");
    private final JLabel pageLabel = new JLabel("1 페이지");
    private final JLabel status = new JLabel("검색어를 입력해 주세요.");
    private final JPanel cardGrid = new JPanel(new GridLayout(0, 2, 12, 12));
    private final AtomicInteger requestVersion = new AtomicInteger();
    private int currentPage = 1;
    private boolean isEnd = true;
    private String committedQuery = "";

    public KakaoLiveSearchDialog(Window owner) {
        super(owner, "카카오 도서 실시간 검색", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 12));
        getContentPane().setBackground(BG);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout(10, 8)); top.setOpaque(false);
        JLabel heading = new JLabel("카카오 도서 실시간 검색");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 21f));
        heading.setForeground(PURPLE);
        top.add(heading, BorderLayout.NORTH);
        JPanel searchRow = new JPanel(new BorderLayout(8, 0)); searchRow.setOpaque(false);
        searchRow.add(keyword, BorderLayout.CENTER);
        search.setBackground(PURPLE); search.setForeground(Color.WHITE);
        searchRow.add(search, BorderLayout.EAST);
        top.add(searchRow, BorderLayout.CENTER);
        JLabel disclosure = new JLabel("카카오 검색 결과는 미등록 상품이며 장바구니·주문이 불가능합니다.");
        disclosure.setForeground(new Color(110, 104, 130));
        top.add(disclosure, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        cardGrid.setBackground(BG);
        JScrollPane scroll = new JScrollPane(cardGrid,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(5, 0)); bottom.setOpaque(false);
        bottom.add(status, BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); controls.setOpaque(false);
        controls.add(previous); controls.add(pageLabel); controls.add(next);
        bottom.add(controls, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        previous.setEnabled(false); next.setEnabled(false);
        search.addActionListener(e -> beginSearch());
        keyword.addActionListener(e -> beginSearch());
        previous.addActionListener(e -> { if (currentPage > 1) {currentPage--; load();} });
        next.addActionListener(e -> { if (!isEnd && currentPage < 50) {currentPage++; load();} });
        setMinimumSize(new Dimension(570, 420));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(790, screen.width - 50), Math.min(640, screen.height - 80));
        setLocationRelativeTo(owner);
    }

    private void beginSearch() {
        String q = keyword.getText().trim();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "검색어를 입력해 주세요."); return;
        }
        if (q.length() > 100) {
            JOptionPane.showMessageDialog(this, "검색어는 100자 이내로 입력해 주세요."); return;
        }
        committedQuery = q;
        currentPage = 1;
        load();
    }

    private void load() {
        final int version = requestVersion.incrementAndGet();
        final int page = currentPage;
        final String q = committedQuery;
        search.setEnabled(false); previous.setEnabled(false); next.setEnabled(false);
        status.setText("카카오에서 검색하는 중...");
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return ApiClient.get("/api/discover/kakao?q=" +
                    URLEncoder.encode(q, StandardCharsets.UTF_8) + "&page=" + page + "&size=20");
            }
            @Override protected void done() {
                if (version != requestVersion.get() || !isDisplayable()) return;
                search.setEnabled(true);
                try {
                    JSONObject data = get();
                    JSONArray items = data.optJSONArray("items");
                    isEnd = data.optBoolean("is_end", true);
                    cardGrid.removeAll();
                    int count = items == null ? 0 : items.length();
                    for (int i = 0; i < count; i++) cardGrid.add(card(items.getJSONObject(i)));
                    if (count == 0) cardGrid.add(new JLabel("검색 결과가 없습니다."));
                    pageLabel.setText(page + " 페이지");
                    previous.setEnabled(page > 1);
                    next.setEnabled(!isEnd && page < 50);
                    status.setText("현재 " + count + "권 · 실시간 조회 · DB 저장 없음");
                    cardGrid.revalidate(); cardGrid.repaint();
                    scrollToTop();
                } catch (Exception ex) {
                    status.setText("조회 실패: Tomcat 또는 카카오 API 상태를 확인하세요.");
                    JOptionPane.showMessageDialog(KakaoLiveSearchDialog.this,
                        status.getText(), "검색 실패", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void scrollToTop() {
        SwingUtilities.invokeLater(() -> {
            Container c = SwingUtilities.getAncestorOfClass(JScrollPane.class, cardGrid);
            if (c instanceof JScrollPane s) s.getViewport().setViewPosition(new Point(0,0));
        });
    }

    private JPanel card(JSONObject item) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224,223,236)), new EmptyBorder(10,10,10,10)));
        panel.setPreferredSize(new Dimension(325, 168));
        JLabel cover = new JLabel("표지 없음", SwingConstants.CENTER);
        cover.setPreferredSize(new Dimension(92, 132));
        cover.setOpaque(true); cover.setBackground(new Color(240,240,246));
        panel.add(cover, BorderLayout.WEST);
        String thumbnail = item.optString("thumbnail", "");
        if (thumbnail.startsWith("https://")) {
            new SwingWorker<ImageIcon,Void>() {
                @Override protected ImageIcon doInBackground() throws Exception {
                    var connection = new URL(thumbnail).openConnection();
                    connection.setConnectTimeout(2500); connection.setReadTimeout(2500);
                    try (var in = connection.getInputStream()) {
                        byte[] bytes = in.readNBytes(1000000);
                        ImageIcon raw = new ImageIcon(bytes);
                        return raw.getIconWidth() > 0 ? new ImageIcon(raw.getImage().getScaledInstance(92,132,Image.SCALE_SMOOTH)) : null;
                    }
                }
                @Override protected void done() {
                    try { ImageIcon img = get(); if (img != null && cover.isDisplayable()) { cover.setIcon(img); cover.setText(""); } }
                    catch (Exception ignored) {}
                }
            }.execute();
        }
        JPanel info = new JPanel(); info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        String title = item.optString("title", "");
        JLabel name = new JLabel(cut(title, 22));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setToolTipText(title);
        info.add(name); info.add(Box.createVerticalStrut(5));
        JSONArray authors = item.optJSONArray("authors");
        info.add(new JLabel(cut(authors != null ? authors.join(", ").replace("\"", "") : "", 24)));
        info.add(Box.createVerticalStrut(6));
        info.add(new JLabel("정가: " + won(item.optInt("price", 0))));
        info.add(new JLabel("판매가: " + won(item.optInt("sale_price", 0))));
        info.add(Box.createVerticalStrut(7));
        JButton details = new JButton("도서 정보");
        details.addActionListener(e -> JOptionPane.showMessageDialog(KakaoLiveSearchDialog.this,
            "제목: " + title + "\n저자: " + (authors != null ? authors.join(", ") : "")
            + "\nISBN: " + item.optString("isbn", "") + "\n출판사: " + item.optString("publisher", "")
            + "\n정가: " + won(item.optInt("price",0)) + "\n판매가: " + won(item.optInt("sale_price",0))
            + "\n\n검색 결과는 주문할 수 없습니다.", "카카오 도서 정보", JOptionPane.INFORMATION_MESSAGE));
        info.add(details);
        JLabel unavailable = new JLabel("조회 전용 · 장바구니 불가");
        unavailable.setForeground(new Color(150,90,90));
        info.add(unavailable);
        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    private static String cut(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
    private static String won(int amount) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(amount) + "원";
    }
}
