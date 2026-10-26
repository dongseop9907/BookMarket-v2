package com.market.page;

import com.market.bookitem.Book;
import com.market.jdbc.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException; 
import java.awt.event.ActionListener; 
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.ActionEvent;
import java.io.FileWriter;

public class AdminPage extends JPanel {
	
	private JTable userTable;
	
	
	private void insertBookToDB(Book book) {
	    try (Connection conn = DBUtil.getConnection()) {
	        String sql = "INSERT INTO books (isbn, title, author, price, stock, publisher) VALUES (?, ?, ?, ?, ?, ?)";
	        PreparedStatement ps = conn.prepareStatement(sql);

	        ps.setString(1, book.getBookId());
	        ps.setString(2, book.getName());
	        ps.setString(3, book.getAuthor());
	        ps.setInt(4, book.getUnitPrice());
	        ps.setInt(5, 10); // 기본 재고
	        ps.setString(6, book.getDescription()); // 출판사 또는 설명

	        ps.executeUpdate();
	        ps.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}


    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return lastIndex > 0 ? name.substring(lastIndex) : "";
    }

    private String imageSavePath = null;
    private JLabel previewLabel;
    private File selectedImageFile = null;

    public AdminPage(JPanel panel) {

        Font ft = new Font("함초롬돋움", Font.BOLD, 15);
        setLayout(null);

        Rectangle rect = panel.getBounds();
        setPreferredSize(rect.getSize());

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMddhhmmss");
        String strDate = formatter.format(date);

        JPanel idPanel = new JPanel();
        idPanel.setBounds(0, 0, 700, 50);
        JLabel idLabel = new JLabel("도서ID : ");
        idLabel.setFont(ft);
        JLabel idTextField = new JLabel();
        idTextField.setFont(ft);
        idTextField.setPreferredSize(new Dimension(290, 50));
        idTextField.setText("ISBN" + strDate);
        idPanel.add(idLabel);
        idPanel.add(idTextField);
        add(idPanel);

        JPanel namePanel = new JPanel();
        namePanel.setBounds(0, 50, 700, 50);
        JLabel nameLabel = new JLabel("도서명 : ");
        nameLabel.setFont(ft);
        JTextField nameTextField = new JTextField(20);
        nameTextField.setFont(ft);
        namePanel.add(nameLabel);
        namePanel.add(nameTextField);
        add(namePanel);

        JPanel pricePanel = new JPanel();
        pricePanel.setBounds(0, 100, 700, 50);
        JLabel priceLabel = new JLabel("가   격 : ");
        priceLabel.setFont(ft);
        JTextField priceTextField = new JTextField(20);
        priceTextField.setFont(ft);
        pricePanel.add(priceLabel);
        pricePanel.add(priceTextField);
        add(pricePanel);

        JPanel authorPanel = new JPanel();
        authorPanel.setBounds(0, 150, 700, 50);
        JLabel authorLabel = new JLabel("저   자 : ");
        authorLabel.setFont(ft);
        JTextField authorTextField = new JTextField(20);
        authorTextField.setFont(ft);
        authorPanel.add(authorLabel);
        authorPanel.add(authorTextField);
        add(authorPanel);

        JPanel descPanel = new JPanel();
        descPanel.setBounds(0, 200, 700, 50);
        JLabel descLabel = new JLabel("설   명 : ");
        descLabel.setFont(ft);
        JTextField descTextField = new JTextField(20);
        descTextField.setFont(ft);
        descPanel.add(descLabel);
        descPanel.add(descTextField);
        add(descPanel);

        JPanel categoryPanel = new JPanel();
        categoryPanel.setBounds(0, 250, 700, 50);
        JLabel categoryLabel = new JLabel("분   야 : ");
        categoryLabel.setFont(ft);
        JTextField categoryTextField = new JTextField(20);
        categoryTextField.setFont(ft);
        categoryPanel.add(categoryLabel);
        categoryPanel.add(categoryTextField);
        add(categoryPanel);

        JPanel datePanel = new JPanel();
        datePanel.setBounds(0, 300, 700, 50);
        JLabel dateLabel = new JLabel("출판일 : ");
        dateLabel.setFont(ft);
        JTextField dateTextField = new JTextField(20);
        dateTextField.setFont(ft);
        datePanel.add(dateLabel);
        datePanel.add(dateTextField);
        add(datePanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBounds(0, 400, 700, 50);
        add(buttonPanel);

        JLabel okLabel = new JLabel("추가");
        okLabel.setFont(ft);
        JButton okButton = new JButton();
        okButton.add(okLabel);
        buttonPanel.add(okButton);
        
        

        okButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String bookId = idTextField.getText();
                String name = nameTextField.getText();
                int price = Integer.parseInt(priceTextField.getText());
                String author = authorTextField.getText();
                String description = descTextField.getText();
                String category = categoryTextField.getText();
                String releaseDate = dateTextField.getText();

                try {
                    // ✅ 이미지 파일 복사
                    if (selectedImageFile != null && imageSavePath != null) {
                        File dest = new File(imageSavePath);
                        dest.getParentFile().mkdirs();
                        Files.copy(selectedImageFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("이미지 저장 완료: " + imageSavePath);
                    }

                    // ✅ Book 객체 생성 및 DB 저장
                    Book book = new Book(bookId, name, price, author, description, category, releaseDate);
                    insertBookToDB(book);

                    JOptionPane.showMessageDialog(okButton, "새 도서 정보가 데이터베이스에 저장되었습니다");

                    // ✅ 입력값 초기화
                    Date date = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyMMddhhmmss");
                    String strDate = formatter.format(date);
                    idTextField.setText("ISBN" + strDate);
                    nameTextField.setText("");
                    priceTextField.setText("");
                    authorTextField.setText("");
                    descTextField.setText("");
                    categoryTextField.setText("");
                    dateTextField.setText("");
                    selectedImageFile = null;
                    imageSavePath = null;
                    previewLabel.setIcon(null);
                    previewLabel.setText("이미지 미리보기");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(okButton, "오류 발생: " + ex.getMessage());
                }
            }
        });


