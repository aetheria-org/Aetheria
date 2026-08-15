# Aetheria 1.1.0 - 1.2.0

Aetheria's Skyblock Mod 1.2.0: major performance improvements, farming features, and a reworked Dungeon Map.

### New Features

- Added Diana Party Finder (/dparty)
- Added Donate Chat Detection
- Added emojis in chat and an emoji suggestion bar
- Added Farming Tracker (/asmfarming)
- Added Global Chat (/globalchat)
- Added Incompatible Mods Warning (/asmignoreincompat)
- Added Kill Combo Tracker (/killcombo)
- Added Organic Matter Tracker (/asmorganicmatter)
- Added Pelt Tracker
- Added Powder Mining Chat Filter
- Added Precise Yaw/Pitch Overlay
- Added profile, IGN, and server specific data storage for all trackers and the storage overlay (in alpha too, I hope)
- Added Rare Drop Tracker (/rdt)
- Added Secret Reports to flag wrong secret locations (/report-secret)
- Added Sensitivity Reducer
- Added Trevor Solver
- Reworked the Dungeon Map

### Internal Changes

- Added a fallback for API errors and removed fetching during price upload
- Added more debug commands (/asmdebug) to help.. debug?
- Added potion type and level parsing for profile viewing and uploading
- Centralized async execution with ThreadUtils
- Fixed ChatUtils leaking executors
- Switched SkyAtlas links to https://skyatlas.lol

### Bug Fixes & Improvements

- Added Ghost Tracker reset and toggle (/ghosttracker)
- Added mouse-locked indication and unlock hint
- Added Pause On Chat to all trackers.
- Added options to hide overlays on scoreboard, tab, chat for each individually
- Fixed convert-to-item items being recognized as pets
- Fixed Diana Tracker not filtering messages (/diana)
- Fixed Dungeon Profit Estimate chest title check
- Fixed enchant description hiding
- Fixed fairy souls being included in secret routes
- Fixed mayor detection before elections have started
- Fixed Storage Overlay consuming clicks while the server command cooldown is active
- Made the search bar render below tooltips
- Now uses US decimal format and accepts commas
- Reworked SkyBlock XP in Chat to accurately show SB XP updates
- Reworked Pristine and Powder Trackers for better performance
- Added Show Rates as Rough for Pristine Tracker
- Fixed AQnvil Combine Helper highlighting every enchant

### Performance

- Improved all overlay rendering with smart caching
- Improved Dungeon Room Detector performance with caching
- Improved loading times with multithreading and fewer unnecessary fetches
- Improved overall Storage Overlay performance and logic
- Optimized item list panes
- Optimized price fetching and upload

Overall code and performance improvements across the mod.

This release also lays the groundwork for the upcoming Aetheria versions on modern Minecraft versions.

The full changelog can be found [here](https://github.com/aetheria-org/Aetheria/commits/main/).