package com.android.boot.core;

import java.util.ArrayList;
import java.util.List;

public class CandyStage {
    public enum ObjectiveType {
        JELLY,
        SCORE,
        FROSTING,
        ORDER,
        INGREDIENT
    }

    public final int chapterIndex;
    public final int stageIndex;
    public final String chapterName;
    public final String stageName;
    public final ObjectiveType objectiveType;
    public final int width;
    public final int height;
    public final int moves;
    public final int targetCount;
    public final int orderColor;
    public final boolean[][] jellyMap;
    public final int[][] frostingMap;
    public final int[] ingredientColumns;

    public CandyStage(int chapterIndex, int stageIndex, String chapterName, String stageName, ObjectiveType objectiveType, int width, int height, int moves, int targetCount, int orderColor, boolean[][] jellyMap, int[][] frostingMap, int[] ingredientColumns) {
        this.chapterIndex = chapterIndex;
        this.stageIndex = stageIndex;
        this.chapterName = chapterName;
        this.stageName = stageName;
        this.objectiveType = objectiveType;
        this.width = width;
        this.height = height;
        this.moves = moves;
        this.targetCount = targetCount;
        this.orderColor = orderColor;
        this.jellyMap = jellyMap;
        this.frostingMap = frostingMap;
        this.ingredientColumns = ingredientColumns;
    }

    public String getCode() {
        return chapterIndex + "-" + stageIndex;
    }

    public static List<CandyStage> createCampaign() {
        List<CandyStage> stages = new ArrayList<>();
        stages.add(new CandyStage(1, 1, "Sugar Square", "Parade Warmup", ObjectiveType.JELLY, 8, 8, 22, 18, 0,
                jelly(
                        "........",
                        "..JJJJ..",
                        ".JJJJJJ.",
                        ".JJJJJJ.",
                        ".JJJJJJ.",
                        ".JJJJJJ.",
                        "..JJJJ..",
                        "........"
                ),
                frosting(
                        "00000000",
                        "00111100",
                        "01100110",
                        "01000010",
                        "01000010",
                        "01100110",
                        "00111100",
                        "00000000"
                ),
                new int[0]
        ));
        stages.add(new CandyStage(2, 1, "Jelly Avenue", "Ribbon Rush", ObjectiveType.SCORE, 8, 8, 18, 14000, 0,
                jelly(
                        "........",
                        "........",
                        "..JJJJ..",
                        "..JJJJ..",
                        "..JJJJ..",
                        "..JJJJ..",
                        "........",
                        "........"
                ),
                frosting(
                        "00000000",
                        "00022000",
                        "00000000",
                        "11000011",
                        "11000011",
                        "00000000",
                        "00022000",
                        "00000000"
                ),
                new int[0]
        ));
        stages.add(new CandyStage(3, 1, "Frosting Canal", "Cream Break", ObjectiveType.FROSTING, 8, 8, 24, 20, 0,
                jelly(
                        "........",
                        "........",
                        "........",
                        "........",
                        "........",
                        "........",
                        "........",
                        "........"
                ),
                frosting(
                        "02222220",
                        "22111122",
                        "21000012",
                        "21000012",
                        "21000012",
                        "21000012",
                        "22111122",
                        "02222220"
                ),
                new int[0]
        ));
        stages.add(new CandyStage(4, 1, "Ribbon Drop", "Cherry Fall", ObjectiveType.INGREDIENT, 8, 8, 20, 2, 0,
                jelly(
                        "........",
                        "........",
                        "........",
                        "........",
                        "........",
                        "........",
                        "........",
                        "........"
                ),
                frosting(
                        "00000000",
                        "00000000",
                        "00100100",
                        "00100100",
                        "00000000",
                        "00100100",
                        "00100100",
                        "00000000"
                ),
                new int[]{2, 5}
        ));
        stages.add(new CandyStage(5, 1, "Portal Parade", "Pink Orders", ObjectiveType.ORDER, 8, 8, 19, 14, 0,
                jelly(
                        "........",
                        "..JJ..JJ",
                        "..JJ..JJ",
                        "........",
                        "JJ..JJ..",
                        "JJ..JJ..",
                        "........",
                        "........"
                ),
                frosting(
                        "00000000",
                        "01100110",
                        "00000000",
                        "00022000",
                        "00022000",
                        "00000000",
                        "01100110",
                        "00000000"
                ),
                new int[0]
        ));
        stages.add(new CandyStage(6, 1, "Starburst Finale", "Rainbow Encore", ObjectiveType.JELLY, 9, 9, 26, 28, 0,
                jelly(
                        ".........",
                        "..JJJJJ..",
                        ".JJJJJJJ.",
                        ".JJ...JJ.",
                        ".JJ...JJ.",
                        ".JJ...JJ.",
                        ".JJJJJJJ.",
                        "..JJJJJ..",
                        "........."
                ),
                frosting(
                        "000111000",
                        "001222100",
                        "012000210",
                        "120000021",
                        "120000021",
                        "120000021",
                        "012000210",
                        "001222100",
                        "000111000"
                ),
                new int[0]
        ));
        return stages;
    }

    private static boolean[][] jelly(String... rows) {
        boolean[][] map = new boolean[rows.length][rows[0].length()];
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                map[row][col] = rows[row].charAt(col) == 'J';
            }
        }
        return map;
    }

    private static int[][] frosting(String... rows) {
        int[][] map = new int[rows.length][rows[0].length()];
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                map[row][col] = rows[row].charAt(col) - '0';
            }
        }
        return map;
    }
}
