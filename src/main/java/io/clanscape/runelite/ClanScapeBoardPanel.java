package io.clanscape.runelite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.PluginPanel;

/** Read-only view of the linked player's server-authoritative board. */
final class ClanScapeBoardPanel extends PluginPanel
{
	private static final Color DARK_WOOD = new Color(55, 34, 18);
	private static final Color WOOD = new Color(93, 59, 30);
	private static final Color PARCHMENT = new Color(229, 207, 157);
	private static final Color PARCHMENT_LIGHT = new Color(245, 232, 197);
	private static final Color INK = new Color(62, 40, 20);
	private static final Color GOLD = new Color(182, 133, 48);
	private static final Color COMPLETE = new Color(75, 103, 59);
	private static final Color ACTIVE = new Color(198, 143, 43);
	private static final Color IDLE = new Color(221, 194, 137);
	private final JLabel heading = new JLabel("ClanScape Bingo");
	private final JLabel summary = new JLabel("Link your Discord account to load a board.");
	private final JLabel detail = new JLabel("Your organizer approves all progress.");
	private final JButton linkButton = new JButton("Link RuneLite");
	private final JButton resetButton = new JButton("Switch server / relink");
	private final JButton refreshButton = new JButton("Refresh board");
	private final JPanel grid = new JPanel(new GridLayout(5, 5, 4, 4));

