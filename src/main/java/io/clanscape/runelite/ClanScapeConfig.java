package io.clanscape.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("clanscape-bingo")
public interface ClanScapeConfig extends Config
{
	@ConfigItem(keyName = "serverUrl", name = "ClanScape service URL", description = "Official ClanScape service. Change only for a trusted development server.")
	default String serverUrl() { return "https://clanscape-production.up.railway.app"; }
	@ConfigItem(keyName = "linkCode", name = "One-time link code", description = "Run /bingo link in Discord and paste the temporary code here.")
	default String linkCode() { return ""; }
	@ConfigItem(keyName = "installationId", name = "Installation ID", description = "Managed automatically.", secret = true)
	default String installationId() { return ""; }
	@ConfigItem(keyName = "installationToken", name = "Installation token", description = "Managed automatically.", secret = true)
	default String installationToken() { return ""; }
	@ConfigItem(keyName = "guildId", name = "Server ID", description = "Managed automatically.", secret = true)
	default String guildId() { return ""; }
	@ConfigItem(keyName = "telemetryEnabled", name = "Enable board-only tracking", description = "Send only live-board loot and collection-log matches for organizer review.", warning = "This feature submits matched board activity and your IP address to a 3rd-party server not controlled or verified by RuneLite developers")
	default boolean telemetryEnabled() { return false; }
}
