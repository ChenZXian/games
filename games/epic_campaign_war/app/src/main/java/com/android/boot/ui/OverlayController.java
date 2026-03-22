package com.android.boot.ui;

import android.view.View;

import com.android.boot.core.MetaState;

public class OverlayController {
    private final View menuOverlay;
    private final View campaignOverlay;
    private final View prepOverlay;
    private final View pauseOverlay;
    private final View resultOverlay;
    private final View helpOverlay;
    private final View hudRoot;
    private final View actionRoot;

    public OverlayController(View menuOverlay, View campaignOverlay, View prepOverlay, View pauseOverlay, View resultOverlay, View helpOverlay, View hudRoot, View actionRoot) {
        this.menuOverlay = menuOverlay;
        this.campaignOverlay = campaignOverlay;
        this.prepOverlay = prepOverlay;
        this.pauseOverlay = pauseOverlay;
        this.resultOverlay = resultOverlay;
        this.helpOverlay = helpOverlay;
        this.hudRoot = hudRoot;
        this.actionRoot = actionRoot;
    }

    public void showState(MetaState metaState) {
        setVisible(menuOverlay, metaState == MetaState.MENU_HOME);
        setVisible(campaignOverlay, metaState == MetaState.CAMPAIGN_MAP);
        setVisible(prepOverlay, metaState == MetaState.CHAPTER_PREP);
        setVisible(pauseOverlay, metaState == MetaState.PAUSED);
        setVisible(resultOverlay, metaState == MetaState.GAME_OVER);
        setVisible(hudRoot, metaState == MetaState.PLAYING || metaState == MetaState.PAUSED || metaState == MetaState.GAME_OVER);
        setVisible(actionRoot, metaState == MetaState.PLAYING || metaState == MetaState.PAUSED || metaState == MetaState.GAME_OVER);
        if (metaState != MetaState.PAUSED && metaState != MetaState.GAME_OVER) {
            setVisible(pauseOverlay, false);
            setVisible(resultOverlay, false);
        }
    }

    public void showHelp(boolean show) {
        setVisible(helpOverlay, show);
    }

    private void setVisible(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
