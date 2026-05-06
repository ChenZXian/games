Candy Chain Quest

Project structure

app/src/main/java/com/android/boot/MainActivity.java
app/src/main/java/com/android/boot/core/GameSession.java
app/src/main/java/com/android/boot/core/BoardManager.java
app/src/main/java/com/android/boot/core/MatchDetector.java
app/src/main/java/com/android/boot/core/SwapSystem.java
app/src/main/java/com/android/boot/core/FallSystem.java
app/src/main/java/com/android/boot/core/SpecialEffectSystem.java
app/src/main/java/com/android/boot/core/ObstacleSystem.java
app/src/main/java/com/android/boot/core/LevelManager.java
app/src/main/java/com/android/boot/core/ScoreManager.java
app/src/main/java/com/android/boot/core/InputManager.java
app/src/main/java/com/android/boot/core/GameStateMachine.java
app/src/main/java/com/android/boot/core/EffectManager.java
app/src/main/java/com/android/boot/ui/GameView.java
app/src/main/java/com/android/boot/ui/GameLoopThread.java
app/src/main/java/com/android/boot/audio/SoundController.java
app/src/main/java/com/android/boot/entity

Selected ui skin

skin_neon_future

Suggested registry entry

{
  "id": "candy_chain_quest",
  "name": "Candy Chain Quest",
  "tags": ["match3", "puzzle", "cascade", "special-combo"],
  "core_loop": "Swap adjacent tiles to form matches, generate specials, break obstacles, and complete layered goals under move or time pressure.",
  "created_at": "2026-04-22",
  "ui_skin": "skin_neon_future"
}
