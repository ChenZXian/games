package com.android.boot.core;

import com.android.boot.ai.AiController;
import com.android.boot.ai.AiEvaluator;
import com.android.boot.ai.KnowledgeModel;
import com.android.boot.entity.BattleOutcome;
import com.android.boot.entity.BattleResult;
import com.android.boot.entity.Cell;
import com.android.boot.entity.Move;
import com.android.boot.entity.Piece;
import com.android.boot.entity.Side;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private final Board board = new Board();
    private final DeploymentSystem deploymentSystem = new DeploymentSystem();
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final BattleResolver battleResolver = new BattleResolver();
    private final VictorySystem victorySystem = new VictorySystem();
    private final KnowledgeModel knowledgeModel = new KnowledgeModel();
    private final AiController aiController = new AiController(moveGenerator, new AiEvaluator(knowledgeModel));
    private final List<Piece> playerArmy = PieceSetFactory.createArmy(Side.PLAYER);
    private final List<Piece> aiArmy = PieceSetFactory.createArmy(Side.AI);

    private GameState state = GameState.MENU;
    private Side turn = Side.PLAYER;
    private String status = "Ready";
    private String result = "";
    private Cell selected;
    private List<Move> highlightedMoves = new ArrayList<>();
    private Cell swapAnchor;
    private boolean aiThinking;
    private GameState pausedFromState = GameState.MENU;

    public GameManager() {
        deploymentSystem.randomFlipDeploy(board, playerArmy, aiArmy);
    }

    public void startMatch() {
        resetBoard();
        state = GameState.PLAYING;
        turn = Side.PLAYER;
        status = "Flip a piece or move a revealed unit";
        result = "";
        selected = null;
        swapAnchor = null;
        highlightedMoves.clear();
        aiThinking = false;
    }

    private void resetBoard() {
        for (Cell cell : board.allCells()) {
            cell.piece = null;
        }
        knowledgeModel.reset();
        playerArmy.clear();
        aiArmy.clear();
        playerArmy.addAll(PieceSetFactory.createArmy(Side.PLAYER));
        aiArmy.addAll(PieceSetFactory.createArmy(Side.AI));
        deploymentSystem.randomFlipDeploy(board, playerArmy, aiArmy);
    }

    public Board getBoard() { return board; }
    public GameState getState() { return state; }
    public Side getTurn() { return turn; }
    public String getStatus() { return status; }
    public String getResult() { return result; }
    public Cell getSelected() { return selected; }
    public List<Move> getHighlightedMoves() { return highlightedMoves; }
    public boolean isAiThinking() { return aiThinking; }

    public void autoDeployPlayer() {
        if (state != GameState.DEPLOYMENT) {
            return;
        }
        deploymentSystem.autoDeploy(board, Side.PLAYER, playerArmy);
        status = "Player formation randomized";
        swapAnchor = null;
    }

    public void beginBattle() {
        if (state != GameState.DEPLOYMENT) {
            return;
        }
        state = GameState.PLAYING;
        turn = Side.PLAYER;
        status = "Battle started";
    }

    public void pause() {
        if (state == GameState.PLAYING || state == GameState.DEPLOYMENT) {
            pausedFromState = state;
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = result.isEmpty() ? pausedFromState : GameState.GAME_OVER;
        }
    }

    public void backToMenu() {
        state = GameState.MENU;
        result = "";
        status = "Ready";
        selected = null;
        highlightedMoves.clear();
    }

    public void onCellTapped(int row, int col) {
        Cell cell = board.get(row, col);
        if (cell == null) {
            return;
        }
        if (state == GameState.DEPLOYMENT) {
            handleDeploymentTap(cell);
            return;
        }
        if (state != GameState.PLAYING || turn != Side.PLAYER || aiThinking) {
            return;
        }
        if (cell.piece != null && !cell.piece.isKnownBy(Side.PLAYER)) {
            revealOpen(cell.piece);
            selected = null;
            highlightedMoves = new ArrayList<>();
            status = "Flipped " + cell.piece.getType().getLabel();
            endTurn();
            return;
        }
        if (selected != null) {
            for (Move move : highlightedMoves) {
                if (move.toRow == row && move.toCol == col) {
                    executeMove(move);
                    selected = null;
                    highlightedMoves = new ArrayList<>();
                    return;
                }
            }
        }
        if (cell.piece != null && cell.piece.getSide() == Side.PLAYER && cell.piece.isKnownBy(Side.PLAYER)) {
            selected = cell;
            highlightedMoves = moveGenerator.generateForPiece(board, row, col);
            status = highlightedMoves.isEmpty() ? "No legal move for that piece" : "Choose a target cell";
        } else {
            selected = null;
            highlightedMoves = new ArrayList<>();
        }
    }

    private void handleDeploymentTap(Cell cell) {
        if (!board.inPlayerZone(cell.row) || cell.piece == null || cell.piece.getSide() != Side.PLAYER) {
            status = "Deployment stays inside your half";
            return;
        }
        if (swapAnchor == null) {
            swapAnchor = cell;
            status = "Select another piece to swap";
            return;
        }
        if (swapAnchor == cell) {
            swapAnchor = null;
            status = "Selection cleared";
            return;
        }
        if (deploymentSystem.canPlaceInCell(board, Side.PLAYER, swapAnchor.piece, cell) && deploymentSystem.canPlaceInCell(board, Side.PLAYER, cell.piece, swapAnchor)) {
            deploymentSystem.swap(swapAnchor, cell);
            status = "Formation updated";
        } else {
            status = "Illegal swap for current rule set";
        }
        swapAnchor = null;
    }

    public void update(float deltaSeconds) {
        if (state == GameState.PLAYING && turn == Side.AI && !aiThinking && result.isEmpty()) {
            aiThinking = true;
        }
    }

    public void maybeRunAi() {
        if (state != GameState.PLAYING || turn != Side.AI || result.length() > 0 || !aiThinking) {
            return;
        }
        Move move = aiController.chooseMove(board);
        aiThinking = false;
        if (move != null) {
            executeMove(move);
            return;
        }
        if (flipHiddenForAi()) {
            endTurn();
            return;
        }
        finishGame("AI has no legal moves");
    }

    private void executeMove(Move move) {
        Cell from = board.get(move.fromRow, move.fromCol);
        Cell to = board.get(move.toRow, move.toCol);
        Piece attacker = from.piece;
        if (attacker == null) {
            return;
        }
        if (to.piece == null) {
            to.piece = attacker;
            from.piece = null;
            status = attacker.getType().getLabel() + " moved";
        } else {
            Piece defender = to.piece;
            reveal(attacker, defender);
            BattleResult battle = battleResolver.resolve(attacker, defender);
            switch (battle.outcome) {
                case ATTACKER_WINS:
                case CAPTURE_FLAG:
                    defender.setAlive(false);
                    to.piece = attacker;
                    from.piece = null;
                    break;
                case DEFENDER_WINS:
                    attacker.setAlive(false);
                    from.piece = null;
                    break;
                case BOTH_DIE:
                    attacker.setAlive(false);
                    defender.setAlive(false);
                    from.piece = null;
                    to.piece = null;
                    break;
            }
            status = battle.message;
            if (battle.outcome == BattleOutcome.CAPTURE_FLAG) {
                finishGame(attacker.getSide() == Side.PLAYER ? "You captured the enemy flag" : "AI captured your flag");
                return;
            }
        }
        String victory = victorySystem.checkVictory(board, moveGenerator);
        if (victory != null) {
            finishGame(victory);
            return;
        }
        endTurn();
    }

    private void reveal(Piece attacker, Piece defender) {
        boolean attackerKnownByAi = attacker.isKnownBy(Side.AI);
        boolean defenderKnownByAi = defender.isKnownBy(Side.AI);
        attacker.revealTo(Side.PLAYER);
        attacker.revealTo(Side.AI);
        defender.revealTo(Side.PLAYER);
        defender.revealTo(Side.AI);
        if (defender.getSide() == Side.PLAYER && !defenderKnownByAi) {
            knowledgeModel.onReveal(defender);
        }
        if (attacker.getSide() == Side.PLAYER && !attackerKnownByAi) {
            knowledgeModel.onReveal(attacker);
        }
    }

    private void revealOpen(Piece piece) {
        piece.revealTo(Side.PLAYER);
        piece.revealTo(Side.AI);
        if (piece.getSide() == Side.PLAYER) {
            knowledgeModel.onReveal(piece);
        }
    }

    private boolean flipHiddenForAi() {
        Cell best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Cell cell : board.allCells()) {
            if (cell.piece != null && !cell.piece.isKnownBy(Side.AI)) {
                float score = evaluateAiFlipCell(cell);
                if (score > bestScore) {
                    bestScore = score;
                    best = cell;
                }
            }
        }
        if (best == null) {
            return false;
        }
        revealOpen(best.piece);
        status = "AI flipped a piece";
        return true;
    }

    private float evaluateAiFlipCell(Cell cell) {
        float score = 0f;
        // Push toward frontline and center files for better initiative.
        score += cell.row * 2.2f;
        score += (2 - Math.abs(2 - cell.col)) * 1.8f;
        if (cell.type == com.android.boot.entity.CellType.RAIL) score += 3f;
        if (cell.type == com.android.boot.entity.CellType.HEADQUARTERS) score -= 4f;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                Cell near = board.get(cell.row + dr, cell.col + dc);
                if (near != null && near.piece != null && near.piece.getSide() == Side.PLAYER && near.piece.isKnownBy(Side.AI)) {
                    score += 4.5f;
                }
            }
        }
        return score;
    }

    private void endTurn() {
        String victory = victorySystem.checkVictory(board, moveGenerator);
        if (victory != null) {
            finishGame(victory);
            return;
        }
        turn = turn.opponent();
    }

    private void finishGame(String reason) {
        result = reason;
        status = reason;
        state = GameState.GAME_OVER;
        aiThinking = false;
    }
}
