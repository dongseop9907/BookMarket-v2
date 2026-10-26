package com.market.server.tools;

import com.market.server.config.DatabaseConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Local CLI. Official Open Library editions dump only; never an HTTP servlet. */
public final class OpenLibraryBulkImport {
    private OpenLibraryBulkImport() {}
    private static final String INSERT = "INSERT INTO public.books " +
            "(isbn,title,author,publisher,description,category,price,stock,published_date,thumbnail_url) " +
            "VALUES (?,?,?,?,?,'미분류',0,0,NULL,?) ON CONFLICT (isbn) DO NOTHING RETURNING id";
    private static String trimmed(String value, int max) {
        if (value == null) return "";
        value = value.strip();
        return value.length() > max ? value.substring(0, max) : value;
    }
    private static String firstString(JSONObject obj, String field) {
        JSONArray arr = obj.optJSONArray(field);
        return arr == null || arr.isEmpty() ? "" : arr.optString(0, "");
    }
    private static String isbn(JSONObject obj) {
        String candidate = firstString(obj, "isbn_13").replaceAll("[^0-9]", "");
        if (candidate.matches("[0-9]{13}")) return candidate;
        candidate = firstString(obj, "isbn_10").replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
        return candidate.matches("[0-9]{9}[0-9X]") ? candidate : "";
    }
    private static String title(JSONObject obj) { return trimmed(obj.optString("title", ""),300); }
    private static String publisher(JSONObject obj) { return trimmed(firstString(obj,"publishers"),200); }
    // Edition dump author references generally contain IDs, not names. Leave author blank rather than fabricate.
    private static String cover(JSONObject obj) {
        JSONArray arr = obj.optJSONArray("covers");
        if (arr != null && !arr.isEmpty()) {
            long id = arr.optLong(0, -1);
            if (id > 0) return "https://covers.openlibrary.org/b/id/" + id + "-M.jpg";
        }
        return "";
    }
    private static BufferedReader reader(Path path) throws IOException {
        InputStream stream = new BufferedInputStream(Files.newInputStream(path), 1 << 17);
        if (path.toString().toLowerCase(Locale.ROOT).endsWith(".gz")) stream = new GZIPInputStream(stream, 1 << 17);
        return new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8), 1 << 18);
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("Usage: <editions-dump.txt.gz> [--apply]");
        Path file = Path.of(args[0]);
        boolean apply = args.length == 2 && "--apply".equals(args[1]);
        if (args.length == 2 && !apply) throw new IllegalArgumentException("Only --apply is supported");
        if (!Files.isRegularFile(file)) throw new FileNotFoundException(file.toString());
        System.out.println("IMPORT_SOURCE=OPEN_LIBRARY_OFFICIAL_DUMP");
        System.out.println("MODE=" + (apply ? "APPLY" : "PREVIEW"));
        System.out.println("DUMP_FILE=" + file.getFileName());
        try (Connection conn = DatabaseConfig.getConnection()) {
            int original;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM public.books")) {
                rs.next(); original = rs.getInt(1);
            }
            final int target = 30000;
            int need = Math.max(0,target - original);
            System.out.println("BOOKS_BEFORE=" + original);
            System.out.println("TARGET_TOTAL=" + target);
            System.out.println("BOOKS_NEEDED=" + need);
            if (need == 0) { System.out.println("IMPORT_STATUS=ALREADY_AT_TARGET"); return; }
            if (!apply) {
                int valid = 0; int lines = 0;
                try (BufferedReader r = reader(file)) {
                    String row;
                    while ((row=r.readLine())!=null && valid < 10 && lines < 300000) {
                        lines++;
                        String[] fields = row.split("\\t",5);
                        if (fields.length < 5 || !"/type/edition".equals(fields[0])) continue;
                        try {
                            JSONObject entry = new JSONObject(fields[4]);
                            if (isbn(entry).isEmpty() || title(entry).isEmpty()) continue;
                            valid++;
                            System.out.println("SAMPLE_" + valid + "=" + title(entry));
                        } catch (RuntimeException ignored) { }
                    }
                }
                System.out.println("SAMPLE_COUNT=" + valid);
                System.out.println("DB_WRITES=0");
                System.out.println("IMPORT_STATUS=PREVIEW_OK");
                return;
            }
            conn.setAutoCommit(false);
            int saved = 0, scanned = 0, skipped = 0;
            Set<String> seen = new HashSet<>();
            try (PreparedStatement ps = conn.prepareStatement(INSERT);
                 BufferedReader r = reader(file)) {
                String row;
                while (saved < need && (row=r.readLine())!=null) {
                    scanned++;
                    String[] fields = row.split("\\t",5);
                    if (fields.length < 5 || !"/type/edition".equals(fields[0])) { skipped++; continue; }
                    try {
                        JSONObject entry = new JSONObject(fields[4]);
                        String isbn = isbn(entry), title = title(entry);
                        if (isbn.isEmpty() || title.isEmpty() || !seen.add(isbn)) { skipped++; continue; }
                        ps.setString(1,isbn);
                        ps.setString(2,title);
                        ps.setString(3,"");
                        ps.setString(4,publisher(entry));
                        ps.setString(5,"");
                        ps.setString(6,cover(entry));
                        try (ResultSet inserted = ps.executeQuery()) {
                            if (inserted.next()) saved++;
                        }
                        if (saved > 0 && saved % 100 == 0) {
                            conn.commit();
                            System.out.println("INSERTED=" + saved + " SCANNED=" + scanned);
                        }
                    } catch (RuntimeException | SQLException ex) {
                        // SQL problems must be surfaced (not silently skipped).
                        if (ex instanceof SQLException) throw (SQLException)ex;
                        skipped++;
                    }
                }
                conn.commit();
            } catch (Exception ex) { conn.rollback(); throw ex; }
            int after;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM public.books")) { rs.next(); after=rs.getInt(1); }
            System.out.println("INSERTED_TOTAL=" + saved);
            System.out.println("SCANNED_LINES=" + scanned);
            System.out.println("SKIPPED_LINES=" + skipped);
            System.out.println("BOOKS_AFTER=" + after);
            System.out.println("EXISTING_ROWS=UNCHANGED");
            System.out.println("NEW_BOOK_PRICE=0_NOT_FOR_SALE");
            System.out.println("NEW_BOOK_STOCK=0");
            System.out.println("IMPORT_STATUS=" + (after >= target ? "TARGET_REACHED" : "DUMP_EXHAUSTED"));
        }
    }
}
