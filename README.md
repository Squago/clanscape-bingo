# ClanScape Bingo RuneLite Plugin

The companion is opt-in. It displays the linked player's server-authoritative 5x5 board in the RuneLite sidebar and sends only loot IDs, NPC IDs, and collection-log names declared by a live board's `telemetryRule`; it does not send general loot, location, inventory, chat, combat, or session information. The ClanScape service repeats the filter and creates an organizer-review candidate only. The sidebar never advances a board automatically: its progress changes only after organizer review.

After Plugin Hub publication, players install it by searching **ClanScape Bingo** in RuneLite's Plugin Hub, enable board-only tracking, run `/bingo link` in the relevant Discord server, and paste the temporary code into the plugin. They then click **Link RuneLite** in the **ClanScape Board** sidebar; the panel shows clear linking or retry feedback and clears the code only after the server accepts it. The sidebar then shows the live board, organizer-confirmed progress, completed Bingos, and the full rule for each tile.

Before Plugin Hub publication, copy this folder to its own public GitHub repository, set the real HTTPS service URL as the config default, test with the RuneLite development client, and submit that repository to Plugin Hub. Do not submit this monorepo.

## Staging test

This repository includes a RuneLite development-client launcher. With Java 11+ installed, run
`./gradlew.bat run` from this folder. In the development client, configure the staging service URL,
enable board-only tracking, run `/bingo link` in the staging Discord server, paste the one-time
code, and click **Link RuneLite** in the sidebar. Do not publish a staging URL in the public Plugin
Hub repository.
