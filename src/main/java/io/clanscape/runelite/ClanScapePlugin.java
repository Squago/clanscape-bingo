package io.clanscape.runelite;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** No general location, chat, inventory, combat, or session telemetry is collected. */
@Slf4j
@PluginDescriptor(name = "ClanScape Bingo", description = "Opt-in, board-only candidate tracking", tags = {"clanscape", "bingo", "loot", "collection"})
public class ClanScapePlugin extends Plugin
{
	private static final String GROUP = "clanscape-bingo";
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final Pattern LINK_CODE = Pattern.compile("^(\\d{17,20})\\.[A-Z2-7]{10}$");
	private static final Pattern COLLECTION_LOG = Pattern.compile("New item added to your collection log: (.*)");
	@Inject private Client client;
	@Inject private ClanScapeConfig config;
	@Inject private ConfigManager configManager;
	@Inject private OkHttpClient okHttpClient;
	@Inject private Gson gson;
	@Inject private ClientToolbar clientToolbar;
	private final List<LootRule> lootRules = new ArrayList<>();
	private final Set<String> collectionEntries = new HashSet<>();
	private long lastRefresh;
	private ClanScapeBoardPanel boardPanel;
	private NavigationButton navigationButton;
	private final AtomicBoolean linkInFlight = new AtomicBoolean();
	private final AtomicBoolean refreshInFlight = new AtomicBoolean();

	@Provides ClanScapeConfig provideConfig(ConfigManager manager) { return manager.getConfig(ClanScapeConfig.class); }
	@Override protected void startUp()
	{
		boardPanel = new ClanScapeBoardPanel(this::beginLink, this::resetLink, this::refreshBoard);
		navigationButton = NavigationButton.builder().tooltip("ClanScape board").icon(icon()).panel(boardPanel).priority(6).build();
		clientToolbar.addNavigation(navigationButton);
		refreshConfiguration();
	}
	@Override protected void shutDown()
	{
		if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
		boardPanel = null;
	}
	@Subscribe public void onGameStateChanged(GameStateChanged event) { refreshConfiguration(); }
	@Subscribe public void onGameTick(GameTick event) { refreshConfiguration(); }
	@Subscribe public void onConfigChanged(ConfigChanged event) { if (GROUP.equals(event.getGroup())) { lastRefresh = 0; refreshConfiguration(); } }

	@Subscribe public void onNpcLootReceived(NpcLootReceived event)
	{
		if (!ready() || event.getNpc() == null) return;
		refreshConfiguration();
		for (ItemStack item : event.getItems()) if (matchesLoot(item.getId(), event.getNpc().getId())) sendLoot(item.getId(), item.getQuantity(), event.getNpc().getId());
	}

	@Subscribe public void onChatMessage(ChatMessage event)
	{
		if (!ready() || (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)) return;
		Matcher match = COLLECTION_LOG.matcher(event.getMessage());
		if (match.find() && collectionEntries.contains(normalize(match.group(1)))) sendCollectionLog(normalize(match.group(1)));
	}

	private void beginLink()
	{
		if (!linkInFlight.compareAndSet(false, true)) return;
		if (!config.telemetryEnabled()) { linkFailed("Turn on board-only tracking in plugin settings first."); return; }
		if (!config.installationId().isEmpty()) { linkInFlight.set(false); showLinked(client.getLocalPlayer() == null ? "your RuneScape account" : client.getLocalPlayer().getName()); return; }
		if (config.linkCode().isEmpty()) { linkFailed("Paste a fresh /bingo link code into plugin settings, then click this button."); return; }
		if (client.getLocalPlayer() == null) { linkFailed("Log into RuneLite before linking your account."); return; }
		if (config.serverUrl().trim().isEmpty()) { linkFailed("Enter the ClanScape service URL in plugin settings first."); return; }
		Matcher code = LINK_CODE.matcher(config.linkCode().trim().toUpperCase(Locale.ROOT));
		if (!code.matches()) { linkFailed("That link code is not valid. Run /bingo link again and paste the new code."); return; }
		showLinking();
		String accountName = client.getLocalPlayer().getName();
		JsonObject body = new JsonObject(); body.addProperty("linkCode", config.linkCode().trim().toUpperCase(Locale.ROOT)); body.addProperty("accountName", accountName);
		post("/v1/runelite/activate", body, new Callback() {
			@Override public void onFailure(Call call, IOException error) { log.debug("ClanScape link failed", error); linkFailed("ClanScape could not be reached. Check the service URL and try again."); }
			@Override public void onResponse(Call call, Response response) throws IOException { try (Response ignored = response) {
				if (!response.isSuccessful() || response.body() == null) { linkFailed("ClanScape did not accept that code. Create a fresh /bingo link code and try again."); return; }
				try {
					JsonObject result = gson.fromJson(response.body().charStream(), JsonObject.class);
					configManager.setConfiguration(GROUP, "installationId", result.get("installationId").getAsString());
					configManager.setConfiguration(GROUP, "installationToken", result.get("token").getAsString());
					configManager.setConfiguration(GROUP, "guildId", code.group(1));
					configManager.setConfiguration(GROUP, "linkCode", "");
					linkInFlight.set(false);
					showLinked(accountName);
					lastRefresh = 0;
					refreshConfiguration();
				} catch (RuntimeException error) {
					log.debug("ClanScape link response was invalid", error);
					linkFailed("ClanScape returned an invalid link response. Try again shortly.");
				}
			} }
		});
	}

