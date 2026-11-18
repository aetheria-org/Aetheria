# Aetheria 1.3.0 - 2.0.0

Aetheria's Skyblock Mod 2.0.0: now available for Minecraft 1.21.1 (Fabric) alongside 1.8.9 (Forge), with more features and better optimization.

### New Features

- Added Item Animations (swing range, speed & position)
- Added Chat Tabs
- Added Player Buttons
- Added Secret Highlights (Lever, Bat, Dropped Item & Essence)
- Added Secret Chime
- Added Garden Plot Warp Grid
- Added New Waypoint Menu
- Added New Chat Filter menu
- Added New ASM waypoint format
- Added Bal Timer & Alert
- Added Personal Booster lines to scoreboard
- Added Room, Secrets & Blood cleared alerts
- Added Tooltip Scaling & Scrolling
- Added Extended Chat History

### Bug Fixes & Improvements

**Additions**
- Better Containers: added "Hide Empty Pane Hover" option (no hover highlight, tooltip or clicks on empty panes)
- Search Bar: added clear button, `/` command execution, recent searches dropdown, hover item copy key & configurable submit key
- Global Chat is now integrated with Minecraft chat
- You can now change chat background opacity via slider

**Improvements**
- Improved config UX and performance (auto-open first subcategory, smoother scrolling, persistent menu state)
- Rewrote the Secret Waypoint system; fixed bugs and improved performance
- Improved Dungeon Map performance (~30 more FPS)
- Ghost Tracker overlay is now only visible in the Mist
- Added marquee text for Fetchur in the scoreboard
- Added new Cylinder mode for Gyrowand
- Added compact booster cookie time in the scoreboard
- New scoreboard lines are now added automatically to existing configs
- Improved Chat and Enchant Parser config UX
- Improved loading times and RAM usage
- All formatted numbers now display to 2 decimal places (e.g. 1.68m instead of 2m)

**Fixes**
- Fixed calculator applying thousands-separator commas back into the search bar
- Fixed NEF (NotEnoughFakepixel) middle clicks breaking the Current Pet Tracker
- Fixed vanilla mining progress resetting on item updates, bow draws being cancelled by quiver updates, and swap animation replaying
- Fixed broken chat features
- Fixed CrashLog recursively failing
- Fixed Emojis not loading correctly
- Fixed map calibration anchoring only to the entrance room

The full changelog can be found [here](https://github.com/aetheria-org/Aetheria/commits/main/).