	ClanScapeBoardPanel(Runnable linkAction, Runnable resetAction, Runnable refreshAction)
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DARK_WOOD, 3), BorderFactory.createEmptyBorder(8, 7, 8, 7)));
		setBackground(DARK_WOOD);
		heading.setFont(new Font(Font.SERIF, Font.BOLD, 18));
		heading.setForeground(INK);
		summary.setFont(new Font(Font.SERIF, Font.PLAIN, 12));
		summary.setForeground(INK);
		JPanel header = new JPanel(new GridLayout(0, 1, 0, 3));
		header.setBackground(PARCHMENT);
		header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(WOOD, 2), BorderFactory.createEmptyBorder(7, 7, 7, 7)));
		header.add(heading);
		header.add(summary);
		linkButton.setFont(new Font(Font.SERIF, Font.BOLD, 12));
		linkButton.setForeground(INK);
		linkButton.setBackground(GOLD);
		linkButton.setFocusPainted(false);
		linkButton.setBorder(BorderFactory.createLineBorder(DARK_WOOD, 1));
		linkButton.addActionListener(event -> linkAction.run());
		header.add(linkButton);
		resetButton.setFont(new Font(Font.SERIF, Font.BOLD, 11));
		resetButton.setForeground(INK);
		resetButton.setBackground(PARCHMENT_LIGHT);
		resetButton.setFocusPainted(false);
		resetButton.setBorder(BorderFactory.createLineBorder(DARK_WOOD, 1));
		resetButton.addActionListener(event -> resetAction.run());
		resetButton.setVisible(false);
		header.add(resetButton);
		refreshButton.setFont(new Font(Font.SERIF, Font.BOLD, 11));
		refreshButton.setForeground(INK);
		refreshButton.setBackground(PARCHMENT_LIGHT);
		refreshButton.setFocusPainted(false);
		refreshButton.setBorder(BorderFactory.createLineBorder(DARK_WOOD, 1));
		refreshButton.addActionListener(event -> refreshAction.run());
		refreshButton.setVisible(false);
		header.add(refreshButton);
		add(header, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(grid);
		scroll.setBorder(BorderFactory.createLineBorder(WOOD, 2));
		scroll.getViewport().setBackground(DARK_WOOD);
		grid.setBackground(DARK_WOOD);
		grid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(scroll, BorderLayout.CENTER);
		detail.setVerticalAlignment(JLabel.TOP);
		detail.setFont(new Font(Font.SERIF, Font.PLAIN, 12));
		detail.setForeground(INK);
		detail.setOpaque(true);
		detail.setBackground(PARCHMENT_LIGHT);
		detail.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(WOOD, 1), BorderFactory.createEmptyBorder(6, 6, 6, 6)));
		add(detail, BorderLayout.SOUTH);
	}

	void showWaitingForLink()
	{
		heading.setText("ClanScape Bingo");
		summary.setText("Link your Discord account to load a board.");
		detail.setText("Run /bingo link in Discord, then paste the code into plugin settings.");
		grid.removeAll();
		grid.revalidate();
		grid.repaint();
		linkButton.setText("Link RuneLite");
		linkButton.setEnabled(true);
		resetButton.setVisible(false);
		refreshButton.setVisible(false);
	}

	void showLinking()
	{
		heading.setText("ClanScape Bingo");
		summary.setText("Linking your RuneScape account…");
		detail.setText("Keep RuneLite open while ClanScape verifies the one-time code.");
		linkButton.setText("Linking…");
		linkButton.setEnabled(false);
	}

	void showLinkError(String message)
	{
		heading.setText("ClanScape Bingo");
		summary.setText("RuneLite could not be linked.");
		detail.setText(message);
		linkButton.setText("Link RuneLite");
		linkButton.setEnabled(true);
		resetButton.setVisible(false);
		refreshButton.setVisible(false);
	}

	void showLinked(String accountName)
	{
		heading.setText("ClanScape Bingo");
		summary.setText("Linked as " + accountName + ".");
		detail.setText("Waiting for the organizer to start the event.");
		linkButton.setText("RuneLite linked");
		linkButton.setEnabled(false);
		resetButton.setVisible(true);
		refreshButton.setVisible(true);
	}

	void showInactive(String accountName)
	{
		heading.setText("ClanScape Bingo");
		summary.setText("No live board for " + accountName + ".");
		detail.setText("The panel refreshes automatically when an organizer starts your event.");
		grid.removeAll();
		grid.revalidate();
		grid.repaint();
		linkButton.setText("RuneLite linked");
		linkButton.setEnabled(false);
		resetButton.setVisible(true);
		refreshButton.setVisible(true);
	}

	void showBoard(String accountName, ClanScapeBoard board)
	{
		heading.setText(board.name);
		long completed = board.tiles.stream().filter(tile -> tile.completed).count();
		summary.setText(accountName + "  |  " + completed + "/25 tiles  |  " + board.completedLines.size() + " bingo(s)");
		detail.setText(board.blackout ? "Blackout complete. Progress is organizer-confirmed." : "Progress is organizer-confirmed. Click a tile for its rule.");
		grid.removeAll();
		board.tiles.stream().sorted(Comparator.comparingInt(tile -> tile.cellIndex)).forEach(this::addTile);
		grid.revalidate();
		grid.repaint();
		linkButton.setText("RuneLite linked");
		linkButton.setEnabled(false);
		resetButton.setVisible(true);
		refreshButton.setVisible(true);
	}

	void showRefreshing()
	{
		refreshButton.setText("Refreshing…");
		refreshButton.setEnabled(false);
	}

	void finishRefresh()
	{
		refreshButton.setText("Refresh board");
		refreshButton.setEnabled(true);
	}

	private void addTile(ClanScapeTile tile)
	{
		String progress = tile.progress + "/" + tile.targetCount;
		String state = tile.completed ? "✓ COMPLETE" : tile.progress > 0 ? "IN PROGRESS" : "TILE " + (tile.cellIndex + 1);
		String label = "<html><center><b>" + escape(state) + "</b><br>" + escape(tile.title) + "<br><font size='-2'>" + progress + " confirmed</font></center></html>";
		JButton button = new JButton(label);
		button.setToolTipText("<html><b>" + escape(tile.title) + "</b><br>" + escape(tile.criteria) + "<br>" + progress + " confirmed by organizer</html>");
		button.setFont(new Font(Font.SERIF, Font.PLAIN, 11));
		button.setForeground(tile.completed ? PARCHMENT_LIGHT : INK);
		button.setBackground(tile.completed ? COMPLETE : tile.progress > 0 ? ACTIVE : IDLE);
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(tile.completed ? PARCHMENT_LIGHT : WOOD, 1), BorderFactory.createEmptyBorder(3, 2, 3, 2)));
		button.addActionListener(event -> detail.setText("Tile " + (tile.cellIndex + 1) + ": " + tile.criteria + "  |  " + progress + " organizer-confirmed"));
		grid.add(button);
	}

	private static String escape(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	static final class ClanScapeBoard
	{
		final String name;
		final List<ClanScapeTile> tiles;
		final List<String> completedLines;
		final boolean blackout;
		ClanScapeBoard(String name, List<ClanScapeTile> tiles, List<String> completedLines, boolean blackout)
		{
			this.name = name;
			this.tiles = tiles;
			this.completedLines = completedLines;
			this.blackout = blackout;
		}
	}

	static final class ClanScapeTile
	{
		final int cellIndex;
		final String title;
		final String criteria;
		final int targetCount;
		final int progress;
		final boolean completed;
		ClanScapeTile(int cellIndex, String title, String criteria, int targetCount, int progress, boolean completed)
		{
			this.cellIndex = cellIndex;
			this.title = title;
			this.criteria = criteria;
			this.targetCount = targetCount;
			this.progress = progress;
			this.completed = completed;
		}
	}
}