	private void resetLink()
	{
		linkInFlight.set(false);
		configManager.setConfiguration(GROUP, "installationId", "");
		configManager.setConfiguration(GROUP, "installationToken", "");
		configManager.setConfiguration(GROUP, "guildId", "");
		configManager.setConfiguration(GROUP, "linkCode", "");
		lootRules.clear();
		collectionEntries.clear();
		lastRefresh = 0;
		if (boardPanel != null) SwingUtilities.invokeLater(boardPanel::showWaitingForLink);
	}

	private void refreshBoard()
	{
		if (!ready()) return;
		if (boardPanel != null) SwingUtilities.invokeLater(boardPanel::showRefreshing);
		refreshConfiguration(true);
	}

	private void linkFailed(String message)
	{
		linkInFlight.set(false);
		if (boardPanel != null) SwingUtilities.invokeLater(() -> boardPanel.showLinkError(message));
	}

	private void showLinking()
	{
		if (boardPanel != null) SwingUtilities.invokeLater(boardPanel::showLinking);
	}

	private void showLinked(String accountName)
	{
		if (boardPanel != null) SwingUtilities.invokeLater(() -> boardPanel.showLinked(accountName));
	}

	private void refreshConfiguration()
	{
		refreshConfiguration(false);
	}

	private void refreshConfiguration(boolean force)
	{
		if (!ready() || (!force && System.currentTimeMillis() - lastRefresh < TimeUnit.MINUTES.toMillis(1))) return;
		if (!refreshInFlight.compareAndSet(false, true)) return;
		lastRefresh = System.currentTimeMillis();
		okHttpClient.newCall(authenticated("/v1/runelite/configuration").get().build()).enqueue(new Callback() {
			@Override public void onFailure(Call call, IOException error) { log.debug("ClanScape configuration refresh failed", error); finishRefresh(); }
			@Override public void onResponse(Call call, Response response) throws IOException { try (Response ignored = response) {
				if (!response.isSuccessful() || response.body() == null) return; applyConfiguration(gson.fromJson(response.body().charStream(), JsonObject.class));
			} finally { finishRefresh(); } }
		});
	}

	private void finishRefresh()
	{
		refreshInFlight.set(false);
		if (boardPanel != null) SwingUtilities.invokeLater(boardPanel::finishRefresh);
	}

	private synchronized void applyConfiguration(JsonObject configuration)
	{
		lootRules.clear(); collectionEntries.clear();
		String accountName = configuration.has("accountName") ? configuration.get("accountName").getAsString() : "linked account";
		if (!configuration.has("active") || !configuration.get("active").getAsBoolean())
		{
			showInactive(accountName);
			return;
		}
		for (JsonElement element : configuration.getAsJsonArray("rules")) {
			JsonObject rule = element.getAsJsonObject().getAsJsonObject("rule");
			if ("LOOT".equals(rule.get("kind").getAsString())) { Set<Integer> items = new HashSet<>(), npcs = new HashSet<>(); for (JsonElement id : rule.getAsJsonArray("itemIds")) items.add(id.getAsInt()); if (rule.has("sourceNpcIds")) for (JsonElement id : rule.getAsJsonArray("sourceNpcIds")) npcs.add(id.getAsInt()); lootRules.add(new LootRule(items, npcs)); }
			else if ("COLLECTION_LOG".equals(rule.get("kind").getAsString())) for (JsonElement entry : rule.getAsJsonArray("entries")) collectionEntries.add(normalize(entry.getAsString()));
		}
		if (configuration.has("board") && !configuration.get("board").isJsonNull()) showBoard(accountName, configuration.getAsJsonObject("board"));
	}

