package com.market.page;

import com.market.bookitem.Book;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

/** Read-only book details. Cart actions remain in CartAddItemPage. */
public final class BookDetailsDialog {
    private BookDetailsDialog() {}

    public static void open(Component owner, Book book) {
        Window window = SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(window, "BookMarket | Book Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(22, 14));
        root.setBackground(new Color(250, 249, 255));
        root.setBorder(new EmptyBorder(24, 24, 20, 24));

        JLabel cover = new JLabel("NO COVER", SwingConstants.CENTER);
        cover.setPreferredSize(new Dimension(195, 275));
        cover.setOpaque(true);
        cover.setBackground(new Color(237, 234, 247));
        cover.setForeground(new Color(110, 103, 133));
        cover.setBorder(BorderFactory.createLineBorder(new Color(219, 215, 232)));
        root.add(cover, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("<html><body style='width:320px'>" + html(book.getName()) + "</body></html>");
        title.setFont(new Font("Dialog", Font.BOLD, 20));
        title.setForeground(new Color(43, 37, 73));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(title);
        info.add(Box.createVerticalStrut(18));
        addLine(info, "ISBN", book.getBookId());
        addLine(info, "Author", book.getAuthor());
        addLine(info, "Publisher", book.getDescription());
        addLine(info, "Category", book.getCategory());
        addLine(info, "Published", book.getReleaseDate());
        addLine(info, "Price", NumberFormat.getIntegerInstance(Locale.KOREA).format(book.getUnitPrice()) + " KRW");
        addLine(info, "Stock", book.getStock() + " (demo inventory)");
        root.add(info, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setSize(680, 435);
        dialog.setMinimumSize(new Dimension(610, 400));
        dialog.setLocationRelativeTo(owner);

        String thumb = book.getThumbnailUrl();
        if (thumb != null && thumb.startsWith("https://")) {
            new SwingWorker<ImageIcon, Void>() {
                @Override protected ImageIcon doInBackground() throws Exception {
                    var connection = new URL(thumb).openConnection();
                    connection.setConnectTimeout(2500);
                    connection.setReadTimeout(2500);
                    try (var stream = connection.getInputStream()) {
                        byte[] bytes = stream.readNBytes(2000000);
                        ImageIcon original = new ImageIcon(bytes);
                        if (original.getIconWidth() < 1 || original.getIconHeight() < 1) return null;
                        Image resized = original.getImage().getScaledInstance(195, 275, Image.SCALE_SMOOTH);
                        return new ImageIcon(resized);
                    }
                }
                @Override protected void done() {
                    if (!dialog.isDisplayable()) return;
                    try {
                        ImageIcon image = get();
                        if (image != null) { cover.setText(""); cover.setIcon(image); }
                    } catch (Exception ignored) { /* keep placeholder */ }
                }
            }.execute();
        }
        dialog.setVisible(true);
    }

    private static void addLine(JPanel p, String key, String value) {
        JLabel row = new JLabel("<html><body style='width:320px'><b>" + html(key) + ":</b> " + html(value) + "</body></html>");
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(0, 0, 8, 0));
        p.add(row);
    }

    private static String html(String text) {
        if (text == null || text.isBlank()) return "-";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                   .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
