# Aetheria Features


## QoL

- **Block Selection Overlay** — Replaces the vanilla block selection with a custom filled or outline highlight.
- **Enchant Parser** — Colors enchants by level, sorts ultimates to the top, supports normal/compressed/expanded layout and chroma(rainbow) animation.
- **Gyro Wand Helper** — Shows the AoE ring when holding the Gyrokinetic Wand, plus a cooldown timer.
- **Roman Numerals** — Converts Roman numerals to integers.
- **Prevent Cursor Reset** — Stops the mouse cursor from resetting when opening GUIs.
- **Skyblock ID** — Shows the internal SkyBlock item ID at the bottom of tooltips.
- **Disable Enchant Glint** — Removes the enchantment glint.
- **Brewing Helper** — Highlights brewing stands.
- **Missing Enchants** — Hold Shift on an enchanted item to see missing enchants.
- **Confirm Disconnect** — Makes you click twice to disconnect so you don't do it by accident.
- **Chat State Restore** — Restores your chat text when server closes chat.
- **Anvil Combine Helper** — Highlights matching items when one anvil slot is filled.
- **Slot Binds** — Bind inventory slots to hotbar slots for quick swapping on any container (unidirectional N:1, bind key to set/remove).
- **Better Containers** — Improved SkyBlock menu backgrounds with multiple styles and a watermark.
- **Damage Formatter** — Shortens large damage numbers (e.g., 1,234,567 → 1.2M).
- **Profile Parser (SkyAtlas)** — Parse your skyblock profiles for SkyAtlas. A web-based profile viewer.



## Misc

- **Performance HUD** — Shows FPS, TPS, ping, coords, and rotation in a small overlay.
- **Search Bar** — Adds a search bar to inventory GUIs with item highlighting.
- **Item Cooldowns** — Tracks cooldowns for abilities and invincibility timers with a HUD overlay.
- **Current Pet** — Shows your active pet as a HUD overlay.
- **Item Pickup Log** — Shows recently picked up or dropped items in a HUD.
- **Inventory Buttons** — Adds clickable shortcut buttons to inventories; configure with `/asmbuttons`.
- **Item Stack Tips** — Shows enchant levels on books and floor numbers on Catacombs passes.
- **Party Finder Floor Labels** — Shows F1–F7, M1–M7, or ENT on listings in the Party Finder.
- **Skill XP Display** — Hold Shift on a skill item to see XP remaining to max.
- **No Swap Animation** — Removes the item lowering animation when switching hotbar slots.
- **Show Own Nametag** — Shows your own nametag in third person.
- **Disable Entity Fire** — Hides the fire overlay on burning entities.
- **SkyBlock XP in Chat** — Sends SkyBlock XP gains from the action bar into chat. *(Needs server support)*
- **DVD Screensaver** — Adds a bouncing DVD logo screensaver.
- **Hoppity Rabbit Highlight** — Highlights NEW rabbits in Hoppity.
- **ATHRProtect** — Item protection system. Use `/asmprotect` while holding an item to protect it.
- **Sign Calculator** — Advanced calculator with expression support in signs.
- **Timer** — `/asmtimer 1h30m` Countdown timer HUD with pause, resume, and cancel.
- **Item List & Recipe Viewer** — Browse SkyBlock items and view their crafting recipes in-game.
- **Player Join/Leave Notifier** — Alerts when watched players join or leave; custom messages per player.
- **Bazaar Order Highlights** — Highlights filled sell orders in gold and buy orders in green.


## Dungeons

- **Blood Mob Highlight** — Highlights blood room mobs with a box or glow.
- **Boss Highlights** — Highlights Bonzo, Scarf, Scarf's minions, and the Professor with configurable colors.
- **Dungeon Overlay** — Run timers and end-of-run stats in chat.
- **Dungeon Breaker Overlay** — Shows Dungeon Breaker charges while in a dungeon.
- **Dungeon Room Overlay** — Shows the name of your current dungeon room.
- **CSGO Chest Opening** — Opening an obsidian/bedrock chest plays a CS:GO crate opening animation.
- **Hide Blessing Messages** — Suppresses the chat spam when dungeon blessings are found.


## Mining

- **Fetchur Overlay** — Shows today's Fetchur item.
- **Powder Tracker** — Tracks gemstone powder, chest drops, and goblin eggs in Crystal Hollows. Excludes PRISTINE drops.
- **Pristine Tracker** — Dedicated tracker for PRISTINE gemstone drops with rates/hour.
- **HOTM Powder Display** — Adds powder spent vs. max cost to HOTM perk tooltips; hold Shift to see the cost for the next 10 levels.
- **Commission Highlight** — Highlights completed commissions in green inside the Commissions menu.
- **Pickobulus Preview** — Shows a wireframe cube previewing the blast radius before activating Pickobulus.


