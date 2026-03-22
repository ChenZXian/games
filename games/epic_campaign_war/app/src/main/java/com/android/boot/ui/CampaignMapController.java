package com.android.boot.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.boot.R;
import com.android.boot.core.CampaignProgress;
import com.android.boot.core.ChapterData;

import java.util.List;

public class CampaignMapController {
    public interface ChapterSelectionListener {
        void onChapterSelected(int chapterIndex);
    }

    private final Context context;
    private final LinearLayout chapterList;
    private final TextView campaignProgressView;
    private final CampaignProgress campaignProgress;
    private final List<ChapterData> chapters;
    private final ChapterSelectionListener listener;

    public CampaignMapController(Context context, LinearLayout chapterList, TextView campaignProgressView, CampaignProgress campaignProgress, List<ChapterData> chapters, ChapterSelectionListener listener) {
        this.context = context;
        this.chapterList = chapterList;
        this.campaignProgressView = campaignProgressView;
        this.campaignProgress = campaignProgress;
        this.chapters = chapters;
        this.listener = listener;
    }

    public void rebuild() {
        chapterList.removeAllViews();
        int highest = campaignProgress.getHighestUnlockedChapter();
        campaignProgressView.setText("Unlocked chapters: " + highest + " / " + chapters.size());
        for (ChapterData chapter : chapters) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.ui_card);
            int pad = context.getResources().getDimensionPixelSize(R.dimen.cst_pad_12);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.cst_pad_12);
            card.setLayoutParams(params);
            card.setPadding(pad, pad, pad, pad);

            TextView title = new TextView(context, null, 0, R.style.TextAppearance_Game_Subtitle);
            title.setText(chapter.chapterName);
            card.addView(title);

            TextView subtitle = new TextView(context, null, 0, R.style.TextAppearance_Game_Body);
            subtitle.setText(chapter.subtitle);
            card.addView(subtitle);

            LinearLayout pathRow = new LinearLayout(context);
            pathRow.setOrientation(LinearLayout.HORIZONTAL);
            pathRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = context.getResources().getDimensionPixelSize(R.dimen.cst_pad_8);
            pathRow.setLayoutParams(rowParams);

            TextView nodeState = new TextView(context, null, 0, R.style.Widget_Game_Badge);
            if (campaignProgress.isCleared(chapter.index)) {
                nodeState.setText("Cleared");
            } else if (chapter.index <= highest) {
                nodeState.setText(chapter.index == highest ? "Current Target" : "Unlocked");
            } else {
                nodeState.setText("Locked");
            }
            pathRow.addView(nodeState);

            TextView route = new TextView(context, null, 0, R.style.TextAppearance_Game_Caption);
            route.setText("  Route node  ->  battle node  ->  stronghold");
            pathRow.addView(route);
            card.addView(pathRow);

            Button startButton = new Button(context, null, 0, R.style.Widget_Game_Button_Primary);
            startButton.setText(chapter.index <= highest ? "Start " + chapter.battleTitle : "Locked");
            startButton.setEnabled(chapter.index <= highest);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.topMargin = context.getResources().getDimensionPixelSize(R.dimen.cst_pad_10);
            startButton.setLayoutParams(buttonParams);
            startButton.setOnClickListener(v -> {
                campaignProgress.setSelectedChapter(chapter.index);
                listener.onChapterSelected(chapter.index);
            });
            card.addView(startButton);
            chapterList.addView(card);
        }
    }
}
