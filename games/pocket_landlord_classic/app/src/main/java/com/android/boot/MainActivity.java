package com.android.boot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.boot.audio.AudioController;
import com.android.boot.core.CampaignStage;
import com.android.boot.core.GamePreferences;
import com.android.boot.core.LandlordGame;
import com.android.boot.core.Move;
import com.android.boot.ui.GameTableView;

public class MainActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LandlordGame game = new LandlordGame();
    private GamePreferences prefs;
    private AudioController audio;
    private GameTableView gameView;
    private TextView subtitleView;
    private TextView chapterView;
    private TextView starView;
    private TextView menuInfoView;
    private TextView resultTitleView;
    private TextView resultBodyView;
    private LinearLayout menuOverlay;
    private LinearLayout pauseOverlay;
    private LinearLayout resultOverlay;
    private LinearLayout helpOverlay;
    private Button actionOneButton;
    private Button actionTwoButton;
    private Button actionThreeButton;
    private Button actionFourButton;
    private ImageButton pauseIconButton;
    private LandlordGame.RoundResult lastResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new GamePreferences(this);
        audio = new AudioController(this);
        audio.setMuted(prefs.isMuted());
        bindViews();
        bindActions();
        updateMenuInfo();
        showMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (game.getPhase() == LandlordGame.Phase.MENU) {
            audio.playMusic("audio/bgm_menu.wav", true);
        } else if (game.getPhase() == LandlordGame.Phase.PLAYING || game.getPhase() == LandlordGame.Phase.BIDDING) {
            audio.playMusic("audio/bgm_play.wav", true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        audio.stopMusic();
    }

    @Override
    protected void onDestroy() {
        audio.release();
        super.onDestroy();
    }

    private void bindViews() {
        gameView = findViewById(R.id.gameView);
        subtitleView = findViewById(R.id.subtitleView);
        chapterView = findViewById(R.id.chapterView);
        starView = findViewById(R.id.starView);
        menuInfoView = findViewById(R.id.menuInfoView);
        resultTitleView = findViewById(R.id.resultTitleView);
        resultBodyView = findViewById(R.id.resultBodyView);
        menuOverlay = findViewById(R.id.menuOverlay);
        pauseOverlay = findViewById(R.id.pauseOverlay);
        resultOverlay = findViewById(R.id.resultOverlay);
        helpOverlay = findViewById(R.id.helpOverlay);
        actionOneButton = findViewById(R.id.actionOneButton);
        actionTwoButton = findViewById(R.id.actionTwoButton);
        actionThreeButton = findViewById(R.id.actionThreeButton);
        actionFourButton = findViewById(R.id.actionFourButton);
        pauseIconButton = findViewById(R.id.pauseIconButton);
        gameView.setListener(new GameTableView.Listener() {
            @Override
            public void onSelectionChanged(int count, String pattern) {
                if (game.getPhase() == LandlordGame.Phase.PLAYING && game.isPlayerTurn()) {
                    subtitleView.setText(pattern + "  Selected " + count);
                }
            }
        });
    }

    private void bindActions() {
        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCampaign();
            }
        });
        findViewById(R.id.drillButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDrill();
            }
        });
        findViewById(R.id.howToPlayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpOverlay.setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.helpCloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpOverlay.setVisibility(View.GONE);
            }
        });
        findViewById(R.id.resumeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseOverlay.setVisibility(View.GONE);
                scheduleAiIfNeeded();
            }
        });
        findViewById(R.id.restartButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultOverlay.setVisibility(View.GONE);
                pauseOverlay.setVisibility(View.GONE);
                game.restartRound();
                gameView.clearSelection();
                audio.playMusic("audio/bgm_play.wav", true);
                refreshUi();
                scheduleAiIfNeeded();
            }
        });
        View.OnClickListener toMenu = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        };
        findViewById(R.id.menuButton).setOnClickListener(toMenu);
        findViewById(R.id.resultMenuButton).setOnClickListener(toMenu);
        findViewById(R.id.nextButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultOverlay.setVisibility(View.GONE);
                if (game.getCurrentStage() != null && game.getCurrentStage().drill) {
                    game.startDrillStage(0);
                } else if (lastResult != null && lastResult.playerWon && game.canAdvanceStage()) {
                    game.advanceStage();
                } else {
                    game.restartRound();
                }
                gameView.clearSelection();
                audio.playMusic("audio/bgm_play.wav", true);
                refreshUi();
                scheduleAiIfNeeded();
            }
        });
        pauseIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (game.getPhase() == LandlordGame.Phase.PLAYING || game.getPhase() == LandlordGame.Phase.BIDDING) {
                    pauseOverlay.setVisibility(View.VISIBLE);
                }
            }
        });
        actionOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAction(0);
            }
        });
        actionTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAction(1);
            }
        });
        actionThreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAction(2);
            }
        });
        actionFourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAction(3);
            }
        });
    }

    private void handleAction(int slot) {
        if (menuOverlay.getVisibility() == View.VISIBLE || pauseOverlay.getVisibility() == View.VISIBLE || resultOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        if (game.getPhase() == LandlordGame.Phase.BIDDING) {
            int bid = slot == 0 ? 1 : slot == 1 ? 2 : slot == 2 ? 3 : 0;
            if (!game.playerBid(bid)) {
                return;
            }
            audio.playSfx("click", "audio/sfx_click.wav");
            refreshUi();
            scheduleAiIfNeeded();
            return;
        }
        if (game.getPhase() != LandlordGame.Phase.PLAYING || !game.isPlayerTurn()) {
            return;
        }
        if (slot == 0) {
            if (!game.playerPlay(gameView.getSelectedCards())) {
                toast("Choose a legal pattern");
                return;
            }
            audio.playSfx("play", "audio/sfx_collect.wav");
            gameView.clearSelection();
        } else if (slot == 1) {
            if (!game.playerPass()) {
                toast("You lead this trick");
                return;
            }
            audio.playSfx("pass", "audio/sfx_warning.wav");
            gameView.clearSelection();
        } else if (slot == 2) {
            Move hint = game.getHint();
            if (hint == null) {
                toast("No legal reply");
                return;
            }
            gameView.setHintMove(hint);
            audio.playSfx("hint", "audio/sfx_click.wav");
            return;
        } else {
            gameView.clearSelection();
        }
        refreshUi();
        scheduleAiIfNeeded();
    }

    private void startCampaign() {
        int index = prefs.getUnlockedStage();
        game.startCampaignStage(index);
        gameView.clearSelection();
        audio.playMusic("audio/bgm_play.wav", true);
        menuOverlay.setVisibility(View.GONE);
        pauseOverlay.setVisibility(View.GONE);
        resultOverlay.setVisibility(View.GONE);
        refreshUi();
        scheduleAiIfNeeded();
    }

    private void startDrill() {
        game.startDrillStage(0);
        gameView.clearSelection();
        audio.playMusic("audio/bgm_play.wav", true);
        menuOverlay.setVisibility(View.GONE);
        pauseOverlay.setVisibility(View.GONE);
        resultOverlay.setVisibility(View.GONE);
        refreshUi();
        scheduleAiIfNeeded();
    }

    private void showMenu() {
        handler.removeCallbacksAndMessages(null);
        menuOverlay.setVisibility(View.VISIBLE);
        pauseOverlay.setVisibility(View.GONE);
        resultOverlay.setVisibility(View.GONE);
        helpOverlay.setVisibility(View.GONE);
        chapterView.setText("Hall " + ((prefs.getUnlockedStage() / 12) + 1));
        subtitleView.setText("Offline campaign tables and drills");
        starView.setText("Stars " + prefs.getStars());
        audio.playMusic("audio/bgm_menu.wav", true);
        updateMenuInfo();
    }

    private void scheduleAiIfNeeded() {
        handler.removeCallbacksAndMessages(null);
        if (pauseOverlay.getVisibility() == View.VISIBLE || resultOverlay.getVisibility() == View.VISIBLE || menuOverlay.getVisibility() == View.VISIBLE) {
            return;
        }
        if (game.getPhase() == LandlordGame.Phase.BIDDING || game.getPhase() == LandlordGame.Phase.PLAYING) {
            if (!game.isPlayerTurn()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String action = game.takeAiTurn();
                        if (action.length() > 0) {
                            subtitleView.setText("AI " + action);
                        }
                        refreshUi();
                        scheduleAiIfNeeded();
                    }
                }, 550L);
            }
        }
    }

    private void refreshUi() {
        gameView.setGame(game);
        CampaignStage stage = game.getCurrentStage();
        if (stage != null) {
            chapterView.setText(stage.hallName);
            subtitleView.setText(stage.displayName + "  " + stage.modifier);
        }
        starView.setText("Stars " + prefs.getStars());
        updateActionButtons();
        LandlordGame.RoundResult result = game.consumePendingResult();
        if (result != null) {
            lastResult = result;
            prefs.addCoins(result.coins);
            if (result.playerWon) {
                prefs.addStars(result.stars);
                if (!game.getCurrentStage().drill) {
                    int unlocked = prefs.getUnlockedStage();
                    int next = Math.max(unlocked, game.getStagePointer() + 1);
                    prefs.setUnlockedStage(Math.min(next, game.getCampaignStages().size() - 1));
                }
            }
            resultTitleView.setText(result.title);
            resultBodyView.setText(result.body);
            resultOverlay.setVisibility(View.VISIBLE);
            audio.playSfx(result.playerWon ? "win" : "fail", result.playerWon ? "audio/sfx_win.wav" : "audio/sfx_fail.wav");
            updateMenuInfo();
        }
    }

    private void updateActionButtons() {
        if (game.getPhase() == LandlordGame.Phase.BIDDING) {
            actionOneButton.setText(R.string.btn_call_one);
            actionTwoButton.setText(R.string.btn_call_two);
            actionThreeButton.setText(R.string.btn_call_three);
            actionFourButton.setText(R.string.btn_pass_bid);
        } else {
            actionOneButton.setText(R.string.btn_play);
            actionTwoButton.setText(R.string.btn_pass);
            actionThreeButton.setText(R.string.btn_hint);
            actionFourButton.setText(R.string.btn_clear);
        }
        boolean enabled = menuOverlay.getVisibility() != View.VISIBLE && pauseOverlay.getVisibility() != View.VISIBLE && resultOverlay.getVisibility() != View.VISIBLE;
        actionOneButton.setEnabled(enabled);
        actionTwoButton.setEnabled(enabled);
        actionThreeButton.setEnabled(enabled);
        actionFourButton.setEnabled(enabled);
    }

    private void updateMenuInfo() {
        int unlocked = prefs.getUnlockedStage() + 1;
        menuInfoView.setText("Unlocked Table " + unlocked + "  Total Stars " + prefs.getStars() + "  Coins " + prefs.getCoins());
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
