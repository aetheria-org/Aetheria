# Aetheria Features


## QoL

- **Block Selection Overlay** — Replaces the vanilla block selection with a custom filled or outline highlight.
- **Enchant Parser** — Colors enchants by level, sorts ultimates to the top, supports normal/compressed/expanded layout and chroma(rainbow) animation.
- **Gyro Wand Helper** — Shows the AoE ring when holding the Gyrokinetic Wand, plus a cooldown timer.
- **Roman Numerals** — Converts Roman numerals to integers.
- **Prevent Cursor Reset** — Stops the mouse cursor from resetting when opening GUIs.
- **Skyblock ID & Price in Tooltip** — Shows the internal SkyBlock item ID and estimated market price at the bottom of item tooltips.
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
- **Rare Drop Tracker** — Search the full item database with `/rdt add <name>` (min. 3 letters) and get a chat/title/sound alert the instant you pick up a tracked item. Manage your list with `/rdt list`, `/rdt remove`, and `/rdt clear`.



## Misc

- **Performance HUD** — Shows FPS, TPS, ping, coords, and rotation in a small overlay.
- **Search Bar** — Adds a search bar to inventory GUIs with item highlighting.
- **Item Cooldowns** — Tracks cooldowns for abilities and invincibility timers with a HUD overlay.
- **Current Pet** — Shows your active pet as a HUD overlay.
- **Item Pickup Log** — Shows recently picked up or dropped items in a HUD.
- **Inventory Buttons** — Adds clickable shortcut buttons to inventories; configure with `/asmbuttons`. Optionally hides buttons in terminal menus.
- **Item Stack Tips** — Shows enchant levels on books and floor numbers on Catacombs passes.
- **Party Finder Floor Labels** — Shows F1–F7, M1–M7, or ENT on listings in the Party Finder.
- **Skill XP Display** — Hold Shift on a skill item to see XP remaining to max.
- **No Swap Animation** — Removes the item lowering animation when switching hotbar slots.
- **Show Own Nametag** — Shows your own nametag in third person.
- **Disable Entity Fire** — Hides the fire overlay on burning entities.
- **SkyBlock XP in Chat** — Sends SkyBlock XP gains from the action bar into chat. *(Needs server support)*
- **DVD Screensaver** — Adds a bouncing DVD logo screensaver.
- **Hoppity Rabbit Highlight** — Highlights NEW rabbits in Hoppity.
- **ASMProtect** — Item protection system. Use `/asmprotect` while holding an item to protect it.
- **Sign Calculator** — Advanced calculator with expression support in signs.
- **Timer** — `/asmtimer 1h30m` Countdown timer HUD with pause, resume, and cancel.
- **Item List & Recipe Viewer** — Browse SkyBlock items and view their crafting recipes in-game.
- **Player Join/Leave Notifier** — Alerts when watched players join or leave; custom messages per player.
- **Bazaar Order Highlights** — Highlights filled sell orders in gold and buy orders in green.
- **Ghost Tracker** — `/ghosttracker` — Tracks ghost kills, drops (Sorrow, Volta, Plasma, etc.), magic find, and scavenger coins in the Dwarven Mines / The Mist with a configurable HUD overlay.
- **Item Log Alerts** — On-screen alerts when configured items are picked up; choose Always or First-Time-Only mode.
- **Item Prices** — Community-driven price database. Dynamically fetches and displays item prices from the community API (auction & bazaar). Configurable detail level (Latest / 24h / 1 Week / 1 Month), optionally show prices only while holding a key, and optionally submit parsed data to the shared price database. Powers profit estimates across the mod.
- **Kill Combo Tracker** — `/killcombo` — Tracks your current and highest kill combo with magic find, coins per kill, and combat wisdom lines.
- **Incompatible Mods Warning** — Notifies when incompatible mods are installed and what they break (SkyblockAddons breaks the enchant parser and enchant chroma; Patcher may break enchant chroma). `/athrignoreincompat ignore|reset|list [modId]` hides or manages warnings.


## Dungeons

- **Blood Mob Highlight** — Highlights blood room mobs with a box or glow.
- **Boss Highlights** — Highlights Bonzo, Scarf, Scarf's minions, and the Professor with configurable colors.
- **Dungeon Overlay** — Run timers and end-of-run stats in chat.
- **Dungeon Breaker Overlay** — Shows Dungeon Breaker charges while in a dungeon.
- **Dungeon Room Overlay** — Shows the name of your current dungeon room.
- **CSGO Chest Opening** — Opening an obsidian/bedrock chest plays a CS:GO crate opening animation.
- **Hide Blessing Messages** — Suppresses the chat spam when dungeon blessings are found.
- **Secret Finder** — Highlights dungeon secrets (chests, levers, superboom, essences, fairy souls, wither essence) with labels, waypoints, tracers, and bounding boxes. Configurable colors and range detection.
- **Dungeon Map** — Custom overlay showing dungeon rooms, player heads with names/ranks, and visited room labels.
- **Dungeon Leap Menu** — Replaces the Spirit Leap/InfiniLeap chest with a full dungeon map. Click player heads or grid buttons to leap to them. Optional player list, arrow icons, and self-exclusion from the map.
- **Dungeon Chest Price Estimator** — Estimates profit or loss on dungeon reward chests using the community price API, with an analyzer overlay that highlights the best-value chest.
- **Secret Reports** — `/report-secret` — Report a wrong secret location from the current dungeon room.


