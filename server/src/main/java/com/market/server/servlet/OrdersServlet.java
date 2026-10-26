package com.market.server.servlet;

import com.market.server.config.DatabaseConfig;
import com.market.server.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/** Transactional orders. No payment gateway: every new order is PENDING, never PAID. */
@WebServlet("/api/orders/*")
public final class OrdersServlet extends HttpServlet {
    private static final int MAX_DISTINCT = 30;
    private static final int MAX_QTY = 99;
    private record Line(UUID bookId, int qty) {}
    private record Book(int price, int stock) {}
    private static final class BadRequest extends Exception { BadRequest(String msg) {super(msg);} }
    private static final class Conflict extends Exception { Conflict(String msg) {super(msg);} }

    private UUID currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) return null;
        try { return UUID.fromString(s.getAttribute("userId").toString()); }
        catch (IllegalArgumentException e) { return null; }
    }
    private boolean verifyWrite(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!"1".equals(req.getHeader("X-BookMarket-Request"))) {
            JsonUtil.write(res, 403, JsonUtil.error("REQUEST_HEADER_REQUIRED", "요청 헤더가 필요합니다.")); return false;
        }
        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !origin.equals("http://localhost:8080")
                && !origin.equals("http://127.0.0.1:8080")) {
            JsonUtil.write(res, 403, JsonUtil.error("INVALID_ORIGIN", "출처가 허용되지 않습니다.")); return false;
        }
        return true;
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control", "no-store");
        UUID user = currentUser(req);
        if (user == null) {JsonUtil.write(res,401,JsonUtil.error("UNAUTHORIZED","로그인이 필요합니다.")); return;}
        if (!verifyWrite(req,res)) return;
        String path = req.getPathInfo();
        try {
            if (path == null || path.equals("/")) create(req,res,user);
            else if (path.matches("/[0-9a-fA-F-]{36}/cancel")) cancel(res,user,UUID.fromString(path.substring(1,path.length()-7)));
            else JsonUtil.write(res,404,JsonUtil.error("NOT_FOUND","알 수 없는 경로입니다."));
        } catch (BadRequest ex) {JsonUtil.write(res,400,JsonUtil.error("INVALID_REQUEST",ex.getMessage()));}
          catch (Conflict ex) {JsonUtil.write(res,409,JsonUtil.error("ORDER_CONFLICT",ex.getMessage()));}
          catch (SQLException ex) {getServletContext().log("Order database error SQLSTATE="+ex.getSQLState()); JsonUtil.write(res,503,JsonUtil.error("DATABASE_UNAVAILABLE","주문 처리를 완료하지 못했습니다."));}
          catch (JSONException | IllegalArgumentException ex) {JsonUtil.write(res,400,JsonUtil.error("INVALID_REQUEST","입력 형식을 확인하세요."));}
          catch (Exception ex) {getServletContext().log("Order server error",ex); JsonUtil.write(res,500,JsonUtil.error("INTERNAL_ERROR","주문 처리 오류"));}
    }
    private void create(HttpServletRequest req,HttpServletResponse res,UUID user) throws Exception {
        JSONObject payload=JsonUtil.readObject(req);
        String address=payload.optString("shippingAddress", "").trim();
        if (address.isBlank() || address.length()>500) throw new BadRequest("배송 주소를 1~500자로 입력하세요.");
        JSONArray items=payload.optJSONArray("items");
        if (items==null || items.isEmpty() || items.length()>MAX_DISTINCT) throw new BadRequest("상품은 1~30종류까지 선택하세요.");
        Map<UUID,Integer> requested=new TreeMap<>();
        for(int i=0;i<items.length();i++) {
            JSONObject item=items.getJSONObject(i);
            UUID id=UUID.fromString(item.getString("bookId"));
            Object q=item.get("quantity");
            if (!(q instanceof Integer || q instanceof Long)) throw new BadRequest("수량은 정수여야 합니다.");
            long qty=((Number)q).longValue();
            if(qty<1 || qty>MAX_QTY || requested.containsKey(id)) throw new BadRequest("도서 중복 또는 수량 범위(1~99) 오류");
            requested.put(id,(int)qty);
        }
        UUID orderId=UUID.randomUUID(); long total=0;
        try(Connection c=DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<UUID,Book> books=new LinkedHashMap<>();
                // UUID sorting gives concurrent orders a consistent row-lock order.
                try(PreparedStatement ps=c.prepareStatement("select price, stock from public.books where id=? for update")) {
                    for(var entry:requested.entrySet()) {
                        ps.setObject(1,entry.getKey());
                        try(ResultSet rs=ps.executeQuery()) {
                            if(!rs.next()) throw new Conflict("도서를 찾을 수 없습니다.");
                            int price=rs.getInt("price"),stock=rs.getInt("stock");
                            if(price<=0 || stock<entry.getValue()) throw new Conflict("가격이나 재고가 부족합니다.");
                            books.put(entry.getKey(),new Book(price,stock));
                            total+=((long)price)*entry.getValue();
                        }
                    }
                }
                if(total>Integer.MAX_VALUE) throw new BadRequest("총액 제한을 초과했습니다.");
                try(PreparedStatement ps=c.prepareStatement("insert into public.orders(id,user_id,total_price,shipping_address,status) values(?,?,?,?,'PENDING')")) {
                    ps.setObject(1,orderId);ps.setObject(2,user);ps.setInt(3,(int)total);ps.setString(4,address);ps.executeUpdate();
                }
                try(PreparedStatement itemPs=c.prepareStatement("insert into public.order_items(order_id,book_id,quantity,price) values(?,?,?,?)");
                    PreparedStatement stockPs=c.prepareStatement("update public.books set stock=stock-? where id=? and stock>=?")) {
                    for(var entry:requested.entrySet()) {
                        int qty=entry.getValue(), price=books.get(entry.getKey()).price();
                        itemPs.setObject(1,orderId);itemPs.setObject(2,entry.getKey());itemPs.setInt(3,qty);itemPs.setInt(4,price);itemPs.executeUpdate();
                        stockPs.setInt(1,qty);stockPs.setObject(2,entry.getKey());stockPs.setInt(3,qty);
                        if(stockPs.executeUpdate()!=1) throw new Conflict("재고가 변경되었습니다.");
                    }
                }
                c.commit();
            } catch(Exception ex) { c.rollback(); throw ex; }
        }
        JsonUtil.write(res,201,new JSONObject().put("ok",true).put("orderId",orderId.toString()).put("status","PENDING").put("totalPrice",total).put("paymentCaptured",false));
    }
    private void cancel(HttpServletResponse res,UUID user,UUID orderId) throws Exception {
        try(Connection c=DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                String status;
                try(PreparedStatement ps=c.prepareStatement("select status from public.orders where id=? and user_id=? for update")) {
                    ps.setObject(1,orderId); ps.setObject(2,user);
                    try(ResultSet rs=ps.executeQuery()) {
                        if(!rs.next()) {c.rollback();JsonUtil.write(res,404,JsonUtil.error("ORDER_NOT_FOUND","주문을 찾을 수 없습니다."));return;}
                        status=rs.getString(1);
                    }
                }
                if(!"PENDING".equals(status)) {c.rollback();JsonUtil.write(res,409,JsonUtil.error("NOT_CANCELLABLE","대기 중인 주문만 취소할 수 있습니다."));return;}
                try(PreparedStatement ps=c.prepareStatement("select book_id,quantity from public.order_items where order_id=? order by book_id")) {
                    ps.setObject(1,orderId);
                    try(ResultSet rs=ps.executeQuery()) {
                        try(PreparedStatement restore=c.prepareStatement("update public.books set stock=stock+? where id=?")) {
                            while(rs.next()) {
                                restore.setInt(1,rs.getInt("quantity")); restore.setObject(2,rs.getObject("book_id"));
                                if(restore.executeUpdate()!=1) throw new Conflict("복구할 도서가 없습니다.");
                            }
                        }
                    }
                }
                try(PreparedStatement ps=c.prepareStatement("update public.orders set status='CANCELLED' where id=? and user_id=? and status='PENDING'")) {
                    ps.setObject(1,orderId);ps.setObject(2,user);
                    if(ps.executeUpdate()!=1) throw new Conflict("주문 상태가 변경되었습니다.");
                }
                c.commit();
            } catch(Exception ex) {c.rollback();throw ex;}
        }
        JsonUtil.write(res,200,new JSONObject().put("ok",true).put("orderId",orderId.toString()).put("status","CANCELLED").put("stockRestored",true));
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control","no-store");
        UUID user=currentUser(req);
        if(user==null) {JsonUtil.write(res,401,JsonUtil.error("UNAUTHORIZED","로그인이 필요합니다."));return;}
        if(req.getPathInfo()!=null && !req.getPathInfo().equals("/")) {JsonUtil.write(res,404,JsonUtil.error("NOT_FOUND","알 수 없는 경로"));return;}
        JSONArray list=new JSONArray();
        try(Connection c=DatabaseConfig.getConnection();PreparedStatement ps=c.prepareStatement(
                "select id,total_price,status,created_at from public.orders where user_id=? order by created_at desc limit 50")) {
            ps.setObject(1,user);
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) list.put(new JSONObject().put("orderId",rs.getString("id"))
                    .put("totalPrice",rs.getInt("total_price")).put("status",rs.getString("status"))
                    .put("createdAt",rs.getTimestamp("created_at").toInstant().toString()));
            }
            JsonUtil.write(res,200,new JSONObject().put("ok",true).put("items",list));
        }catch(SQLException ex) {getServletContext().log("Order list SQLSTATE="+ex.getSQLState());JsonUtil.write(res,503,JsonUtil.error("DATABASE_UNAVAILABLE","DB 연결 오류"));}
    }
}
