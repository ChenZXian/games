Iron Flag Duel

Overview

Iron Flag Duel is a single player Android land battle chess project built in Java. It uses a 5x12 tactical board, hidden enemy identities, deployment before battle, rail movement, mines, bombs, flag capture, and a modular AI that scores moves without reading unrevealed enemy identities.

Rule Set

The project implements one clear and consistent rule set.

1. The board is 5 columns by 12 rows.
2. Each side deploys one full army in its own half.
3. The flag must be placed in one of the two headquarters cells.
4. Mines can only be placed in the last two rows of the side and never in camps.
5. Headquarters pieces cannot move once battle starts.
6. Normal movement is one step orthogonally.
7. Camps add diagonal movement.
8. Rail cells allow straight line movement. Engineers can traverse the full connected rail network until blocked.
9. Bombs destroy both pieces in combat.
10. Engineers defeat mines. All other movable pieces lose to mines.
11. Higher rank defeats lower rank. Equal ranks remove both pieces.
12. Capturing the flag wins instantly.
13. A side with no legal moves loses.

AI Notes

The AI uses a knowledge model with unseen piece counts, back row flag and mine suspicion, move generation, and heuristic scoring. It does not inspect hidden enemy identities directly when evaluating unknown targets.

Project Structure

app/src/main/java/com/android/boot
  MainActivity.java
  core/
  ai/
  entity/
  ui/

Skin

skin_military_tech

How To Run

1. Open games/iron_flag_duel in Android Studio.
2. Ensure JDK and SDK match the repo baseline.
3. Sync Gradle.
4. Run the app on API 24 or higher.

Next Extensions

1. Difficulty layers with deeper search.
2. Deployment templates and saved layouts.
3. Replay and undo stack.
4. Online multiplayer.
5. Better animations and particle effects.