## Mining

- **Fetchur Overlay** — Shows today's Fetchur item.
- **Powder Tracker** — Tracks gemstone powder, chest drops, and goblin eggs in Crystal Hollows. Excludes PRISTINE drops.
- **Pristine Tracker** — Dedicated tracker for PRISTINE gemstone drops with rates/hour.
- **Gold Tracker** — Tracks gold ingot and enchanted gold pickups in Dwarven Mines and Crystal Hollows. Configurable display unit (ingots or enchanted), profit unit (ingots, enchanted, or blocks), and mining stats from tablist (speed, fortune, spread). Compact drop tracking via chat.
- **HOTM Powder Display** — Adds powder spent vs. max cost to HOTM perk tooltips; hold Shift to see the cost for the next 10 levels.
- **Commission Highlight** — Highlights completed commissions in green inside the Commissions menu.
- **Pickobulus Preview** — Shows a wireframe cube previewing the blast radius before activating Pickobulus.
- **Powder Mining Chat Filter** — Hides powder mining reward popups and chat lines in the Crystal Hollows.
  - Chest unlocked / already looted and breaking-power warnings
  - Compact messages and reward wrapper lines (separators, headers)
  - Powder and essence lines, each with an amount threshold
  - Gemstone drops with a tier filter (Rough / Flawed / etc.)
  - Special drops: Oil Barrel, Ascension Rope, Wishing Compass, Jungle Heart, Prehistoric Egg, Pickonimbus 2000, Sludge Juice, Yoggie, Robot Parts, Treasurite


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
- **Profit Estimate** — Live profit calculation for all Diana drops (Chimeras, Daedalus Sticks, Feathers, etc.) using community price data, displayed in the Loot Overlay.
- **Diana Party Finder** — `/dparty <join|create|leave|disband|transfer|kick|setpass|list>` — Cross-server Diana party system with a GUI. Party chat via `/dpc <message>`.


## Farming

- **Lock Mouse** — Locks your yaw and pitch so you don't accidentally move the camera while farming. Shows a lock icon and unlock hint when active.
- **BPS Overlay** — Shows blocks broken per second while farming.
- **Trevor Solver** — Highlights possible spawn spots when Trevor gives you a hunt and marks the animal once it spawns.
  - Spot color and distance labels
  - Animal beacon beam with custom color
  - First-detect title/chat alert
  - Trapper warp helper: press the warp key within 5 seconds after a kill to run /warp trapper
- **Pelt Tracker** — Overlay tracking pelts earned this session and your pelts/hour rate; reposition and reset from the config.
- **Farming Tracker** — `/asmfarming` — Tracks crop counts and coin value with a per-hour rate.
  - Require farming location (Barn, Private Island, Garden)
  - Keep tracker across sessions
  - Configurable display lines and overlay scale/colors
- **Organic Matter Tracker** — `/asmorganicmatter` — Tracks Organic Matter and items/hour.
  - Choose which crops count (incl. Seeds, Squash, Cropie, Fermento)
  - Configurable display lines and overlay scale/colors
- **Sensitivity Reducer** — Reduces your mouse sensitivity while holding a crop farming tool (Melon Dicer, Pumpkin Dicer, etc.); configurable percentage and optional farming-island requirement.
- **Precise Yaw/Pitch Overlay** — Live pitch/yaw HUD; configurable label color, position, and scale.
- **Visitor Shopping List** — Tracks what each Garden visitor wants and gives. Panel and overlay show prices, profit, have/need counts, and farming time estimates. Sign fill for Bazaar amount signs. Configurable copper deal quality display with confirm-click protection. Custom tooltips replace vanilla Accept Offer lore with prices and profit. Panel shown on visitor menus, Bazaar, inventory, and signs.
- **Garden Plot Numbers** — Shows plot numbers on Configure Plots chest slots. Color-coded by state (unlocked, buyable, no materials, locked). Optional slot highlights for unlocked and buyable plots.

### Pests

- **Pest Finder** — Overlay showing tablist pest data (total pests, plots, spray, repellent, bonus, cooldown, bonus pest chance). Configurable warp keybind to teleport to infested plots with Closest or Most Pests target selection. Requires holding a vacuum to show.
- **Pest Tracker** — `/pesttracker` — Tracks pest kills and crop drops with profit estimate in the Garden. Configurable lines: Total Pests, Total Drops, Session Time, Total Time, Pests (per-type), Drops (per-crop), Profit Estimate. Rate basis: All Time or Session.
- **Pest Cooldown Alert** — `/asmpest` — Warns when pest cooldown drops below a configurable threshold (5-300s). Sound, chat, and on-screen banner with flash/fade animation. Off by default.


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
- **Chat Ping** - Play a sound and highlight the message when your name is mentioned in chat.
- **Emojis** — Renders `:emoji_name:` tokens as emoji textures in chat, with a suggestion popup while typing and selectable Discord/Google/iOS themes.


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