	private void showInactive(String accountName)
	{
		if (boardPanel == null) return;
		SwingUtilities.invokeLater(() -> boardPanel.showInactive(accountName));
	}

	private void showBoard(String accountName, JsonObject board)
	{
		if (boardPanel == null) return;
		List<ClanScapeBoardPanel.ClanScapeTile> tiles = new ArrayList<>();
		for (JsonElement element : board.getAsJsonArray("tiles"))
		{
			JsonObject tile = element.getAsJsonObject();
			tiles.add(new ClanScapeBoardPanel.ClanScapeTile(tile.get("cellIndex").getAsInt(), tile.get("title").getAsString(), tile.get("criteria").getAsString(), tile.get("targetCount").getAsInt(), tile.get("progress").getAsInt(), tile.get("completed").getAsBoolean()));
		}
		List<String> completedLines = new ArrayList<>();
		for (JsonElement line : board.getAsJsonArray("completedLines")) completedLines.add(line.getAsString());
		ClanScapeBoardPanel.ClanScapeBoard view = new ClanScapeBoardPanel.ClanScapeBoard(board.get("name").getAsString(), tiles, completedLines, board.get("blackout").getAsBoolean());
		SwingUtilities.invokeLater(() -> boardPanel.showBoard(accountName, view));
	}

	private boolean matchesLoot(int itemId, int npcId) { for (LootRule rule : lootRules) if (rule.items.contains(itemId) && (rule.npcs.isEmpty() || rule.npcs.contains(npcId))) return true; return false; }
	private void sendLoot(int itemId, int quantity, int npcId) { JsonObject body = event("LOOT"); body.addProperty("itemId", itemId); body.addProperty("quantity", quantity); body.addProperty("sourceNpcId", npcId); send(body); }
	private void sendCollectionLog(String entry) { JsonObject body = event("COLLECTION_LOG"); body.addProperty("collectionLogEntry", entry); send(body); }
	private JsonObject event(String kind) { JsonObject body = new JsonObject(); body.addProperty("eventId", UUID.randomUUID().toString()); body.addProperty("kind", kind); body.addProperty("occurredAt", Instant.now().toString()); return body; }
	private void send(JsonObject body) { okHttpClient.newCall(authenticated("/v1/runelite/events").post(RequestBody.create(JSON, gson.toJson(body))).build()).enqueue(new Callback() { @Override public void onFailure(Call call, IOException error) { log.debug("ClanScape event failed", error); } @Override public void onResponse(Call call, Response response) { response.close(); } }); }
	private void post(String path, JsonObject body, Callback callback) { okHttpClient.newCall(new Request.Builder().url(baseUrl() + path).post(RequestBody.create(JSON, gson.toJson(body))).build()).enqueue(callback); }
	private Request.Builder authenticated(String path) { return new Request.Builder().url(baseUrl() + path).header("Authorization", "Bearer " + config.installationToken()).header("X-ClanScape-Guild-Id", config.guildId()).header("X-ClanScape-Installation-Id", config.installationId()); }
	private boolean ready() { return config.telemetryEnabled() && !config.serverUrl().trim().isEmpty() && !config.installationId().isEmpty() && !config.installationToken().isEmpty() && !config.guildId().isEmpty(); }
	private String baseUrl() { return config.serverUrl().trim().replaceAll("/+$", ""); }
	private static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(87, 160, 119));
		graphics.fillRoundRect(1, 1, 14, 14, 3, 3);
		graphics.setColor(Color.WHITE);
		for (int index = 1; index < 5; index++) { int point = index * 3 + 1; graphics.drawLine(point, 2, point, 14); graphics.drawLine(2, point, 14, point); }
		graphics.dispose();
		return image;
	}
	private static String normalize(String value) { return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
	private static final class LootRule { private final Set<Integer> items, npcs; private LootRule(Set<Integer> items, Set<Integer> npcs) { this.items = items; this.npcs = npcs; } }
}
