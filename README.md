# ClanScape Bingo RuneLite Plugin

The companion is opt-in. It displays the linked player's server-authoritative 5x5 board in the RuneLite sidebar and sends only loot IDs, NPC IDs, and collection-log names declared by a live board's `telemetryRule`; it does not send general loot, location, inventory, chat, combat, or session information. The ClanScape service repeats the filter and creates an organizer-review candidate only. The sidebar never advances a board automatically: its progress changes only after organizer review.

After Plugin Hub publication, players install it by searching **ClanScape Bingo** in RuneLite's Plugin Hub, enable board-only tracking, run `/bingo link` in the relevant Discord server, and paste the temporary code into the plugin. They then click **Link RuneLite** in the **ClanScape Board** sidebar; the panel shows clear linking or retry feedback and clears the code only after the server accepts it. The sidebar then shows the live board, organizer-confirmed progress, completed Bingos, and the full rule for each tile.

This is the dedicated public repository for Plugin Hub submission. The plugin defaults to the official ClanScape HTTPS service; do not replace it with a staging address before publication. Plugin Hub tracks this repository at a pinned commit, so every public update must be tested and submitted separately.

## Staging test

This repository includes a RuneLite development-client launcher. With Java 11+ installed, run
`./gradlew.bat run` from this folder. In the development client, configure the staging service URL,
enable board-only tracking, run `/bingo link` in the staging Discord server, paste the one-time
code, and click **Link RuneLite** in the sidebar. Do not publish a staging URL in the public Plugin
Hub repository.
