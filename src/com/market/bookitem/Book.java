package com.market.bookitem;

public class Book extends Item {
    private int stock = -1;
    private String thumbnailUrl = "";
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String url) { this.thumbnailUrl = url == null ? "" : url; }
    private String author;
    private String description;  // 썸네일 이미지 URL 또는 출판사 정보
    private String category;     // 분야
    private String releaseDate;  // 출판일

    // 기본 생성자
    public Book(String bookId, String name, int unitPrice) {
        super(bookId, name, unitPrice);
    }

    // 전체 필드 초기화용 생성자 (API용)
    public Book(String bookId, String name, int unitPrice, String author,
                String description, String category, String releaseDate) {
        super(bookId, name, unitPrice);
        this.author = author;
        this.description = description;
        this.category = category;
        this.releaseDate = releaseDate;
    }

    // JDBC DB 저장용 생성자
    public Book(String isbn, String title, String author,
                int price, int stock, String publisher, String category, String releaseDate) {
        super(isbn, title, price);
        this.author = author;
        this.description = publisher; // 출판사 정보를 description에 임시 저장
        this.category = category;
        this.releaseDate = releaseDate;
    }

    // Getter/Setter
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
}
