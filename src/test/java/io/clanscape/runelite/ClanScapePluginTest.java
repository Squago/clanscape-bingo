package io.clanscape.runelite;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches a RuneLite development client with the ClanScape companion loaded. */
public class ClanScapePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanScapePlugin.class);
		RuneLite.main(args);
	}
}
