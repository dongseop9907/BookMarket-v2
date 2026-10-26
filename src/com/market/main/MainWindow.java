package com.market.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.market.page.GuestInfoPage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.market.cart.Cart;
import com.market.bookitem.BookInIt;
import com.market.page.CartAddItemPage;
import com.market.page.CartItemListPage;
import com.market.page.CartShippingPage;
import com.market.page.AdminLoginDialog;
import com.market.page.AdminPage;
import com.market.api.AuthApi;
import com.market.session.SessionContext;

public class MainWindow extends JFrame {
	static Cart mCart;
	static JPanel mMenuPanel, mPagePanel;

	public MainWindow(String title, int x, int y, int width, int height) {

		initContainer(title, x, y, width, height);
		initMenu();

		setVisible(true);
		setResizable(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(new ImageIcon("./images/shop.png").getImage());
	}

	private void initContainer(String title, int x, int y, int width, int height) {
		setTitle(title);
		setBounds(x, y, width, height);
		setLayout(null);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - 1000) / 2, (screenSize.height - 750) / 2);

		mMenuPanel = new JPanel();
		mMenuPanel.setBounds(0, 20, width, 130);
		menuIntroduction();
        applyModernNavigation();
		add(mMenuPanel);

		mPagePanel = new JPanel();
		mPagePanel.setBounds(0, 150, width, height);
		add(mPagePanel);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				logoutSilently();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				setVisible(false);
				new GuestWindow("BookMarket 로그인", 0, 0, 1000, 750);
			}
		});
	}

	private void menuIntroduction() {
		mCart = new Cart();
		Font ft;
		ft = new Font("함초롬돋움", Font.BOLD, 15);

		JButton bt1 = new JButton("고객 정보 확인하기", new ImageIcon("./images/1.png"));
		bt1.setBounds(0, 0, 100, 50);
		bt1.setFont(ft);
		mMenuPanel.add(bt1);

		bt1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mPagePanel.removeAll(); // 패널(mPagePanel)에 표시된 구성 요소 모두 삭제

				mPagePanel.add("고객 정보 확인", new GuestInfoPage(mPagePanel)); // 패널(mPagePanel)에 GuestInfoPage의 내용 출력
				mPagePanel.revalidate(); // 구성 요소 가로/세로 속성 변경하여 호출
				mPagePanel.repaint(); // 구성요소 모양을 변경하여 호출
			}
		});

		JButton bt2 = new JButton("장바구니 상품목록보기", new ImageIcon("./images/2.png"));
		bt2.setBounds(0, 0, 100, 30);
		bt2.setFont(ft);
		mMenuPanel.add(bt2);

		bt2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (mCart.mCartCount == 0)
					JOptionPane.showMessageDialog(bt2, "장바구니에 항목이 없습니다", "장바구니 상품 목록 보기", JOptionPane.ERROR_MESSAGE);
				else {
					mPagePanel.removeAll();
					mPagePanel.add("장바구니 상품 목록 보기", new CartItemListPage(mPagePanel, mCart));
					mPagePanel.revalidate();
					mPagePanel.repaint();

				}
			}
		});

		JButton bt3 = new JButton("장바구니 비우기", new ImageIcon("./images/3.png"));
		bt3.setBounds(0, 0, 100, 30);
		bt3.setFont(ft);
		mMenuPanel.add(bt3);

		bt3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (mCart.mCartCount == 0)
					JOptionPane.showMessageDialog(bt3, "장바구니에 항목이 없습니다", "장바구니 비우기", JOptionPane.ERROR_MESSAGE);
				else {
					mPagePanel.removeAll();
					menuCartClear(bt3);
					mPagePanel.add("장바구니 비우기", new CartItemListPage(mPagePanel, mCart));
					mPagePanel.revalidate();
					mPagePanel.repaint();
				}
			}
		});

		JButton bt4 = new JButton("장바구니에 항목추가하기", new ImageIcon("./images/4.png"));
		bt4.setFont(ft);
		mMenuPanel.add(bt4);
		bt4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				mPagePanel.removeAll();
				mPagePanel.add("장바구니에 항목 추가하기", new CartAddItemPage(mPagePanel, mCart));
				mPagePanel.revalidate();
				mPagePanel.repaint();
			}
		});

		JButton bt5 = new JButton("장바구니에 항목수량 줄이기", new ImageIcon("./images/5.png"));
		bt5.setFont(ft);
		mMenuPanel.add(bt5);

		JButton bt6 = new JButton("장바구니에 항목삭제하기", new ImageIcon("./images/6.png"));
		bt6.setFont(ft);
		mMenuPanel.add(bt6);

		bt6.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (mCart.mCartCount == 0)
					JOptionPane.showMessageDialog(bt3, "장바구니에 항목이 없습니다", "장바구니 비우기", JOptionPane.ERROR_MESSAGE);
				else {

					mPagePanel.removeAll();
					CartItemListPage cartList = new CartItemListPage(mPagePanel, mCart);
					if (mCart.mCartCount == 0)
						JOptionPane.showMessageDialog(bt6, "장바구니에 항목이 없습니다");
					else if (cartList.mSelectRow == -1)
						JOptionPane.showMessageDialog(bt6, "장바구니에서 삭제할 항목을 선택하세요");
					else {
						mCart.removeCart(cartList.mSelectRow);
// 장바구니에서 선택 항목 삭제하기		
						cartList.mSelectRow = -1;
					}
				}
				mPagePanel.add("장바구니의 항목 삭제하기", new CartItemListPage(mPagePanel, mCart));

				mPagePanel.revalidate();
				mPagePanel.repaint();
			}
		});

		JButton bt7 = new JButton("주문하기", new ImageIcon("./images/7.png"));
		bt7.setFont(ft);
		mMenuPanel.add(bt7);

		bt7.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (mCart.mCartCount == 0)
					JOptionPane.showMessageDialog(bt7, "장바구니에 항목이 없습니다", "주문처리", JOptionPane.ERROR_MESSAGE);
				else {

					mPagePanel.removeAll();
					mPagePanel.add("주문 배송지", new CartShippingPage(mPagePanel, mCart));
					mPagePanel.revalidate();
					mPagePanel.repaint();
				}
			}
		});

		JButton bt8 = new JButton("종료", new ImageIcon("./images/8.png"));
		bt8.setFont(ft);
		mMenuPanel.add(bt8);

		bt8.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				int select = JOptionPane.showConfirmDialog(bt8, "쇼핑몰을 종료하겠습니까? ");

				if (select == 0) {
					logoutSilently();
					System.exit(0);
				}
			}
		});

		JButton bt9 = new JButton("관리자", new ImageIcon("./images/9.png"));
		bt9.setFont(ft);
		mMenuPanel.add(bt9);

        bt9.addActionListener(e -> {
            if (!com.market.session.SessionContext.isLoggedIn()) {
                JOptionPane.showMessageDialog(MainWindow.this, "Please log in first.");
                return;
            }
            if (!com.market.session.SessionContext.isAdmin()) {
                JOptionPane.showMessageDialog(MainWindow.this, "Administrator permission required.");
                return;
            }
            mPagePanel.removeAll();
            mPagePanel.add("Inventory", new com.market.page.AdminStockPage());
            mPagePanel.revalidate();
            mPagePanel.repaint();
        });

        JButton orderHistoryButton = new JButton("주문 내역 / 취소");
        orderHistoryButton.setFont(ft);
        mMenuPanel.add(orderHistoryButton);
        orderHistoryButton.addActionListener(e -> {
            if (!com.market.session.SessionContext.isLoggedIn()) {
                JOptionPane.showMessageDialog(MainWindow.this, "로그인 후 이용할 수 있습니다.");
                return;
            }
            mPagePanel.removeAll();
            mPagePanel.add("OrderHistory", new com.market.page.OrderHistoryPage());
            mPagePanel.revalidate();
            mPagePanel.repaint();
        });
	}

	
    private void applyModernNavigation() {
        mMenuPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 9));
        mMenuPanel.setBackground(new Color(249, 249, 253));
        String[] labels = {
            "\uB0B4 \uC815\uBCF4", "\uC7A5\uBC14\uAD6C\uB2C8", "\uC7A5\uBC14\uAD6C\uB2C8 \uBE44\uC6B0\uAE30",
            "\uB3C4\uC11C \uC1FC\uD551", "\uC218\uB7C9 \uC904\uC774\uAE30", "\uC120\uD0DD \uC0AD\uC81C",
            "\uC8FC\uBB38\uD558\uAE30", "\uC885\uB8CC", "\uAD00\uB9AC\uC790", "\uC8FC\uBB38 \uB0B4\uC5ED"
        };
        int index = 0;
        for (Component component : mMenuPanel.getComponents()) {
            if (!(component instanceof JButton)) continue;
            JButton button = (JButton) component;
            if (index < labels.length) button.setText(labels[index]);
            button.setIcon(null);
            button.setFont(new Font("Dialog", Font.BOLD, 12));
            button.setForeground(new Color(43, 47, 67));
            button.setBackground(Color.WHITE);
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 222, 235), 1, true),
                    BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            button.setPreferredSize(new Dimension(index == 2 ? 142 : 116, 38));
            index++;
        }
    }

    private void initMenu() {
		Font ft;
		ft = new Font("함초롬돋움", Font.BOLD, 15);

		JMenuBar menuBar = new JMenuBar();

		JMenu menu01 = new JMenu("고객");
		menu01.setFont(ft);
		JMenuItem item01 = new JMenuItem("고객 정보");
		JMenuItem item11 = new JMenuItem("종료");
		menu01.add(item01);
		menu01.add(item11);
		menuBar.add(menu01);

		JMenu menu02 = new JMenu("상품");
		menu02.setFont(ft);
		JMenuItem item02 = new JMenuItem("상품 목록");
		menu02.add(item02);
		menuBar.add(menu02);

		JMenu menu03 = new JMenu("장바구니");
		menu03.setFont(ft);
		JMenuItem item03 = new JMenuItem("항목 추가");
		JMenuItem item04 = new JMenuItem("항목 수량 줄이기");
		JMenuItem item05 = new JMenuItem("항목 삭제하기");
		JMenuItem item06 = new JMenuItem("장바구니 비우기");
		menu03.add(item03);
		menu03.add(item04);
		menu03.add(item05);
		menu03.add(item06);
		menuBar.add(menu03);

		JMenu menu04 = new JMenu("주문");
		menu04.setFont(ft);
		JMenuItem item07 = new JMenuItem("영수증 표시");
		menu04.add(item07);
		menuBar.add(menu04);
		setJMenuBar(menuBar);

		item01.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mPagePanel.removeAll();
				mPagePanel.add("고객 정보 확인 ", new GuestInfoPage(mPagePanel));
				add(mPagePanel);
				mPagePanel.revalidate();
			}
		});

		item02.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mPagePanel.removeAll();
				mPagePanel.add("장바구니에 항목 추가하기", new CartAddItemPage(mPagePanel, mCart));
				add(mPagePanel);
				mPagePanel.revalidate();
			}
		});

		item11.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				logoutSilently();
				dispose();
			}
		});
	}

	private void menuCartClear(JButton button) {

		if (mCart.mCartCount == 0)
			JOptionPane.showMessageDialog(button, "장바구니의 항목이 없습니다");
		else {

			int select = JOptionPane.showConfirmDialog(button, "장바구니의 모든 항목을 삭제하겠습니까? ");

			if (select == 0) {
				mCart.deleteBook();
				JOptionPane.showMessageDialog(button, "장바구니의 모든 항목을 삭제했습니다");
			}
		}
	}
	private void logoutSilently() {
		try {
			if (SessionContext.isLoggedIn()) {
				AuthApi.logout();
			}
		} catch (Exception ignored) {
		} finally {
			SessionContext.clear();
		}
	}

}