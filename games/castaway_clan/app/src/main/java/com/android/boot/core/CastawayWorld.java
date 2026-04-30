package com.android.boot.core;

import com.android.boot.entity.CampStructure;
import com.android.boot.entity.ResourceNode;
import com.android.boot.entity.Survivor;
import com.android.boot.entity.Wildlife;
import com.android.boot.fx.FloatText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CastawayWorld {
    public static class StatusSnapshot {
        public final CastawayState state;
        public final int food;
        public final int water;
        public final int morale;
        public final int population;
        public final int day;
        public final String weather;
        public final String objective;
        public final String hint;
        public final String buildLabel;
        public final String assignLabel;
        public final String craftLabel;
        public final String resultTitle;
        public final String resultBody;
        public final int wood;
        public final int herbs;
        public final int scrap;
        public final int guards;
        public final int builders;
        public final boolean mapMode;
        public final boolean finalStorm;

        public StatusSnapshot(CastawayState state, int food, int water, int morale, int population, int day, String weather,
                              String objective, String hint, String buildLabel, String assignLabel, String craftLabel,
                              String resultTitle, String resultBody, int wood, int herbs, int scrap, int guards,
                              int builders, boolean mapMode, boolean finalStorm) {
            this.state = state;
            this.food = food;
            this.water = water;
            this.morale = morale;
            this.population = population;
            this.day = day;
            this.weather = weather;
            this.objective = objective;
            this.hint = hint;
            this.buildLabel = buildLabel;
            this.assignLabel = assignLabel;
            this.craftLabel = craftLabel;
            this.resultTitle = resultTitle;
            this.resultBody = resultBody;
            this.wood = wood;
            this.herbs = herbs;
            this.scrap = scrap;
            this.guards = guards;
            this.builders = builders;
            this.mapMode = mapMode;
            this.finalStorm = finalStorm;
        }
    }

    public final List<ResourceNode> nodes = new ArrayList<>();
    public final List<Survivor> survivors = new ArrayList<>();
    public final List<CampStructure> structures = new ArrayList<>();
    public final List<Wildlife> wildlife = new ArrayList<>();
    public final FloatText[] floatTexts = new FloatText[20];
    public CastawayState state = CastawayState.MENU;
    public final Random random = new Random(52);
    public final float worldWidth = 2200f;
    public final float worldHeight = 1600f;
    public final float campCenterX = 720f;
    public final float campCenterY = 900f;
    public float playerX = 720f;
    public float playerY = 900f;
    public float playerFacingX = 1f;
    public float playerFacingY = 0f;
    public float camX = 0f;
    public float camY = 0f;
    public float inputX;
    public float inputY;
    public float actionCooldown;
    public float weatherClock;
    public float dayClock;
    public float productivityClock;
    public boolean actionQueued;
    public boolean mapMode;
    public int day = 1;
    public int food = 7;
    public int water = 6;
    public int morale = 76;
    public int wood = 6;
    public int herbs = 1;
    public int scrap = 1;
    public int objectiveIndex;
    public int rescuedCount;
    public boolean finalStormUnlocked;
    public boolean finalStormResolved;
    public WeatherType weather = WeatherType.CLEAR;
    public CampStructureType selectedBuild = CampStructureType.SHELTER;
    public SurvivorRole selectedRole = SurvivorRole.GATHERER;
    public String hint = "Rescue survivors and stabilize the beach camp";
    private final float[] buildSlotX = new float[]{560f, 660f, 760f, 860f, 610f, 810f};
    private final float[] buildSlotY = new float[]{820f, 760f, 820f, 760f, 920f, 920f};

    public CastawayWorld() {
        for (int i = 0; i < floatTexts.length; i++) {
            floatTexts[i] = new FloatText();
        }
        resetRun();
    }

    public void resetRun() {
        nodes.clear();
        survivors.clear();
        structures.clear();
        wildlife.clear();
        state = CastawayState.MENU;
        playerX = 720f;
        playerY = 900f;
        camX = 0f;
        camY = 0f;
        inputX = 0f;
        inputY = 0f;
        actionCooldown = 0f;
        weatherClock = 0f;
        dayClock = 0f;
        productivityClock = 0f;
        actionQueued = false;
        mapMode = false;
        day = 1;
        food = 7;
        water = 6;
        morale = 76;
        wood = 6;
        herbs = 1;
        scrap = 1;
        objectiveIndex = 0;
        rescuedCount = 0;
        finalStormUnlocked = false;
        finalStormResolved = false;
        weather = WeatherType.CLEAR;
        selectedBuild = CampStructureType.SHELTER;
        selectedRole = SurvivorRole.GATHERER;
        hint = "Tap Start and scout the beach for survivors";
        survivors.add(new Survivor("Leader", SurvivorRole.IDLE, 700f, 910f));
        survivors.add(new Survivor("Mira", SurvivorRole.GATHERER, 620f, 950f));
        survivors.add(new Survivor("Bram", SurvivorRole.FISHER, 790f, 970f));
        spawnDayContent();
    }

    public void startGame() {
        state = CastawayState.PLAYING;
        hint = "Hold and drag on the left half to move. Tap Action near resources, wildlife, and castaways";
    }

    public void openMenu() {
        state = CastawayState.MENU;
    }

    public void togglePause() {
        if (state == CastawayState.PLAYING) {
            state = CastawayState.PAUSED;
        } else if (state == CastawayState.PAUSED) {
            state = CastawayState.PLAYING;
        }
    }

    public void queueAction() {
        actionQueued = true;
    }

    public void setInput(float x, float y) {
        inputX = x;
        inputY = y;
        if (Math.abs(x) > 0.01f || Math.abs(y) > 0.01f) {
            playerFacingX = x;
            playerFacingY = y;
        }
    }

    public void handleWorldTap(float worldX, float worldY) {
        if (state != CastawayState.PLAYING) {
            return;
        }
        float nearest = Float.MAX_VALUE;
        int nearestSlot = -1;
        for (int i = 0; i < buildSlotX.length; i++) {
            if (findStructureAt(i) != null) {
                continue;
            }
            float dx = buildSlotX[i] - worldX;
            float dy = buildSlotY[i] - worldY;
            float dist = dx * dx + dy * dy;
            if (dist < nearest) {
                nearest = dist;
                nearestSlot = i;
            }
        }
        if (nearestSlot >= 0 && nearest < 120f * 120f) {
            tryBuild(nearestSlot);
        } else {
            hint = "Build on marked camp pads or keep scouting new zones";
        }
    }

    public void cycleBuildType() {
        CampStructureType[] values = CampStructureType.values();
        int next = (selectedBuild.ordinal() + 1) % values.length;
        selectedBuild = values[next];
        hint = selectedBuild.label + " selected";
    }

    public void reassignSurvivor() {
        SurvivorRole[] roles = new SurvivorRole[]{
                SurvivorRole.GATHERER,
                SurvivorRole.FISHER,
                SurvivorRole.BUILDER,
                SurvivorRole.GUARD,
                SurvivorRole.HEALER
        };
        int next = 0;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i] == selectedRole) {
                next = (i + 1) % roles.length;
                break;
            }
        }
        selectedRole = roles[next];
        Survivor target = null;
        for (int i = 1; i < survivors.size(); i++) {
            if (survivors.get(i).role == SurvivorRole.IDLE) {
                target = survivors.get(i);
                break;
            }
        }
        if (target == null) {
            for (int i = 1; i < survivors.size(); i++) {
                Survivor survivor = survivors.get(i);
                if (survivor.role != selectedRole) {
                    target = survivor;
                    break;
                }
            }
        }
        if (target != null) {
            target.role = selectedRole;
            hint = target.name + " now works as " + selectedRole.label;
        } else {
            hint = "Rescue more people before expanding camp jobs";
        }
    }

    public void craftSupplies() {
        if (wood >= 2 && scrap >= 1) {
            wood -= 2;
            scrap -= 1;
            morale = Math.min(100, morale + 6);
            spawnText("+Kit", playerX, playerY - 70f, 0xFFFFB24A);
            hint = "Workshop kits boosted morale and repair confidence";
        } else if (food >= 2 && herbs >= 1) {
            food -= 2;
            herbs -= 1;
            water += 1;
            morale = Math.min(100, morale + 4);
            spawnText("+Meal", playerX, playerY - 70f, 0xFF4FE38A);
            hint = "Cooked rations steadied the camp";
        } else {
            hint = "Need 2 wood and 1 scrap, or 2 food and 1 herb, to craft relief supplies";
        }
    }

    public void toggleMapFocus() {
        mapMode = !mapMode;
        hint = mapMode ? regionFocusText() : "Map folded away. Follow the objective ribbon";
    }

    public void advanceObjectiveCard() {
        objectiveIndex = (objectiveIndex + 1) % 3;
        hint = objectiveText();
    }

    public void update(float dt) {
        if (state != CastawayState.PLAYING) {
            updateFloatTexts(dt);
            return;
        }
        dayClock += dt;
        weatherClock += dt;
        productivityClock += dt;
        actionCooldown = Math.max(0f, actionCooldown - dt);
        movePlayer(dt);
        updateSurvivors(dt);
        updateWildlife(dt);
        if (actionQueued) {
            actionQueued = false;
            tryInteract();
        }
        if (productivityClock >= 4f) {
            productivityClock = 0f;
            runColonyProduction();
        }
        if (weatherClock >= 18f) {
            weatherClock = 0f;
            rotateWeather();
        }
        if (dayClock >= 84f) {
            resolveDay();
        }
        updateFloatTexts(dt);
        updateCamera();
    }

    private void movePlayer(float dt) {
        float len = (float) Math.sqrt(inputX * inputX + inputY * inputY);
        float moveX = inputX;
        float moveY = inputY;
        if (len > 1f) {
            moveX /= len;
            moveY /= len;
        }
        float speed = 190f + countRole(SurvivorRole.GUARD) * 8f + builtCount(CampStructureType.WORKSHOP) * 10f;
        playerX += moveX * speed * dt;
        playerY += moveY * speed * dt;
        playerX = clamp(playerX, 80f, worldWidth - 80f);
        playerY = clamp(playerY, 120f, worldHeight - 120f);
    }

    private void updateSurvivors(float dt) {
        for (int i = 0; i < survivors.size(); i++) {
            Survivor survivor = survivors.get(i);
            survivor.animTime += dt;
            if (i == 0) {
                survivor.x += (playerX - survivor.x) * Math.min(1f, dt * 8f);
                survivor.y += (playerY - survivor.y) * Math.min(1f, dt * 8f);
                continue;
            }
            float orbit = 44f + (i % 3) * 24f;
            float angle = survivor.animTime * 0.8f + i * 0.9f;
            if (survivor.role == SurvivorRole.GATHERER) {
                survivor.targetX = campCenterX - 120f + (i * 28f) % 110f;
                survivor.targetY = campCenterY + 120f + (i % 2) * 22f;
            } else if (survivor.role == SurvivorRole.FISHER) {
                survivor.targetX = campCenterX + 270f;
                survivor.targetY = campCenterY + 140f + (i % 2) * 36f;
            } else if (survivor.role == SurvivorRole.BUILDER) {
                survivor.targetX = campCenterX + (float) Math.cos(angle) * orbit;
                survivor.targetY = campCenterY - 110f + (float) Math.sin(angle) * 28f;
            } else if (survivor.role == SurvivorRole.GUARD) {
                survivor.targetX = campCenterX + (float) Math.cos(angle) * 180f;
                survivor.targetY = campCenterY + (float) Math.sin(angle) * 140f;
            } else if (survivor.role == SurvivorRole.HEALER) {
                survivor.targetX = campCenterX - 70f;
                survivor.targetY = campCenterY + 32f;
            } else {
                survivor.targetX = campCenterX + (float) Math.cos(angle) * 90f;
                survivor.targetY = campCenterY + (float) Math.sin(angle) * 60f;
            }
            survivor.x += (survivor.targetX - survivor.x) * Math.min(1f, dt * 2.4f);
            survivor.y += (survivor.targetY - survivor.y) * Math.min(1f, dt * 2.4f);
        }
    }

    private void updateWildlife(float dt) {
        for (Wildlife animal : wildlife) {
            if (!animal.active) {
                continue;
            }
            animal.animTime += dt;
            animal.x += animal.vx * dt;
            animal.y += animal.vy * dt;
            if (animal.x < 120f || animal.x > worldWidth - 120f) {
                animal.vx *= -1f;
            }
            if (animal.y < 140f || animal.y > worldHeight - 120f) {
                animal.vy *= -1f;
            }
            float dx = animal.x - playerX;
            float dy = animal.y - playerY;
            if (dx * dx + dy * dy < 80f * 80f && actionCooldown <= 0f) {
                morale = Math.max(0, morale - 1);
                actionCooldown = 1.2f;
                hint = "Wildlife pressure is rising. Use Action to drive them off";
            }
        }
    }

    private void tryInteract() {
        ResourceNode nearest = null;
        float bestDist = Float.MAX_VALUE;
        for (ResourceNode node : nodes) {
            if (!node.active) {
                continue;
            }
            float dx = node.x - playerX;
            float dy = node.y - playerY;
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                nearest = node;
            }
        }
        if (nearest != null && bestDist < 110f * 110f) {
            harvestNode(nearest);
            return;
        }
        Wildlife targetAnimal = null;
        bestDist = Float.MAX_VALUE;
        for (Wildlife animal : wildlife) {
            if (!animal.active) {
                continue;
            }
            float dx = animal.x - playerX;
            float dy = animal.y - playerY;
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                targetAnimal = animal;
            }
        }
        if (targetAnimal != null && bestDist < 120f * 120f) {
            targetAnimal.active = false;
            food += targetAnimal.type == Wildlife.Type.BOAR ? 2 : 1;
            morale = Math.min(100, morale + 2);
            hint = "The camp drove off a threat and salvaged food";
            spawnText("+Safe", targetAnimal.x, targetAnimal.y - 50f, 0xFF4FE38A);
            return;
        }
        hint = "No actionable target nearby";
    }

    private void harvestNode(ResourceNode node) {
        node.active = false;
        switch (node.type) {
            case WOOD:
                wood += 2 + node.tier;
                spawnText("+Wood", node.x, node.y - 40f, 0xFFE0A16A);
                hint = "Driftwood stocked the camp stores";
                break;
            case FOOD:
                food += 2 + node.tier;
                spawnText("+Food", node.x, node.y - 40f, 0xFFFFB24A);
                hint = "Fruit and shellfish raised daily food reserves";
                break;
            case WATER:
                water += 2 + node.tier;
                spawnText("+Water", node.x, node.y - 40f, 0xFF2ED6FF);
                hint = "Fresh water secured before dusk";
                break;
            case HERB:
                herbs += 1 + node.tier;
                spawnText("+Herb", node.x, node.y - 40f, 0xFF4FE38A);
                hint = "Medicinal herbs support the healer";
                break;
            case SCRAP:
                scrap += 1 + node.tier;
                spawnText("+Scrap", node.x, node.y - 40f, 0xFFBFA680);
                hint = "Wreck salvage can finish advanced structures";
                break;
            case SURVIVOR:
                rescuedCount++;
                Survivor newcomer = new Survivor("Crew " + (survivors.size()), SurvivorRole.IDLE, node.x, node.y);
                survivors.add(newcomer);
                morale = Math.min(100, morale + 5);
                spawnText("+Crew", node.x, node.y - 40f, 0xFF7B61FF);
                hint = newcomer.name + " joined the camp. Assign a role from the notebook rail";
                break;
        }
    }

    private void runColonyProduction() {
        food += countRole(SurvivorRole.FISHER);
        wood += countRole(SurvivorRole.GATHERER);
        if (builtCount(CampStructureType.COLLECTOR) > 0) {
            water += builtCount(CampStructureType.COLLECTOR);
        }
        if (countRole(SurvivorRole.HEALER) > 0 && herbs > 0 && morale < 92) {
            herbs--;
            morale += 2 + countRole(SurvivorRole.HEALER);
        }
        if (countRole(SurvivorRole.BUILDER) > 0 && builtCount(CampStructureType.WORKSHOP) > 0) {
            scrap += 1;
        }
        if (rescuedCount >= 3 && builtCount(CampStructureType.WORKSHOP) > 0) {
            finalStormUnlocked = true;
        }
    }

    private void rotateWeather() {
        if (finalStormUnlocked && day >= 5) {
            weather = WeatherType.STORM;
            return;
        }
        WeatherType[] cycle = new WeatherType[]{WeatherType.CLEAR, WeatherType.WINDY, WeatherType.RAIN, WeatherType.CLEAR};
        weather = cycle[(day + objectiveIndex) % cycle.length];
    }

    private void resolveDay() {
        dayClock = 0f;
        int population = survivors.size();
        int shelter = builtCount(CampStructureType.SHELTER);
        int fence = builtCount(CampStructureType.FENCE);
        int infirmary = builtCount(CampStructureType.INFIRMARY);
        int guard = countRole(SurvivorRole.GUARD);
        int healer = countRole(SurvivorRole.HEALER);
        food -= Math.max(2, population - countRole(SurvivorRole.FISHER));
        water -= Math.max(2, population - builtCount(CampStructureType.COLLECTOR));
        if (food < 0) {
            morale += food * 3;
            food = 0;
        }
        if (water < 0) {
            morale += water * 4;
            water = 0;
        }
        morale -= Math.max(0, population - shelter) * 2;
        if (weather == WeatherType.STORM) {
            morale -= Math.max(2, 7 - guard - fence * 2 - healer - infirmary);
        } else if (weather == WeatherType.RAIN) {
            morale -= Math.max(0, 2 - collectorCoverage());
        }
        morale = clampInt(morale, 0, 100);
        day++;
        if (weather == WeatherType.STORM && builtCount(CampStructureType.BEACON) > 0 && rescuedCount >= 3 && population >= 6) {
            finalStormResolved = true;
            state = CastawayState.VICTORY;
            hint = "Beacon fire held through the storm. Rescue ships are inbound";
            return;
        }
        if (morale <= 0 || population <= 1) {
            state = CastawayState.GAME_OVER;
            hint = "The colony collapsed before rescue could arrive";
            return;
        }
        spawnDayContent();
        hint = "Day " + day + " begins. " + objectiveText();
    }

    private int collectorCoverage() {
        return builtCount(CampStructureType.COLLECTOR) + countRole(SurvivorRole.BUILDER) / 2;
    }

    private void spawnDayContent() {
        nodes.clear();
        wildlife.clear();
        addNode(ResourceNode.Type.WOOD, 310f, 1180f, 46f, 1);
        addNode(ResourceNode.Type.WOOD, 1220f, 1110f, 44f, 1);
        addNode(ResourceNode.Type.WATER, 520f, 540f, 48f, 1);
        addNode(ResourceNode.Type.FOOD, 1440f, 830f, 44f, 1);
        addNode(ResourceNode.Type.HERB, 1040f, 380f, 38f, 1);
        addNode(ResourceNode.Type.SCRAP, 1720f, 670f, 42f, 1);
        if (rescuedCount < 4) {
            addNode(ResourceNode.Type.SURVIVOR, 1730f - rescuedCount * 120f, 1180f - rescuedCount * 90f, 44f, 1);
        }
        wildlife.add(new Wildlife(Wildlife.Type.BOAR, 1300f, 470f, 30f, 18f));
        wildlife.add(new Wildlife(Wildlife.Type.MONKEY, 1820f, 1020f, -22f, 16f));
        wildlife.add(new Wildlife(Wildlife.Type.SNAKE, 930f, 300f, 20f, 12f));
        wildlife.add(new Wildlife(Wildlife.Type.CRAB, 260f, 1320f, 16f, -8f));
    }

    private void addNode(ResourceNode.Type type, float x, float y, float radius, int tier) {
        nodes.add(new ResourceNode(type, x, y, radius, tier));
    }

    private void tryBuild(int slot) {
        if (findStructureAt(slot) != null) {
            hint = "That build pad is already occupied";
            return;
        }
        if (wood < selectedBuild.woodCost || food < selectedBuild.foodCost || herbs < selectedBuild.herbCost
                || water < selectedBuild.waterCost || scrap < selectedBuild.scrapCost) {
            hint = "Need " + selectedBuild.woodCost + " wood, " + selectedBuild.foodCost + " food, "
                    + selectedBuild.herbCost + " herbs, " + selectedBuild.waterCost + " water, "
                    + selectedBuild.scrapCost + " scrap";
            return;
        }
        if (selectedBuild == CampStructureType.BEACON && rescuedCount < 3) {
            hint = "Rescue at least three castaways before raising the beacon";
            return;
        }
        wood -= selectedBuild.woodCost;
        food -= selectedBuild.foodCost;
        herbs -= selectedBuild.herbCost;
        water -= selectedBuild.waterCost;
        scrap -= selectedBuild.scrapCost;
        structures.add(new CampStructure(selectedBuild, buildSlotX[slot], buildSlotY[slot]));
        morale = Math.min(100, morale + 3);
        spawnText(selectedBuild.label, buildSlotX[slot], buildSlotY[slot] - 40f, 0xFF7B61FF);
        hint = selectedBuild.label + " completed";
    }

    private CampStructure findStructureAt(int slot) {
        for (CampStructure structure : structures) {
            if (Math.abs(structure.x - buildSlotX[slot]) < 1f && Math.abs(structure.y - buildSlotY[slot]) < 1f) {
                return structure;
            }
        }
        return null;
    }

    private int countRole(SurvivorRole role) {
        int total = 0;
        for (int i = 1; i < survivors.size(); i++) {
            if (survivors.get(i).role == role) {
                total++;
            }
        }
        return total;
    }

    private int builtCount(CampStructureType type) {
        int total = 0;
        for (CampStructure structure : structures) {
            if (structure.type == type) {
                total++;
            }
        }
        return total;
    }

    private void spawnText(String text, float x, float y, int color) {
        for (FloatText item : floatTexts) {
            if (item.life <= 0f) {
                item.set(text, x, y, color);
                return;
            }
        }
    }

    private void updateFloatTexts(float dt) {
        for (FloatText item : floatTexts) {
            if (item.life > 0f) {
                item.life -= dt;
                item.y -= dt * 26f;
            }
        }
    }

    private void updateCamera() {
        camX = clamp(playerX - 540f, 0f, worldWidth - 1080f);
        camY = clamp(playerY - 760f, 0f, worldHeight - 1600f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String objectiveText() {
        if (finalStormResolved) {
            return "Hold morale until rescue arrives";
        }
        switch (objectiveIndex) {
            case 0:
                return "Rescue survivors and keep food, water, and morale above collapse";
            case 1:
                return "Place Shelter, Fence, and Rain Collector to stabilize the beachhead";
            default:
                return "Raise the Beacon after three rescues and survive the final storm";
        }
    }

    private String regionFocusText() {
        String[] labels = new String[]{
                "Beachhead camp and drift line",
                "Freshwater grove and herb pockets",
                "Wreck ridge and beacon hill"
        };
        return "Map focus: " + labels[objectiveIndex];
    }

    public StatusSnapshot snapshot() {
        String resultTitle = "";
        String resultBody = "";
        if (state == CastawayState.GAME_OVER) {
            resultTitle = "Camp Lost";
            resultBody = "Day " + day + "  Rescued " + rescuedCount + "  Structures " + structures.size();
        } else if (state == CastawayState.VICTORY) {
            resultTitle = "Rescue Secured";
            resultBody = "Day " + day + "  Population " + survivors.size() + "  Beacon complete";
        }
        return new StatusSnapshot(
                state,
                food,
                water,
                morale,
                survivors.size(),
                day,
                weather.label,
                objectiveText(),
                hint,
                "Build: " + selectedBuild.label,
                "Assign: " + selectedRole.label,
                "Craft: Relief Kit",
                resultTitle,
                resultBody,
                wood,
                herbs,
                scrap,
                countRole(SurvivorRole.GUARD),
                countRole(SurvivorRole.BUILDER),
                mapMode,
                finalStormUnlocked
        );
    }

    public String debugRegionLabel(float x, float y) {
        if (x < 520f && y > 1040f) {
            return "Drift Line";
        }
        if (x < 760f && y < 700f) {
            return "Freshwater Grove";
        }
        if (x > 1450f && y > 950f) {
            return "Wreck Ridge";
        }
        if (x > 1500f && y < 620f) {
            return "Beacon Hill";
        }
        return String.format(Locale.US, "Camp Zone %.0f, %.0f", x, y);
    }
}