        JLabel noLabel = new JLabel("취소");
        noLabel.setFont(ft);
        JButton noButton = new JButton();
        noButton.add(noLabel);
        buttonPanel.add(noButton);

        noButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                nameTextField.setText("");
                priceTextField.setText("");
                authorTextField.setText("");
                descTextField.setText("");
                categoryTextField.setText("");
                dateTextField.setText("");
            }
        });

        JPanel imgPanel = new JPanel();
        imgPanel.setBounds(0, 350, 700, 50);
        add(imgPanel);

        JLabel imgLabel = new JLabel("이미지 등록");
        imgLabel.setFont(ft);
        JButton imgButton = new JButton();
        imgButton.add(imgLabel);
        imgPanel.add(imgButton);

        previewLabel = new JLabel("이미지 미리보기", SwingConstants.CENTER);
        previewLabel.setBounds(700, 70, 250, 250);
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        add(previewLabel);

        imgButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser imageChooser = new JFileChooser();
                imageChooser.setDialogTitle("이미지를 선택하세요");
                int imageResult = imageChooser.showOpenDialog(null);
                if (imageResult != JFileChooser.APPROVE_OPTION) return;

                selectedImageFile = imageChooser.getSelectedFile();

                ImageIcon icon = new ImageIcon(selectedImageFile.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(img));
                previewLabel.setText("");

                JFileChooser saveChooser = new JFileChooser();
                saveChooser.setDialogTitle("이미지를 저장할 경로를 선택하세요");

                String fileName = idTextField.getText() + getFileExtension(selectedImageFile);
                saveChooser.setSelectedFile(new File(fileName));

                int saveResult = saveChooser.showSaveDialog(null);
                if (saveResult == JFileChooser.APPROVE_OPTION) {
                    File dest = saveChooser.getSelectedFile();
                    imageSavePath = dest.getAbsolutePath();
                    System.out.println("이미지 저장 예정 경로: " + imageSavePath);
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setBounds(0, 0, 1000, 750);
        frame.setLayout(null);

        JPanel mPagePanel = new JPanel();
        mPagePanel.setBounds(0, 150, 1000, 750);

        frame.add(mPagePanel);
        mPagePanel.add(new AdminPage(mPagePanel));
        frame.setVisible(true);
    }
}