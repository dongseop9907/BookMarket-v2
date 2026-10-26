package com.market.api;

import com.market.cart.Cart;
import com.market.cart.CartItem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Lookup the cart ISBN on the server, never scan a 27k+ book catalog. */
public final class OrderApi {
    private OrderApi() {}

    private static String resolveBookId(String isbn) throws ApiException {
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("Cart contains a book without ISBN. Remove it and add again.");
        String q = URLEncoder.encode(isbn.trim(), StandardCharsets.UTF_8);
        JSONObject response = ApiClient.get("/api/books?page=1&limit=50&q=" + q);
        JSONArray rows = response.optJSONArray("items");
        if (rows != null) for (int i = 0; i < rows.length(); i++) {
            JSONObject book = rows.getJSONObject(i);
            String serverIsbn = book.optString("isbn", "").trim();
            if (serverIsbn.equals(isbn.trim())) {
                String uuid = book.optString("id", "");
                try { UUID.fromString(uuid); } catch (Exception e) { throw new IllegalStateException("Invalid book identifier from server."); }
                if (book.optInt("price", 0) <= 0) throw new IllegalArgumentException("Book has no valid sales price: " + isbn);
                return uuid;
            }
        }
        throw new IllegalArgumentException("This book is no longer in the catalog (ISBN: " + isbn + "). Remove it from cart and add the current listing again.");
    }

    public static JSONObject place(Cart cart, String shippingAddress) throws ApiException {
        if (cart == null || cart.mCartItem.isEmpty()) throw new IllegalArgumentException("Cart is empty.");
        if (shippingAddress == null || shippingAddress.isBlank() || shippingAddress.length() > 500)
            throw new IllegalArgumentException("Shipping address must contain 1 to 500 characters.");
        JSONArray items = new JSONArray();
        for (CartItem ci : cart.mCartItem) {
            int qty = ci.getQuantity();
            if (qty < 1 || qty > 99) throw new IllegalArgumentException("Quantity must be between 1 and 99.");
            String uuid = resolveBookId(ci.getBookID());
            items.put(new JSONObject().put("bookId", uuid).put("quantity", qty));
        }
        JSONObject body = new JSONObject().put("shippingAddress", shippingAddress.trim()).put("items", items);
        JSONObject result = ApiClient.post("/api/orders", body);
        if (!result.optBoolean("ok", false) || !"PENDING".equals(result.optString("status")))
            throw new IllegalStateException("Unexpected order response.");
        return result;
    }

    public static JSONObject list() throws ApiException { return ApiClient.get("/api/orders"); }
    public static JSONObject cancel(String uuid) throws ApiException {
        UUID.fromString(uuid);
        return ApiClient.post("/api/orders/" + uuid + "/cancel", new JSONObject());
    }
}