## Network & Privacy

- **Global Chat** — `/globalchat` — Cross-server chat with image/GIF support.
  - Reduced animations (only animate on hover)
  - Max image/GIF quality (240p–4K)
  - Mention notifications: toast on `@username` / `@everyone`
  - Smart Connection: only connect while in use, disconnect after 10 minutes of inactivity (off by default)
- **Network Controls** — Manage how much internet access the mod gets.
  - Offline Mode: disable all internet access (most features stop working)
  - Disable API Calls: turn off features that use the mod API (capes, profile viewer, profile parser, /sync)
  - Disable GitHub Calls: stop updating from GitHub (overlays, timers, version checks)
- **Network Status Screen** — Shows which network features are disabled and what they break. Appears when blocked gates change. Buttons to enable features or dismiss.
- **Telemetry** — Controls what the mod shares on server join (used for player counts and bug reports).
  - Disable Telemetry: don't share your username, mod list, or version
  - Hide Mod List in Telemetry: only keep your username and version
- **Privacy Notice** — Review how your data is handled before enabling features. [Read the privacy policy](/docs/ABOUT.md).



## Commands

> **Note:** All commands are available with `asm`, `athr`, and `jef` prefixes for backward compatibility. For example, `/asm`, `/athr`, and `/jef` all work interchangeably.

- `/asm` — Opens the main Aetheria menu.
- `/asm config` — Opens the config editor.
- `/asm <search>` — Opens the config editor with the search box pre-filled with `<search>`.
- `/asm reload` — Reloads repo data.
- `/pv [username]` — Opens the in-game Profile Viewer.
- `/sync` — Generate a sync code to link your Discord with SkyAtlas.
- `/asmtimer <time>` — Start, pause, resume, or cancel a countdown timer.
- `/chatfilters` — Opens the Chat Filters editor.
- `/athrcalc <expression>` — Advanced calculator with multipliers and trig support.
- `/diana <reset|toggle>` — Reset or pause Diana tracking.
- `/pdt <reset|toggle>` (`/powdertracker`) — Powder tracking controls.
- `/prt <reset|toggle>` (`/pristinetracker`) — Pristine tracking controls.
- `/dparty <join|create|leave|disband|transfer|kick|setpass|list>` — Diana Party Finder controls.
- `/dpc <message>` — Send a message in your Diana party chat.
- `/asmfarming <on|off|reset>` — Farming Tracker controls.
- `/asmorganicmatter <on|off|reset>` — Organic Matter Tracker controls.
- `/killcombo <reset|toggle>` — Kill Combo Tracker controls.
- `/ghosttracker <reset|toggle>` — Ghost Tracker controls.
- `/report-secret` — Open the Secret Report GUI for the current dungeon room.
- `/globalchat` — Open the Global Chat window.
- `/athrignoreincompat <ignore|reset|list> [modId]` — Manage hidden incompatible-mod warnings.
- `/lockmouse` — Toggle mouse lock for farming.
- `/pesttracker` (`pest`, `pt`) — Pest Tracker controls: reset, show, hide, toggle.
- `/asmpest` (`pestcd`) — Pest Cooldown Alert: set threshold with `/asmpest <time>`, or off, status, test.
- `/visitortip` (`asmvisitortip`) — Toggle visitor tip visibility.
- `/athrnet enable|hide|unhide|reset|list` — Network status commands.
- `/athrw guide` — Ordered waypoint commands.
- `/waypoint` — Open the waypoint group manager.
- `/asmbuttons` — Open the inventory button editor.
- `/capes` — Open the cape manager.
- `/asmprotect` — Protect the held item from drops and sales.
- `/asmdebug` — Copy a general debug report to the clipboard.
- `/asmcopyitem`, `/asmcopyinternalname` — Copy held-item info / internal SkyBlock id.
- `/asmcopytablist`, `/asmcopytabfooter`, `/asmcopyactionbar`, `/asmcopybossbar`, `/asmcopyscoreboard` — Copy various HUD/UI text to the clipboard (all support `-nocolor`).
- `/asmcopylocation`, `/asmcopynearbyentities` — Copy player position / nearby entity dumps.


## Party Commands

+ `!help` for Diana commands.
+ `!pb` to view personal bests of dungeon floors and phases.
  - Usages: `!pb f1`–`m7`, `br`, `p1`, `p2`, `p3`, `p4`, `p5`
+ `!athr` to view a user's mod version.


> **Mod Installer:** Run the JAR directly (`java -jar Aetheria-*.jar`) to launch the standalone installer, which can download and update mods from GitHub releases.
