package com.android.boot.core;

import com.android.boot.entity.GoalDefinition;
import com.android.boot.entity.GoalType;
import com.android.boot.entity.LevelConfig;
import com.android.boot.entity.ObstacleSeed;
import com.android.boot.entity.ObstacleType;
import com.android.boot.entity.SpawnProfile;
import com.android.boot.entity.TileColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LevelManager {
    private final List<LevelConfig> levels = new ArrayList<>();
    private int currentLevelIndex;
    private int remainingMoves;
    private float remainingTime;

    public LevelManager() {
        levels.add(buildLevelOne());
        levels.add(buildLevelTwo());
        levels.add(buildLevelThree());
    }

    private LevelConfig buildLevelOne() {
        SpawnProfile profile = new SpawnProfile()
                .weight(TileColor.RED, 22)
                .weight(TileColor.BLUE, 18)
                .weight(TileColor.GREEN, 18)
                .weight(TileColor.YELLOW, 18)
                .weight(TileColor.PURPLE, 12)
                .weight(TileColor.ORANGE, 12);
        return new LevelConfig("Level 1 - Neon Orchard", 8, 8, 24, 0, profile)
                .addGoal(new GoalDefinition(GoalType.SCORE, null, null, 14000))
                .addGoal(new GoalDefinition(GoalType.CLEAR_COLOR, TileColor.RED, null, 12))
                .addObstacle(new ObstacleSeed(2, 2, ObstacleType.ICE, 1))
                .addObstacle(new ObstacleSeed(2, 5, ObstacleType.ICE, 1))
                .addObstacle(new ObstacleSeed(5, 2, ObstacleType.CHAIN, 1))
                .addObstacle(new ObstacleSeed(5, 5, ObstacleType.CHAIN, 1));
    }

    private LevelConfig buildLevelTwo() {
        SpawnProfile profile = new SpawnProfile()
                .weight(TileColor.RED, 16)
                .weight(TileColor.BLUE, 16)
                .weight(TileColor.GREEN, 24)
                .weight(TileColor.YELLOW, 16)
                .weight(TileColor.PURPLE, 14)
                .weight(TileColor.ORANGE, 14);
        return new LevelConfig("Level 2 - Cargo Lock", 8, 8, 28, 0, profile)
                .addGoal(new GoalDefinition(GoalType.CLEAR_OBSTACLE, null, ObstacleType.CRATE, 6))
                .addGoal(new GoalDefinition(GoalType.CLEAR_COLOR, TileColor.GREEN, null, 14))
                .addObstacle(new ObstacleSeed(3, 2, ObstacleType.CRATE, 2))
                .addObstacle(new ObstacleSeed(3, 3, ObstacleType.CRATE, 2))
                .addObstacle(new ObstacleSeed(3, 4, ObstacleType.CRATE, 2))
                .addObstacle(new ObstacleSeed(4, 2, ObstacleType.CRATE, 2))
                .addObstacle(new ObstacleSeed(4, 3, ObstacleType.STONE, 999))
                .addObstacle(new ObstacleSeed(4, 4, ObstacleType.CRATE, 2))
                .addObstacle(new ObstacleSeed(2, 6, ObstacleType.ICE, 1))
                .addObstacle(new ObstacleSeed(5, 1, ObstacleType.CHAIN, 1));
    }

    private LevelConfig buildLevelThree() {
        SpawnProfile profile = new SpawnProfile()
                .weight(TileColor.RED, 18)
                .weight(TileColor.BLUE, 18)
                .weight(TileColor.GREEN, 14)
                .weight(TileColor.YELLOW, 14)
                .weight(TileColor.PURPLE, 18)
                .weight(TileColor.ORANGE, 18);
        return new LevelConfig("Level 3 - Reactor Rush", 8, 8, 0, 90, profile)
                .addGoal(new GoalDefinition(GoalType.SCORE, null, null, 24000))
                .addGoal(new GoalDefinition(GoalType.CLEAR_OBSTACLE, null, ObstacleType.CHAIN, 8))
                .addObstacle(new ObstacleSeed(1, 1, ObstacleType.CHAIN, 1))
                .addObstacle(new ObstacleSeed(1, 6, ObstacleType.CHAIN, 1))
                .addObstacle(new ObstacleSeed(2, 3, ObstacleType.ICE, 1))
                .addObstacle(new ObstacleSeed(2, 4, ObstacleType.ICE, 1))
                .addObstacle(new ObstacleSeed(5, 3, ObstacleType.CRATE, 2))
                .addObstacle(new ObstacleSeed(5, 4, ObstacleType.CRATE, 2))
                .addObstacle(new ObstacleSeed(6, 1, ObstacleType.CHAIN, 1))
                .addObstacle(new ObstacleSeed(6, 6, ObstacleType.CHAIN, 1));
    }

    public LevelConfig getCurrentLevel() {
        return levels.get(currentLevelIndex);
    }

    public void startCurrentLevel() {
        remainingMoves = getCurrentLevel().getMoveLimit();
        remainingTime = getCurrentLevel().getTimeLimitSeconds();
        for (GoalDefinition goal : getCurrentLevel().getGoals()) {
            goal.setProgress(0);
        }
    }

    public void restartCurrentLevel() {
        startCurrentLevel();
    }

    public void advanceLevel() {
        currentLevelIndex = (currentLevelIndex + 1) % levels.size();
        startCurrentLevel();
    }

    public void useMove() {
        if (remainingMoves > 0) {
            remainingMoves--;
        }
    }

    public void updateTime(float delta) {
        if (remainingTime > 0f) {
            remainingTime = Math.max(0f, remainingTime - delta);
        }
    }

    public boolean isOutOfResources() {
        return (getCurrentLevel().getMoveLimit() > 0 && remainingMoves <= 0) || (getCurrentLevel().getTimeLimitSeconds() > 0 && remainingTime <= 0f);
    }

    public int getRemainingMoves() {
        return remainingMoves;
    }

    public float getRemainingTime() {
        return remainingTime;
    }

    public String getHudTurnText() {
        if (getCurrentLevel().getMoveLimit() > 0) {
            return String.valueOf(remainingMoves);
        }
        return String.format(Locale.US, "%.0f", remainingTime);
    }

    public String getLevelTitle() {
        return getCurrentLevel().getTitle();
    }

    public String buildGoalSummary() {
        StringBuilder builder = new StringBuilder();
        List<GoalDefinition> goals = getCurrentLevel().getGoals();
        for (int i = 0; i < goals.size(); i++) {
            GoalDefinition goal = goals.get(i);
            if (i > 0) {
                builder.append("   ");
            }
            builder.append(formatGoal(goal));
        }
        return builder.toString();
    }

    private String formatGoal(GoalDefinition goal) {
        switch (goal.getGoalType()) {
            case SCORE:
                return "Score " + goal.getProgress() + "/" + goal.getTarget();
            case CLEAR_COLOR:
                return goal.getTileColor().name() + " " + goal.getProgress() + "/" + goal.getTarget();
            case CLEAR_OBSTACLE:
                return goal.getObstacleType().name() + " " + goal.getProgress() + "/" + goal.getTarget();
            case DROP_OBJECT:
                return "Drop " + goal.getProgress() + "/" + goal.getTarget();
            case CLEAR_ALL_CRATES:
                return "Crates " + goal.getProgress() + "/" + goal.getTarget();
            default:
                return "Goal";
        }
    }

    public List<GoalDefinition> getGoals() {
        return getCurrentLevel().getGoals();
    }

    public void updateScoreGoal(int score) {
        for (GoalDefinition goal : getGoals()) {
            if (goal.getGoalType() == GoalType.SCORE) {
                goal.setProgress(score);
            }
        }
    }

    public boolean areGoalsComplete() {
        for (GoalDefinition goal : getGoals()) {
            if (!goal.isComplete()) {
                return false;
            }
        }
        return true;
    }
}
