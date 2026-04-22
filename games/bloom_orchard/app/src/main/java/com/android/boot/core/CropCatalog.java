package com.android.boot.core;

import android.graphics.Color;

import com.android.boot.entity.CropType;

import java.util.ArrayList;
import java.util.List;

public class CropCatalog {
    private final List<CropType> crops = new ArrayList<>();

    public CropCatalog() {
        add("carrot","Carrot","Root",1,8,24,1,"YIELD",18,9,"common",62,2,"root",new String[]{"#8A6A45","#6BBE45","#FF8F2E","#FFA94D"},"fruit");
        add("strawberry","Strawberry","Berry",1,10,30,2,"BEAUTY",24,11,"common",66,2,"berry",new String[]{"#6A4B2E","#4AA74C","#E44E6D","#FF5B70"},"fruit");
        add("blueberry","Blueberry","Berry",2,12,34,2,"RARE",28,12,"common",68,2,"berry",new String[]{"#4A3A2A","#4E9B58","#4C65CC","#5B75E2"},"fruit");
        add("tulip","Tulip","Flower",1,9,28,2,"BEAUTY",22,12,"common",70,2,"flower",new String[]{"#5A4634","#5BAF53","#E9546E","#FF6A82"},"flower");
        add("sunflower","Sunflower","Flower",2,14,42,2,"BEAUTY",32,16,"common",76,3,"flower",new String[]{"#6A4B2D","#68B04A","#D9A32C","#F8C53B"},"flower");
        add("lavender","Lavender","Flower",3,16,44,2,"BEAUTY",36,18,"uncommon",78,3,"flower",new String[]{"#5A4860","#4D9754","#8D64D9","#A178F0"},"flower");
        add("pumpkin","Pumpkin","Gourd",3,18,48,2,"YIELD",42,20,"uncommon",72,3,"gourd",new String[]{"#694C2A","#5AA14C","#DF8B2A","#F9A43A"},"fruit");
        add("watermelon","Watermelon","Gourd",4,22,60,3,"YIELD",56,24,"uncommon",74,4,"gourd",new String[]{"#516138","#4BAA53","#DC4E5F","#F86874"},"fruit");
        add("corn","Corn","Grain",2,13,36,2,"YIELD",30,14,"common",65,3,"grain",new String[]{"#62522F","#5AA84E","#D7A93A","#F2C54C"},"spike");
        add("tomato","Tomato","Fruit",2,12,32,2,"YIELD",26,12,"common",67,2,"fruit",new String[]{"#5A4433","#56A654","#D64F4C","#F0605E"},"fruit");
        add("chili","Chili","Spice",4,20,50,3,"RARE",48,22,"uncommon",80,3,"spice",new String[]{"#533B29","#56A150","#D63B3B","#F94D4D"},"spike");
        add("rose","Rose","Flower",4,24,58,3,"BEAUTY",55,24,"rare",88,3,"flower",new String[]{"#503D31","#4F9951","#C93A68","#E24A7C"},"flower");
        add("cabbage","Cabbage","Leaf",3,16,40,2,"YIELD",34,16,"common",64,3,"leaf",new String[]{"#5B4A31","#4E9B4E","#65B85B","#7BD06A"},"leaf");
        add("grape","Grape","Fruit",5,28,66,3,"BEAUTY",64,28,"rare",89,4,"fruit",new String[]{"#514038","#4B9751","#7D57CC","#936BDE"},"fruit");
        add("pear","Pear","Fruit",5,30,70,3,"YIELD",70,30,"rare",84,4,"fruit",new String[]{"#5A4834","#5AA04D","#A4C742","#C0DE56"},"fruit");
        add("apple_blossom","Apple Blossom","Flower",5,32,72,3,"BEAUTY",76,30,"rare",90,4,"flower",new String[]{"#5A4539","#5AA054","#E68DAE","#F4A8C0"},"flower");
        add("orchid","Orchid","Flower",6,36,78,3,"BEAUTY",88,34,"epic",96,4,"flower",new String[]{"#503D51","#4F9753","#AE6DDE","#C083F2"},"flower");
        add("golden_wheat","Golden Wheat","Grain",6,35,74,3,"YIELD",82,32,"epic",92,5,"grain",new String[]{"#624D2E","#67AC4E","#DEB548","#F5CD60"},"spike");
        add("cherry_bloom","Cherry Bloom","Flower",7,40,86,3,"BEAUTY",96,36,"epic",98,5,"flower",new String[]{"#5A4250","#5AA05E","#E48CAD","#FFAAC6"},"flower");
        add("mint","Mint","Herb",4,20,46,2,"RARE",44,20,"uncommon",79,3,"herb",new String[]{"#4B5340","#4EA66E","#57C987","#74DBA0"},"leaf");
        add("peony","Peony","Flower",7,44,90,3,"BEAUTY",102,38,"epic",99,5,"flower",new String[]{"#5A3E50","#599D58","#D36F9B","#EB85B1"},"flower");
        add("bell_pepper","Bell Pepper","Fruit",5,30,68,3,"YIELD",72,30,"rare",85,4,"fruit",new String[]{"#544634","#57A24D","#D65E46","#F1725B"},"fruit");
        add("lotus","Lotus","Flower",8,52,102,4,"BEAUTY",118,45,"legend",108,6,"flower",new String[]{"#5A4F63","#529E68","#C18CEB","#D9A5F6"},"flower");
        add("crystal_melon","Crystal Melon","Mythic",9,60,120,4,"RARE",140,50,"legend",120,6,"myth",new String[]{"#4A5674","#49A58C","#6BC8E0","#8EE7FF"},"fruit");
    }

    private void add(String id, String n, String c, int u, int s, float g, int w, String f, int sv, int xp, String r, float b, int y, String t, String[] colors, String shape) {
        crops.add(new CropType(id, n, c, u, s, g, w, f, sv, xp, r, b, y, t, new int[]{Color.parseColor(colors[0]), Color.parseColor(colors[1]), Color.parseColor(colors[2]), Color.parseColor(colors[3])}, shape));
    }

    public List<CropType> all() {
        return crops;
    }

    public CropType firstUnlocked(int level) {
        for (CropType c : crops) {
            if (c.unlockLevel <= level) {
                return c;
            }
        }
        return crops.get(0);
    }
}
