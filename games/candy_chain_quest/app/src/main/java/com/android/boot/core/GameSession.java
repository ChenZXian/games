package com.android.boot.core;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.audio.SoundController;
import com.android.boot.entity.Cell;
import com.android.boot.entity.ClearSummary;
import com.android.boot.entity.GoalDefinition;
import com.android.boot.entity.GoalType;
import com.android.boot.entity.MatchGroup;
import com.android.boot.entity.ObstacleType;
import com.android.boot.entity.Position;
import com.android.boot.entity.SpecialType;
import com.android.boot.entity.SwapData;
import com.android.boot.entity.TileColor;
import com.android.boot.entity.VisualEffect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameSession {
    private final Context context;
    private final RandomProvider randomProvider = new RandomProvider();
    private final BoardManager boardManager = new BoardManager(randomProvider);
    private final MatchDetector matchDetector = new MatchDetector();
    private final SwapSystem swapSystem = new SwapSystem();
    private final FallSystem fallSystem = new FallSystem();
    private final SpecialEffectSystem specialEffectSystem = new SpecialEffectSystem();
    private final ObstacleSystem obstacleSystem = new ObstacleSystem();
    private final GameStateMachine stateMachine = new GameStateMachine();
    private final InputManager inputManager = new InputManager();
    private final ScoreManager scoreManager = new ScoreManager();
    private final LevelManager levelManager = new LevelManager();
    private final EffectManager effectManager = new EffectManager();
    private final SoundController soundController;

    private GameEventListener eventListener;
    private boolean lastResultWin;
    private String resultSummary = "";
    private Position swapStart;
    private Position swapEnd;
    private float phaseTimer;
    private int chainDepth;
    private int boardAccentColor;
    private int specialEffectColor;
    private int successEffectColor;
    private int warningEffectColor;

    public GameSession(Context context, SoundController soundController) {
        this.context = context;
        this.soundController = soundController;
        boardAccentColor = ContextCompat.getColor(context, R.color.cst_accent);
        specialEffectColor = ContextCompat.getColor(context, R.color.cst_accent_2);
        successEffectColor = ContextCompat.getColor(context, R.color.cst_success);
        warningEffectColor = ContextCompat.getColor(context, R.color.cst_warning);
    }

    public void setEventListener(GameEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void startNewRun() {
        scoreManager.reset();
        levelManager.startCurrentLevel();
        boardManager.loadLevel(levelManager.getCurrentLevel());
        effectManager.clear();
        inputManager.clear();
        stateMachine.setState(GameState.PLAYING);
        stateMachine.setBoardPhase(BoardPhase.IDLE);
        chainDepth = 0;
        emitUiChange();
    }

    public void restartLevel() {
        scoreManager.reset();
        levelManager.restartCurrentLevel();
        boardManager.loadLevel(levelManager.getCurrentLevel());
        inputManager.clear();
        effectManager.clear();
        stateMachine.setState(GameState.PLAYING);
        stateMachine.setBoardPhase(BoardPhase.IDLE);
        chainDepth = 0;
        emitUiChange();
    }

    public void backToMenu() {
        stateMachine.setState(GameState.MENU);
        inputManager.clear();
        effectManager.clear();
        emitUiChange();
    }

    public void pauseGame() {
        stateMachine.setState(GameState.PAUSED);
        emitUiChange();
    }

    public void resumeGame() {
        if (stateMachine.getState() == GameState.PAUSED) {
            stateMachine.setState(GameState.PLAYING);
            emitUiChange();
        }
    }

    public void advanceAfterResult() {
        levelManager.advanceLevel();
        startNewRun();
    }

    public void onTileTapped(int row, int col) {
        Position first = inputManager.handleTap(row, col, stateMachine);
        if (first == null) {
            emitUiChange();
            return;
        }
        Position second = new Position(row, col);
        beginPlayerSwap(first, second);
    }

    private void beginPlayerSwap(Position first, Position second) {
        if (!stateMachine.acceptsInput()) {
            return;
        }
        if (swapSystem.beginSwap(boardManager, first, second)) {
            swapStart = first;
            swapEnd = second;
            phaseTimer = 0.16f;
            stateMachine.setBoardPhase(BoardPhase.SWAP_FORWARD);
            soundController.playSwap();
            emitUiChange();
        } else {
            soundController.playInvalid();
        }
    }

    public void update(float delta) {
        effectManager.update(delta);
        if (!stateMachine.isPlaying()) {
            return;
        }
        levelManager.updateTime(delta);
        if (levelManager.getCurrentLevel().getTimeLimitSeconds() > 0) {
            emitUiChange();
        }
        if (levelManager.isOutOfResources() && !levelManager.areGoalsComplete()) {
            finishRun(false);
            return;
        }
        switch (stateMachine.getBoardPhase()) {
            case IDLE:
                if (!boardManager.hasAnyMove()) {
                    stateMachine.setBoardPhase(BoardPhase.SHUFFLE);
                    phaseTimer = 0.28f;
                    effectManager.add(new VisualEffect(0.5f, 0.5f, 0.5f, warningEffectColor, 0.35f, "Shuffle"));
                }
                break;
            case SWAP_FORWARD:
                advancePhaseTimer(delta, this::handleSwapResolved);
                break;
            case SWAP_BACK:
                advancePhaseTimer(delta, this::finishSwapBack);
                break;
            case RESOLVE_CLEAR:
                advancePhaseTimer(delta, this::resolveFall);
                break;
            case RESOLVE_FALL:
                advancePhaseTimer(delta, this::resolveRefill);
                break;
            case RESOLVE_REFILL:
                advancePhaseTimer(delta, this::continueCascadeOrFinish);
                break;
            case SHUFFLE:
                advancePhaseTimer(delta, this::performShuffle);
                break;
        }
    }

    private void advancePhaseTimer(float delta, Runnable completeAction) {
        phaseTimer -= delta;
        if (phaseTimer <= 0f) {
            completeAction.run();
        }
    }

    private void handleSwapResolved() {
        SwapData swapData = swapSystem.getPendingSwap();
        if (swapData == null) {
            stateMachine.setBoardPhase(BoardPhase.IDLE);
            return;
        }
        Cell firstCell = boardManager.getCell(swapData.getFirst().row, swapData.getFirst().col);
        Cell secondCell = boardManager.getCell(swapData.getSecond().row, swapData.getSecond().col);
        if (firstCell.getSpecialType() != SpecialType.NONE || secondCell.getSpecialType() != SpecialType.NONE) {
            levelManager.useMove();
            clearFromSpecialSwap(swapData);
            emitUiChange();
            return;
        }
        List<MatchGroup> matches = matchDetector.detect(boardManager);
        if (matches.isEmpty()) {
            boardManager.swap(swapData.getFirst(), swapData.getSecond());
            phaseTimer = 0.14f;
            stateMachine.setBoardPhase(BoardPhase.SWAP_BACK);
            soundController.playInvalid();
        } else {
            levelManager.useMove();
            chainDepth = 1;
            clearFromMatches(matches, swapData.getFirst(), swapData.getSecond());
        }
        emitUiChange();
    }

    private void finishSwapBack() {
        swapSystem.clearPendingSwap();
        swapStart = null;
        swapEnd = null;
        stateMachine.setBoardPhase(BoardPhase.IDLE);
        emitUiChange();
    }

    private void clearFromSpecialSwap(SwapData swapData) {
        Set<Position> positions = specialEffectSystem.resolveSpecialSwap(boardManager, swapData.getFirst(), swapData.getSecond());
        clearPositionsWithEffects(positions, 2);
    }

    private void clearFromMatches(List<MatchGroup> matches, Position firstSwap, Position secondSwap) {
        Set<Position> toClear = new HashSet<>();
        int generatedSpecials = 0;
        for (MatchGroup group : matches) {
            toClear.addAll(group.getPositions());
            SpecialType generated = specialEffectSystem.chooseGeneratedSpecial(group);
            Position anchor = chooseAnchor(group, firstSwap, secondSwap);
            if (generated != SpecialType.NONE && anchor != null) {
                Cell anchorCell = boardManager.getCell(anchor.row, anchor.col);
                anchorCell.setSpecialType(generated);
                toClear.remove(anchor);
                generatedSpecials++;
            }
        }
        Set<Position> expanded = specialEffectSystem.expandTriggeredTiles(boardManager, toClear, secondSwap, null);
        clearPositionsWithEffects(expanded, generatedSpecials);
    }

    private Position chooseAnchor(MatchGroup group, Position firstSwap, Position secondSwap) {
        if (group.getPositions().contains(secondSwap)) {
            return secondSwap;
        }
        if (group.getPositions().contains(firstSwap)) {
            return firstSwap;
        }
        return group.getPositions().isEmpty() ? null : group.getPositions().get(group.getPositions().size() / 2);
    }

    private void clearPositionsWithEffects(Set<Position> positions, int generatedSpecials) {
        ClearSummary summary = new ClearSummary();
        Set<Position> expanded = specialEffectSystem.expandTriggeredTiles(boardManager, positions, swapEnd, null);
        int triggeredSpecials = specialEffectSystem.clearPositions(boardManager, expanded, summary);
        obstacleSystem.applyAdjacentHits(boardManager, expanded, summary);
        scoreManager.addForClear(summary.getTotalTiles(), chainDepth, generatedSpecials + triggeredSpecials);
        updateGoals(summary);
        levelManager.updateScoreGoal(scoreManager.getScore());
        soundController.playClear();
        phaseTimer = 0.12f;
        stateMachine.setBoardPhase(BoardPhase.RESOLVE_CLEAR);
        effectManager.add(new VisualEffect(0.5f, 0.42f, 0.33f, generatedSpecials + triggeredSpecials > 0 ? specialEffectColor : boardAccentColor, 0.26f, "x" + summary.getTotalTiles()));
        if (chainDepth > 1) {
            effectManager.add(new VisualEffect(0.5f, 0.56f, 0.28f, successEffectColor, 0.3f, "Chain " + chainDepth));
            soundController.playCascade();
        }
        if (levelManager.areGoalsComplete()) {
            finishRun(true);
        }
    }

    private void resolveFall() {
        fallSystem.apply(boardManager);
        phaseTimer = 0.12f;
        stateMachine.setBoardPhase(BoardPhase.RESOLVE_FALL);
        emitUiChange();
    }

    private void resolveRefill() {
        phaseTimer = 0.1f;
        stateMachine.setBoardPhase(BoardPhase.RESOLVE_REFILL);
        emitUiChange();
    }

    private void continueCascadeOrFinish() {
        List<MatchGroup> nextMatches = matchDetector.detect(boardManager);
        if (!nextMatches.isEmpty()) {
            chainDepth++;
            clearFromMatches(nextMatches, swapStart, swapEnd);
        } else {
            swapSystem.clearPendingSwap();
            swapStart = null;
            swapEnd = null;
            stateMachine.setBoardPhase(BoardPhase.IDLE);
            if (!boardManager.hasAnyMove()) {
                stateMachine.setBoardPhase(BoardPhase.SHUFFLE);
                phaseTimer = 0.25f;
            }
        }
        emitUiChange();
    }

    private void performShuffle() {
        boardManager.shuffleUntilPlayable();
        stateMachine.setBoardPhase(BoardPhase.IDLE);
        effectManager.add(new VisualEffect(0.5f, 0.5f, 0.38f, warningEffectColor, 0.28f, "New Board"));
        emitUiChange();
    }

    private void updateGoals(ClearSummary summary) {
        for (GoalDefinition goal : levelManager.getGoals()) {
            if (goal.getGoalType() == GoalType.CLEAR_COLOR && goal.getTileColor() != null) {
                goal.addProgress(summary.getColorCount(goal.getTileColor()));
            } else if (goal.getGoalType() == GoalType.CLEAR_OBSTACLE && goal.getObstacleType() != null) {
                goal.addProgress(summary.getObstacleCount(goal.getObstacleType()));
            } else if (goal.getGoalType() == GoalType.DROP_OBJECT) {
                goal.addProgress(summary.getDroppedTargets());
            } else if (goal.getGoalType() == GoalType.CLEAR_ALL_CRATES) {
                int left = boardManager.countObstacle(ObstacleType.CRATE);
                goal.setProgress(goal.getTarget() - left);
            }
        }
    }

    private void finishRun(boolean win) {
        lastResultWin = win;
        resultSummary = "Score " + scoreManager.getScore() + "   " + levelManager.buildGoalSummary();
        stateMachine.setState(GameState.GAME_OVER);
        soundController.playResult(win);
        emitUiChange();
    }

    private void emitUiChange() {
        if (eventListener != null) {
            eventListener.onUiChanged();
        }
    }

    public BoardManager getBoardManager() {
        return boardManager;
    }

    public GameStateMachine getStateMachine() {
        return stateMachine;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public Position getSwapStart() {
        return swapStart;
    }

    public Position getSwapEnd() {
        return swapEnd;
    }

    public boolean getLastResultWin() {
        return lastResultWin;
    }

    public String getResultSummary() {
        return resultSummary;
    }
}