## Fishing

- **Trophy Fish Tracker** — Tracks trophy fish counts with an overlay, chat message formatting, and Odger tooltip totals.
- **Fishing Timer** — Shows a timer while fishing with a configurable alert time.


## Overlays

- **Profile Viewer** — `/pv [username]` — View SkyBlock profiles in-game using data from SkyAtlas. Shows stats, dungeons, slayers, and more.


## Diana

- **Diana Tracker** — Tracks playtime, burrows, and mob rates during the Diana event.
- **Event Overlay** — HUD for the event stats.
- **Loot Overlay** — HUD for chimeras, rare drops, and coins.
- **Inquisitor HP Overlay** — Live HP bar for the nearest Minos Inquisitor.
- **Diana Mob HP Overlay** — Live HP bar for the nearest non-inquisitor Diana mob.


## Farming

- **Lock Mouse** — Locks your yaw and pitch so you don't accidentally move the camera while farming.
- **BPS Overlay** — Shows blocks broken per second while farming.


## Scoreboard

- **Custom Scoreboard** — Replaces the vanilla sidebar with a custom one.
  - Configurable lines, order, colors, scale, and alignment
  - Minimum width setting to prevent shrinking too small
  - Hide when Tab is held
  - Background color and corner radius customization
  - Drag-to-reorder lines with bin to hide unrecognized lines


## Chat

- **Chat Filters** — `/chatfilters` — Block or rewrite chat messages based on custom patterns.
- **Chat Compacting** — Collapses repeated identical messages; configurable expiry and consecutive-only modes.
- **Chat Timestamps** — Prepends timestamps to chat; choose 12/24-hour and whether to show seconds.
- **Chat Heads** — Shows a player's head next to their messages; hides repeats on consecutive messages.
- **Chat Copy** — Click or CTRL+click to copy a chat line with or without color codes.
- **Transparent Chat** — Makes the chat background fully transparent.
- **Animated Chat** — New messages slide into view.


## Cosmetics

- **Capes** — Visible to any player using Aetheria. Manage capes in `/capes`


## Storage

- **Storage Overlay** — Renders a Custom Storage Overlay which allows for managing inventories with ease.
- **Jump To Active** — Automatically center the active storage container in overlay.
- **Multiple themes** — Storage overlay has themes "Default", "Dark", "Wooden", "Ender", "Parchment" to select from.


## Waypoints

- **Ordered Waypoints** — `/athrw guide`
- **Waypoint Manager** — GUI to manage waypoint groups.
- **Auto Advance** — Automatically moves to the next waypoint when you're close enough for long enough.



## Commands

> **Note:** All commands are available with `asm`, `athr`, and `jef` prefixes for backward compatibility. For example, `/asm`, `/athr`, and `/jef` all work interchangeably.

- `/asm` — Opens the main Aetheria menu.
- `/asm config [category]` — Opens config, optionally to a specific category.
- `/asm reload` — Reloads repo data.
- `/pv [username]` — Opens the in-game Profile Viewer.
- `/sync` — Generate a sync code to link your Discord with SkyAtlas.
- `/asmtimer <time>` — Start, pause, resume, or cancel a countdown timer.
- `/chatfilters` — Opens the Chat Filters editor.
- `/athrcalc <expression>` — Advanced calculator with multipliers and trig support.
- `/diana <reset|toggle>` — Reset or pause Diana tracking.
- `/pdt <reset|toggle>` (`/powdertracker`) — Powder tracking controls.
- `/prt <reset|toggle>` (`/pristinetracker`) — Pristine tracking controls.
- `/lockmouse` — Toggle mouse lock for farming.
- `/athrw guide` — Ordered waypoint commands.
- `/waypoint` — Open the waypoint group manager.
- `/asmbuttons` — Open the inventory button editor.
- `/capes` — Open the cape manager.
- `/asmprotect` — Protect the held item from drops and sales.
- `/athrdebug` — Debug tools for copying tab list and footer data.


## Party Commands

+ `!help` for Diana commands.
+ `!pb` to view personal bests of dungeon floors and phases.
  - Usages: `!pb f1`–`m7`, `br`, `p1`, `p2`, `p3`, `p4`, `p5`
+ `!athr` to view a user's mod version.


> **Mod Installer:** Run the JAR directly (`java -jar Aetheria-*.jar`) to launch the standalone installer, which can download and update mods from GitHub releases.
