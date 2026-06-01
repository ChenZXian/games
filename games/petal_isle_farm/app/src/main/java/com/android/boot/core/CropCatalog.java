package com.android.boot.core;

import android.graphics.Color;

import com.android.boot.entity.CropType;

import java.util.ArrayList;
import java.util.List;

public class CropCatalog {
    private final List<CropType> crops = new ArrayList<>();

    public CropCatalog() {
        add("tulip","Tulip","Flower",1,8,22,1,"BEAUTY",20,10,"common",68,2,"flower",new String[]{"#6B503F","#6BC36F","#F66F9B","#FF8AB7"},"flower");
        add("daisy","Daisy","Flower",1,9,24,1,"BEAUTY",22,11,"common",70,2,"flower",new String[]{"#7C5B43","#73C86E","#FFF4D6","#FFE16A"},"flower");
        add("strawberry","Strawberry","Boutique Fruit",1,10,28,2,"BEAUTY",24,11,"common",72,2,"berry",new String[]{"#6A4B2E","#4AA74C","#E44E6D","#FF5B70"},"fruit");
        add("sunflower","Sunflower","Flower",2,12,34,2,"BEAUTY",30,14,"common",78,3,"flower",new String[]{"#745233","#6EB553","#E7B539","#FFD35D"},"flower");
        add("lavender","Lavender","Flower",2,13,36,2,"BEAUTY",33,15,"common",80,3,"flower",new String[]{"#5F4E76","#5DA76A","#9E7BF0","#B391FF"},"flower");
        add("mint","Mint","Herb",2,11,30,2,"RARE",26,12,"common",74,2,"herb",new String[]{"#4E5948","#57B884","#79D9AA","#9BE8BF"},"leaf");
        add("rose","Rose","Flower",3,16,42,2,"BEAUTY",42,18,"uncommon",88,3,"flower",new String[]{"#5B413D","#56A85C","#D9487D","#F65E99"},"flower");
        add("blue_hydrangea","Blue Hydrangea","Flower",3,17,44,2,"BEAUTY",44,18,"uncommon",90,3,"flower",new String[]{"#4F4A64","#5CA56B","#71A8FF","#92BEFF"},"flower");
        add("lemon_balm","Lemon Balm","Herb",3,15,38,2,"YIELD",36,16,"uncommon",76,3,"herb",new String[]{"#56613F","#5FB55B","#9FD45B","#C5EB7C"},"leaf");
        add("peony","Peony","Flower",4,20,50,3,"BEAUTY",54,22,"rare",96,3,"flower",new String[]{"#65485D","#5FAD62","#DD7CAF","#F495C0"},"flower");
        add("apple_blossom","Apple Blossom","Flower",4,21,52,3,"BEAUTY",56,23,"rare",98,3,"flower",new String[]{"#6A544E","#61B45F","#F0A2C0","#FFC0D4"},"flower");
        add("bell_pepper","Bell Pepper","Boutique Fruit",4,20,46,3,"YIELD",46,20,"rare",82,3,"fruit",new String[]{"#614B39","#5FA85A","#EB6C5C","#FF8A7A"},"fruit");
        add("orchid","Orchid","Flower",5,25,58,3,"BEAUTY",68,26,"rare",104,4,"flower",new String[]{"#594963","#58A35F","#C07EFF","#D6A0FF"},"flower");
        add("grapevine","Grapevine","Boutique Fruit",5,24,56,3,"BEAUTY",62,25,"rare",99,4,"fruit",new String[]{"#54475C","#58A35E","#9064DA","#B18CFF"},"fruit");
        add("camellia","Camellia","Flower",5,26,60,3,"BEAUTY",72,28,"epic",108,4,"flower",new String[]{"#5C4747","#61AD63","#F16386","#FF88A3"},"flower");
        add("lotus","Lotus","Flower",6,30,68,3,"BEAUTY",84,31,"epic",114,4,"flower",new String[]{"#5C5A72","#5CA56D","#D79FFF","#E9B7FF"},"flower");
        add("honeysuckle","Honeysuckle","Flower",6,31,70,3,"BEAUTY",86,32,"epic",116,4,"flower",new String[]{"#665A43","#69B760","#F3D26A","#FFF09A"},"flower");
        add("gardenia","Gardenia","Flower",7,34,78,4,"BEAUTY",98,36,"epic",122,5,"flower",new String[]{"#6D614F","#67AF68","#FFF8E8","#FFF0B4"},"flower");
        add("cherry_bloom","Cherry Bloom","Flower",7,36,82,4,"BEAUTY",102,38,"epic",126,5,"flower",new String[]{"#67505B","#67B169","#F5A2C7","#FFC2DD"},"flower");
        add("moon_orchid","Moon Orchid","Flower",8,42,94,4,"BEAUTY",122,44,"legend",136,5,"flower",new String[]{"#594D6D","#67A86A","#D7C2FF","#F0E3FF"},"flower");
        add("golden_rose","Golden Rose","Flower",8,44,98,4,"BEAUTY",126,46,"legend",142,5,"flower",new String[]{"#69583B","#6FAF5F","#E8BF54","#FFE188"},"flower");
        add("crystal_melon","Crystal Melon","Boutique Fruit",9,50,110,4,"RARE",138,50,"legend",144,6,"myth",new String[]{"#4F5C72","#56B49A","#79D7EC","#A2F0FF"},"fruit");
    }

    private void add(String id, String n, String c, int u, int s, float g, int w, String f, int sv, int xp, String r, float b, int y, String t, String[] colors, String shape) {
        crops.add(new CropType(id, n, c, u, s, g, w, f, sv, xp, r, b, y, t, new int[]{Color.parseColor(colors[0]), Color.parseColor(colors[1]), Color.parseColor(colors[2]), Color.parseColor(colors[3])}, shape));
    }

    public List<CropType> all() {
        return crops;
    }

    public CropType firstUnlocked(int level) {
        return pickForPlot(level, 0);
    }

    public CropType pickForPlot(int level, int plotIndex) {
        List<CropType> available = new ArrayList<>();
        List<CropType> flowers = new ArrayList<>();
        for (CropType c : crops) {
            if (c.unlockLevel <= level) {
                available.add(c);
                if ("Flower".equals(c.category)) {
                    flowers.add(c);
                }
            }
        }
        if (!flowers.isEmpty()) {
            return flowers.get(Math.floorMod(plotIndex, flowers.size()));
        }
        if (!available.isEmpty()) {
            return available.get(Math.floorMod(plotIndex, available.size()));
        }
        return crops.get(0);
    }
}
