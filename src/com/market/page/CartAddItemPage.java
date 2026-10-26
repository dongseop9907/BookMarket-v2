package com.market.page;

import com.market.api.ApiClient;
import com.market.bookitem.Book;
import com.market.cart.Cart;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/** Server-side pagination: never loads 30,000 books into the Swing heap. */
public final class CartAddItemPage extends JPanel {
    private static final int PAGE_SIZE = 20;
    private static final Color BG = new Color(247, 247, 252);
    private static final Color PRIMARY = new Color(112, 84, 225);
    private final Cart cart;
    private final JTextField query = new JTextField(16);
    private final JTextField category = new JTextField(12);
    private final JLabel status = new JLabel("불러오는 중...");
    private final JLabel pager = new JLabel("1 / 1");
    private final JButton previous = new JButton("이전");
    private final JButton next = new JButton("다음");
    private final JButton search = new JButton("검색");
    // BM_CATALOG_NAV_V1: navigation controls on server-side paginated catalog.
    private final JTextField pageInput = new JTextField("1", 4);
    private final JButton jump = new JButton("Go");
    private final JButton clear = new JButton("Reset");
    private final JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
    private final AtomicInteger requestVersion = new AtomicInteger();
    private int page = 1;
    private int totalPages = 1;

    public CartAddItemPage(JPanel parent, Cart cart) {
        this.cart = cart;
        setLayout(new BorderLayout(12, 12));
        setPreferredSize(new Dimension(930, 490));
        setBackground(BG);
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.setOpaque(false);
        JLabel heading = new JLabel("도서 쇼핑");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        top.add(heading, BorderLayout.WEST);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("제목·저자·ISBN")); filters.add(query);
        filters.add(new JLabel("카테고리")); filters.add(category); filters.add(search);
        JButton kakaoLiveButton = new JButton("카카오 실시간 검색");
        kakaoLiveButton.addActionListener(e -> new KakaoLiveSearchDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true));
        filters.add(kakaoLiveButton);
        filters.add(clear);
        top.add(filters, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        grid.setBackground(BG);
        JScrollPane scroller = new JScrollPane(grid, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.getViewport().setBackground(BG);
        scroller.getVerticalScrollBar().setUnitIncrement(20);
        add(scroller, BorderLayout.CENTER);
        JPanel foot = new JPanel(new BorderLayout()); foot.setOpaque(false);
        foot.add(status, BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); controls.setOpaque(false);
        controls.add(previous); controls.add(pager); controls.add(next);
        controls.add(new JLabel("Page")); controls.add(pageInput); controls.add(jump);
        foot.add(controls, BorderLayout.EAST); add(foot, BorderLayout.SOUTH);
        search.addActionListener(e -> { page = 1; load(); });
        clear.addActionListener(e -> { query.setText(""); category.setText(""); page = 1; load(); });
        jump.addActionListener(e -> goToPage());
        pageInput.addActionListener(e -> goToPage());
        query.addActionListener(e -> { page = 1; load(); });
        category.addActionListener(e -> { page = 1; load(); });
        previous.addActionListener(e -> { if(page > 1) { page--; load(); } });
        next.addActionListener(e -> { if(page < totalPages) { page++; load(); } });
        load();
    }

    private void goToPage() {
        try {
            int target = Integer.parseInt(pageInput.getText().trim());
            if (target < 1 || target > totalPages) {
                JOptionPane.showMessageDialog(this, "Enter a page between 1 and " + totalPages);
                return;
            }
            page = target;
            load();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Page must be a number.");
        }
    }

    private void load() {
        final int version = requestVersion.incrementAndGet();
        final int requestedPage = page;
        final String q = query.getText().trim();
        final String c = category.getText().trim();
        search.setEnabled(false); previous.setEnabled(false); next.setEnabled(false); jump.setEnabled(false); clear.setEnabled(false);
        status.setText("서버에서 도서를 불러오는 중...");
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                String path = "/api/books?page=" + requestedPage + "&limit=" + PAGE_SIZE
                        + "&q=" + encode(q) + "&category=" + encode(c);
                return ApiClient.get(path);
            }
            @Override protected void done() {
                if(version != requestVersion.get()) return;
                search.setEnabled(true); jump.setEnabled(true); clear.setEnabled(true);
                try {
                    JSONObject data = get();
                    int total = data.getInt("total");
                    totalPages = Math.max(1, data.optInt("totalPages", 1));
                    JSONArray items = data.getJSONArray("items");
                    grid.removeAll();
                    for(int i = 0; i < items.length(); i++) {
                        JSONObject j = items.getJSONObject(i);
                        Book b = new Book(j.optString("isbn", ""), j.optString("title", ""),
                                j.optString("author", ""), j.optInt("price", 0),
                                j.optInt("stock", 0), j.optString("publisher", ""),
                                j.optString("category", ""), j.optString("published_date", ""));
                        b.setStock(j.optInt("stock", 0));
                        b.setThumbnailUrl(j.optString("thumbnail_url", ""));
                        grid.add(card(b));
                    }
                    if(items.length() == 0) grid.add(new JLabel("검색 결과가 없습니다."));
                    pager.setText(requestedPage + " / " + totalPages);
                    pageInput.setText(Integer.toString(requestedPage));
                    previous.setEnabled(requestedPage > 1);
                    next.setEnabled(requestedPage < totalPages);
                    status.setText("전체 " + NumberFormat.getIntegerInstance(Locale.KOREA).format(total)
                            + "권 · 현재 " + items.length() + "권 표시");
                    grid.revalidate(); grid.repaint();
                    scrollerTop();
                } catch(Exception ex) {
                    status.setText("조회 실패: Tomcat 서버 연결 및 API를 확인하세요.");
                    JOptionPane.showMessageDialog(CartAddItemPage.this, status.getText(), "도서 조회 오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void scrollerTop() { SwingUtilities.invokeLater(() -> {
        Container p = SwingUtilities.getAncestorOfClass(JScrollPane.class, grid);
        if(p instanceof JScrollPane s) s.getViewport().setViewPosition(new Point(0, 0));
    }); }

    private JPanel card(Book book) {
        JPanel card = new JPanel(new BorderLayout(10, 7));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 223, 236)), new EmptyBorder(10, 10, 10, 10)));
        JLabel cover = new JLabel("표지 없음", SwingConstants.CENTER);
        cover.setPreferredSize(new Dimension(94, 135));
        cover.setOpaque(true); cover.setBackground(new Color(242, 242, 248));
        cover.setForeground(new Color(135, 135, 148));
        String url = book.getThumbnailUrl();
        if(url != null && url.startsWith("https://")) {
            new SwingWorker<ImageIcon, Void>() {
                @Override protected ImageIcon doInBackground() throws Exception {
                    URL u = new URL(url);
                    var conn = u.openConnection(); conn.setConnectTimeout(2000); conn.setReadTimeout(2000);
                    try(var stream = conn.getInputStream()) {
                        byte[] bytes = stream.readNBytes(1500000);
                        ImageIcon original = new ImageIcon(bytes);
                        if(original.getIconWidth() <= 0) return null;
                        return new ImageIcon(original.getImage().getScaledInstance(94, 135, Image.SCALE_SMOOTH));
                    }
                }
                @Override protected void done() { try { ImageIcon icon = get(); if(icon != null && cover.isDisplayable()) {
                    cover.setText(""); cover.setIcon(icon); } } catch(Exception ignored){} }
            }.execute();
        }
        card.add(cover, BorderLayout.WEST);
        JPanel details = new JPanel(); details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(shortText(book.getName(), 23)); name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
        name.setToolTipText(book.getName());
        details.add(name); details.add(Box.createVerticalStrut(6));
        details.add(new JLabel(shortText(book.getAuthor(), 25))); details.add(Box.createVerticalStrut(8));
        details.add(new JLabel(NumberFormat.getIntegerInstance(Locale.KOREA).format(book.getUnitPrice()) + "원"));
        details.add(new JLabel("재고: " + book.getStock() + "권"));
        details.add(Box.createVerticalStrut(5));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); buttons.setOpaque(false);
        JButton detail = new JButton("상세");
        detail.addActionListener(e -> BookDetailsDialog.open(this, book)); // BM_DETAIL_DIALOG_V2
        JButton add = new JButton("담기"); add.setBackground(PRIMARY); add.setForeground(Color.WHITE);
        add.setEnabled(book.getStock() > 0 && book.getUnitPrice() > 0);
        add.addActionListener(e -> {
            if(!cart.isCartInBook(book.getBookId())) cart.insertBook(book);
            JOptionPane.showMessageDialog(this, "장바구니에 추가했습니다.");
        });
        buttons.add(detail); buttons.add(add); details.add(buttons);
        card.add(details, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(300, 165));
        return card;
    }

    private static String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String shortText(String s,int max) { if(s == null) return ""; return s.length() <= max ? s : s.substring(0, max - 1) + "…"; }
}
