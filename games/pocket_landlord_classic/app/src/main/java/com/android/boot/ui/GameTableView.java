package com.android.boot.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.android.boot.R;
import com.android.boot.core.Card;
import com.android.boot.core.LandlordGame;
import com.android.boot.core.Move;
import com.android.boot.core.SeatState;

import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameTableView extends View {
    public interface Listener {
        void onSelectionChanged(int count, String pattern);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Map<Integer, RectF> cardHitBoxes = new HashMap<>();
    private final Set<Integer> selectedIds = new HashSet<>();
    private final Set<Integer> hintIds = new HashSet<>();
    private Bitmap feltBadge;
    private Bitmap leadToken;
    private Bitmap allianceSeal;
    private boolean artLoaded;
    private LandlordGame game;
    private Listener listener;

    public GameTableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setGame(LandlordGame game) {
        this.game = game;
        ensureArt();
        invalidate();
    }

    public void clearSelection() {
        selectedIds.clear();
        hintIds.clear();
        notifySelection();
        invalidate();
    }

    public void setHintMove(Move move) {
        hintIds.clear();
        selectedIds.clear();
        if (move != null) {
            for (Card card : move.cards) {
                hintIds.add(card.id);
                selectedIds.add(card.id);
            }
        }
        notifySelection();
        invalidate();
    }

    public List<Card> getSelectedCards() {
        List<Card> cards = new ArrayList<>();
        if (game == null) {
            return cards;
        }
        for (Card card : game.getPlayerHand()) {
            if (selectedIds.contains(card.id)) {
                cards.add(card);
            }
        }
        return cards;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.cst_panel_dark));
        drawFelt(canvas);
        if (game == null || game.getCurrentStage() == null) {
            return;
        }
        cardHitBoxes.clear();
        drawSeatCounts(canvas);
        drawKitty(canvas);
        drawLastMove(canvas);
        drawOpponentHands(canvas, game.getSeat(1), getWidth() * 0.12f, getHeight() * 0.3f);
        drawOpponentHands(canvas, game.getSeat(2), getWidth() * 0.88f, getHeight() * 0.3f);
        drawPlayerHand(canvas);
        drawTurnMarker(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (game == null || !game.isPlayerTurn() || game.getPhase() != LandlordGame.Phase.PLAYING) {
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            for (Map.Entry<Integer, RectF> entry : cardHitBoxes.entrySet()) {
                if (entry.getValue().contains(event.getX(), event.getY())) {
                    if (selectedIds.contains(entry.getKey())) {
                        selectedIds.remove(entry.getKey());
                        hintIds.remove(entry.getKey());
                    } else {
                        selectedIds.add(entry.getKey());
                    }
                    notifySelection();
                    invalidate();
                    break;
                }
            }
        }
        return true;
    }

    private void drawFelt(Canvas canvas) {
        rect.set(getPaddingLeft() + 6f, getPaddingTop() + 6f, getWidth() - getPaddingRight() - 6f, getHeight() - getPaddingBottom() - 6f);
        paint.setShader(new LinearGradient(0f, rect.top, 0f, rect.bottom, ContextCompat.getColor(getContext(), R.color.cst_table_inner), ContextCompat.getColor(getContext(), R.color.cst_button_secondary_dark), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, 36f, 36f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel));
        canvas.drawRoundRect(rect, 36f, 36f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSeatCounts(Canvas canvas) {
        SeatState left = game.getSeat(1);
        SeatState right = game.getSeat(2);
        drawSeatBadge(canvas, "Left " + left.hand.size(), getWidth() * 0.08f, 40f, left.landlord);
        drawSeatBadge(canvas, "Right " + right.hand.size(), getWidth() * 0.72f, 40f, right.landlord);
        drawSeatBadge(canvas, "You " + game.getPlayerHand().size(), getWidth() * 0.38f, getHeight() - 118f, game.getSeat(0).landlord);
    }

    private void drawSeatBadge(Canvas canvas, String text, float left, float top, boolean landlord) {
        rect.set(left, top, left + 140f, top + 42f);
        paint.setColor(ContextCompat.getColor(getContext(), landlord ? R.color.cst_badge_red : R.color.cst_chip));
        canvas.drawRoundRect(rect, 18f, 18f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawRoundRect(rect, 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        paint.setTextSize(20f);
        canvas.drawText(text, left + 12f, top + 27f, paint);
    }

    private void drawKitty(Canvas canvas) {
        if (feltBadge != null) {
            canvas.drawBitmap(feltBadge, (getWidth() - feltBadge.getWidth()) * 0.5f, 14f, paint);
        }
        List<Card> kitty = game.getKitty();
        float cardW = 44f;
        float gap = 12f;
        float totalW = kitty.size() * cardW + Math.max(0, kitty.size() - 1) * gap;
        float startX = (getWidth() - totalW) * 0.5f;
        float top = 70f;
        for (int i = 0; i < kitty.size(); i++) {
            drawCardFace(canvas, kitty.get(i), startX + i * (cardW + gap), top, cardW, 60f, false, true);
        }
    }

    private void drawLastMove(Canvas canvas) {
        Move move = game.getLastMove();
        if (leadToken != null) {
            canvas.drawBitmap(leadToken, getWidth() * 0.5f - leadToken.getWidth() * 0.5f, getHeight() * 0.38f, paint);
        }
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_secondary));
        paint.setTextSize(22f);
        String label = move == null || move.isPass() ? "Open Lead" : move.label();
        canvas.drawText(label, getWidth() * 0.5f - 55f, getHeight() * 0.44f, paint);
        if (move == null || move.isPass()) {
            return;
        }
        float cardW = 48f;
        float spacing = 18f;
        float totalW = cardW + Math.max(0, move.cards.size() - 1) * spacing;
        float startX = (getWidth() - totalW) * 0.5f;
        float top = getHeight() * 0.48f;
        for (int i = 0; i < move.cards.size(); i++) {
            drawCardFace(canvas, move.cards.get(i), startX + i * spacing, top, cardW, 66f, false, false);
        }
    }

    private void drawOpponentHands(Canvas canvas, SeatState seat, float centerX, float centerY) {
        int count = seat.hand.size();
        float cardW = 34f;
        float cardH = 48f;
        float offset = Math.min(9f, count > 0 ? 130f / count : 9f);
        float startY = centerY - Math.min(100f, count * offset * 0.5f);
        for (int i = 0; i < count; i++) {
            float top = startY + i * offset;
            rect.set(centerX - cardW * 0.5f, top, centerX + cardW * 0.5f, top + cardH);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_card_back));
            canvas.drawRoundRect(rect, 10f, 10f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_panel));
            canvas.drawRoundRect(rect, 10f, 10f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawPlayerHand(Canvas canvas) {
        List<Card> hand = game.getPlayerHand();
        if (hand.isEmpty()) {
            return;
        }
        float cardW = Math.min(64f, (getWidth() - 64f) / Math.max(6f, hand.size() * 0.42f));
        float cardH = cardW * 1.4f;
        float spacing = Math.min(cardW * 0.42f, (getWidth() - cardW - 40f) / Math.max(1, hand.size() - 1));
        float totalW = cardW + Math.max(0, hand.size() - 1) * spacing;
        float startX = (getWidth() - totalW) * 0.5f;
        float baseY = getHeight() - cardH - 26f;
        for (Card card : hand) {
            boolean selected = selectedIds.contains(card.id);
            boolean hinted = hintIds.contains(card.id);
            float y = baseY - (selected ? 24f : 0f);
            drawCardFace(canvas, card, startX, y, cardW, cardH, selected, hinted);
            cardHitBoxes.put(card.id, new RectF(startX, y, startX + cardW, y + cardH));
            startX += spacing;
        }
    }

    private void drawCardFace(Canvas canvas, Card card, float left, float top, float width, float height, boolean selected, boolean hinted) {
        rect.set(left, top, left + width, top + height);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_card_shadow));
        canvas.drawRoundRect(new RectF(rect.left + 3f, rect.top + 4f, rect.right + 3f, rect.bottom + 4f), 12f, 12f, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_card_face));
        canvas.drawRoundRect(rect, 12f, 12f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? 4f : 2f);
        int edgeColor = hinted ? R.color.cst_button_primary : R.color.cst_card_edge;
        paint.setColor(ContextCompat.getColor(getContext(), edgeColor));
        canvas.drawRoundRect(rect, 12f, 12f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(width * 0.28f);
        paint.setFakeBoldText(true);
        paint.setColor(ContextCompat.getColor(getContext(), card.isRed() ? R.color.cst_red : R.color.cst_black));
        canvas.drawText(card.rankLabel(), left + width * 0.12f, top + height * 0.26f, paint);
        paint.setTextSize(width * 0.22f);
        canvas.drawText(card.suitLabel(), left + width * 0.12f, top + height * 0.43f, paint);
        paint.setTextSize(width * 0.33f);
        String mid = card.rank >= Card.RANK_SMALL_JOKER ? "J" : card.suitLabel();
        canvas.drawText(mid, left + width * 0.39f, top + height * 0.63f, paint);
        paint.setFakeBoldText(false);
    }

    private void drawTurnMarker(Canvas canvas) {
        String text;
        if (game.getPhase() == LandlordGame.Phase.BIDDING) {
            text = game.isPlayerTurn() ? "Your bid" : "AI bidding";
        } else if (game.getPhase() == LandlordGame.Phase.PLAYING) {
            text = game.isPlayerTurn() ? "Your turn" : "AI turn";
        } else {
            text = "Round settled";
        }
        paint.setTextSize(26f);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.cst_text_primary));
        canvas.drawText(text, 32f, getHeight() * 0.42f, paint);
        if (allianceSeal != null && game.getLandlordSeat() == 0) {
            canvas.drawBitmap(allianceSeal, getWidth() - allianceSeal.getWidth() - 28f, getHeight() - allianceSeal.getHeight() - 142f, paint);
        }
    }

    private void ensureArt() {
        if (artLoaded) {
            return;
        }
        artLoaded = true;
        try {
            feltBadge = BitmapFactory.decodeStream(getContext().getAssets().open("game_art/felt_badge.png"));
            leadToken = BitmapFactory.decodeStream(getContext().getAssets().open("game_art/lead_token.png"));
            allianceSeal = BitmapFactory.decodeStream(getContext().getAssets().open("game_art/alliance_seal.png"));
        } catch (IOException ignored) {
            feltBadge = null;
            leadToken = null;
            allianceSeal = null;
        }
    }

    private void notifySelection() {
        if (listener == null) {
            return;
        }
        Move move = Move.pass();
        List<Card> selected = getSelectedCards();
        if (!selected.isEmpty()) {
            Move parsed = com.android.boot.core.RuleEngine.identify(selected);
            if (parsed != null) {
                move = parsed;
            } else {
                move = null;
            }
        }
        listener.onSelectionChanged(selected.size(), move == null ? "Illegal" : move.label());
    }
}
