# Aetheria 1.0.3 - 1.1.0

A new major release focusing on dungeons, profit trackers, and privacy, with internal rewrites and performance improvements.

### New Features

* Added secret waypoints
* Added dungeon map with player heads and names
* Added ghost tracker
* Added item log alerts
* Added price system — fetches item prices from the community price API and bazaar/auction parsing
* Added price display in item lore, optionally gated behind a held key
* Added configurable price detail level (Latest / 24h / 1 Week / 1 Month)
* Added option to submit parsed bazaar/auction data to the shared price database
* Added Dungeon Reward Profit Estimator
* Added profit estimate in Diana tracker
* Added config option to disable inventory buttons in terminal menus

### Privacy & Configuration

* Added Privacy Notice screen on first launch to opt out of any external calls before they are executed (note: GitHub resources such as Skyblock recipes and the cape index may still be fetched before this screen appears)
* Added config options to opt out of telemetry
* Added config options to disable API calls and GitHub fetching
* Added NetworkGuard to all external calls (except whitelist checks in alpha/beta versions)
* Unreleased mod versions now require whitelist access
* Added branches support in config GUI
* Added ConfigEditorTextDisplay config annotation

### Internal Changes

* Rewritten event registration
* Rewritten auto mixin discovery
* Rewritten main command registry
* Rewritten repo fetching system
* Moved all MoulConfig-derived files into the moulconfig package with LGPL v3 license header

### Bug Fixes & Improvements

* Fixed chat formatting not being preserved by chat ping highlights
* Fixed search bar defaulting to item list mode while the feature was off
* Fixed phase timers continuing to run after a run had ended
* Fixed Diana tracker incrementing activity on every chat message
* Fixed a crash in scoreboard debug
* Improved room detection in Dungeon Room Overlay
* Improved performance in all world rendering
* Optimized mod installer
* Optimized and centralized all inventory container usages
* Decreased mod size by not shadowing reflections

# Important changes

Aetheria is now licensed under the **Aetheria Mod License v1.0 (AML-1.0)**. The source code is publicly available and we're committed to keeping it that way. For a full breakdown of what this means for players, contributors, content creators, and developers, see [ABOUT.md](https://github.com/aetheria-org/Aetheria/blob/main/docs/ABOUT.md).

The full changelog can be found [here](https://github.com/aetheria-org/Aetheria/commits/main/).