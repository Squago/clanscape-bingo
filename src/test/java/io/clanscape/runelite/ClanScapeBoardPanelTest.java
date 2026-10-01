package io.clanscape.runelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import org.junit.Test;

public class ClanScapeBoardPanelTest
{
	@Test
	public void linkButtonRequestsAnExplicitLinkAndShowsRetryState()
	{
		AtomicInteger requests = new AtomicInteger();
		AtomicInteger resets = new AtomicInteger();
		AtomicInteger refreshes = new AtomicInteger();
		ClanScapeBoardPanel panel = new ClanScapeBoardPanel(requests::incrementAndGet, resets::incrementAndGet, refreshes::incrementAndGet);
		JButton button = findButton(panel, "Link RuneLite");
		assertNotNull(button);
		button.doClick();
		assertEquals(1, requests.get());

		panel.showLinking();
		assertFalse(button.isEnabled());
		panel.showLinkError("Try again.");
		assertTrue(button.isEnabled());

		panel.showLinked("Squag");
		JButton resetButton = findButton(panel, "Switch server / relink");
		assertNotNull(resetButton);
		assertTrue(resetButton.isVisible());
		resetButton.doClick();
		assertEquals(1, resets.get());

		JButton refreshButton = findButton(panel, "Refresh board");
		assertNotNull(refreshButton);
		refreshButton.doClick();
		assertEquals(1, refreshes.get());
	}

	private static JButton findButton(Container parent, String text)
	{
		for (Component component : parent.getComponents())
		{
			if (component instanceof JButton && text.equals(((JButton) component).getText())) return (JButton) component;
			if (component instanceof Container)
			{
				JButton nested = findButton((Container) component, text);
				if (nested != null) return nested;
			}
		}
		return null;
	}
}
