# Free Quest map clear (experimental custom build)

This mode is for English/NA story maps after finishing their main story. It uses your
selected FGA battle profile, including its support selection, skills and card settings.
It stays in the currently open map; it does not switch chapters or handle event maps.

1. Install the custom **FGA Free Quests** APK. It installs alongside official FGA. Export
   your battle profiles from official FGA and import them into this app.
2. Enable the usual accessibility and screenshot permissions, choose the NA server,
   and open the story map in FGO.
3. Press the overlay Play button, select your battle profile and check
   **Free Quest map clear (experimental)**. Disable run/material/CE limits if you want
   the whole map cleared. Configure apples as usual; zero refills stops when AP runs out.
4. Keep the game visible. The script closes any open quest panel, pinches the map out,
   selects an orange notification counter and runs the first quest in that node.
   The first quest must be the uncleared Free Quest.
5. It repeats that quest until FGA recognizes the first-clear reward popup, accepts it,
   rejects any repeat prompt, closes the panel and pinches out again. It stops after
   three recognized map scans contain no eligible notification badges.

The existing “stop on first-clear rewards” setting is ignored in this mode. Other battle
stop conditions, including refill limits and inventory-full checks, remain active.
Result screens advance one tap at a time so the reward popup can be detected.

This build has not been validated in a live FGO session. Map badge recognition uses
orange color, circular shape, white border and white digits; navigation also requires a
recognized blue Free Quest map marker. Badges obscured by the game's HUD or clipped at
the screen edge may need manual map adjustment. A failure to recognize the map is an
error, not a successful map clear. Repeated navigation failures or six runs without a
recognized first-clear reward stop the script for inspection. Do not start on a map
with unfinished main story, interludes or other quests above the intended Free Quest.

Templates `free_quest_close_panel.png` and `free_quest_map_marker.png` are cropped from
`docs/reference-images/En/quest/quest-list.png`; `free_quest_repeat_close.png` is cropped
from `docs/reference-images/En/battle-result/repeat-quest.png`. Those existing reference
screenshots are 720p. Map marker matching tries several sizes to cover map zoom levels.

Builds use the upstream **PR Build** GitHub Actions workflow (`workflow_dispatch` on
the `free-quest-mode` branch). It runs unit tests and `assembleCi`; the APK uses the
project's CI signing key and application ID `io.github.fate_grand_automata.freequests`.
