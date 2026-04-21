package com.android.boot.core;

import java.util.ArrayList;
import java.util.List;

public class AchievementManager {
    public final List<Achievement> list = new ArrayList<>();

    public AchievementManager() {
        add("care_20","Feed 20 animals","Care",20,80);
        add("care_200","Feed 200 animals","Care",200,400);
        add("clean_20","Clean 20 pens","Care",20,80);
        add("clean_200","Clean 200 pens","Care",200,500);
        add("collect_50","Collect 50 goods","Collection",50,120);
        add("collect_500","Collect 500 goods","Collection",500,650);
        add("deliver_5","Complete 5 deliveries","Delivery",5,120);
        add("deliver_50","Complete 50 deliveries","Delivery",50,700);
        add("combo_10","Reach combo x10","Mastery",10,160);
        add("combo_25","Reach combo x25","Mastery",25,700);
        add("coins_1000","Earn 1000 coins","Wealth",1000,120);
        add("coins_10000","Earn 10000 coins","Wealth",10000,1000);
        add("unlock_3","Unlock 3 species","Expansion",3,180);
        add("unlock_8","Unlock 8 species","Expansion",8,750);
        add("unlock_all","Unlock all species","Expansion",12,1400);
        add("upgrade_3","Upgrade 3 pens","Expansion",3,180);
        add("upgrade_10","Upgrade 10 pens","Expansion",10,800);
        add("level_5","Reach ranch level 5","Mastery",5,210);
        add("level_10","Reach ranch level 10","Mastery",10,900);
        add("neglect_guard","Keep neglect low for 3 minutes","Care",180,420);
        add("urgent_chain","Finish urgent request chain","Delivery",3,380);
        add("fill_pens","Fill all active pens","Collection",6,320);
        add("triple_chain","Collect from 3 pens in one streak","Collection",3,260);
        add("clean_session","Finish session with no game over","Mastery",1,420);
    }

    private void add(String id, String title, String category, int target, int rewardCoins) {
        list.add(new Achievement(id, title, category, target, rewardCoins));
    }
}
