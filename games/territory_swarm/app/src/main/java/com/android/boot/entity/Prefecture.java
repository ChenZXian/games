package com.android.boot.entity;

public class Prefecture {
  public int id;
  public float x;
  public float y;
  public float radius;
  public int owner;
  
  // Polygon shape data (for drawing irregular territories)
  public float[] polygonPoints;  // [x1, y1, x2, y2, ...]
  public int polygonVertexCount;
  
  // Terrain type
  public int terrainType;
  public static final int TERRAIN_LAND = 0;      // Land
  public static final int TERRAIN_CITY = 1;      // City
  public static final int TERRAIN_COAST = 2;     // Coast
  public static final int TERRAIN_MOUNTAIN = 3;  // Mountain
  public static final int TERRAIN_GRASS = 4;     // Grass
  public static final int TERRAIN_OCEAN = 5;     // Ocean
  public int shield;
  public int sword;
  public int archer;
  public int[] neighbors;
  public float growthRate;
  public float growthBuffer;
  public boolean selected;
  public String name;
  public int type;
  public static final int TYPE_PREFECTURE = 0;
  public static final int TYPE_CITY = 1;

  // Building and special effect system
  public int buildingType;
  public static final int BUILDING_NONE = 0;
  public static final int BUILDING_CANNON = 1;      // Cannon: provides ranged support in battle
  public static final int BUILDING_BARRACKS = 2;    // Barracks: increases unit generation speed
  public static final int BUILDING_FORTRESS = 3;    // Fortress: increases defense
  public static final int BUILDING_TRAINING = 4;    // Training: spawns giant units (elite)
  
  public int specialEffect;
  public static final int EFFECT_NONE = 0;
  public static final int EFFECT_GIANT_SPAWN = 1;   // Spawn giant units
  public static final int EFFECT_RAPID_GEN = 2;     // Rapid generation
  public static final int EFFECT_DEFENSE_BOOST = 3; // Defense boost
  
  // Resource system
  public int gold;  // Gold, used to buy units
  public float goldIncome;  // Gold income per second
  
  // Giant units (elite)
  public int giantShield;
  public int giantSword;
  public int giantArcher;

  public boolean battleActive;
  public int battleNonce;
  public int battleAttackerOwner;
  public int battleAtkShield;
  public int battleAtkSword;
  public int battleAtkArcher;
  public float battleTickBuffer;
  public float battleTime;
  // Save original defender count at battle start (for battle simulation init)
  public int battleDefShieldStart;
  public int battleDefSwordStart;
  public int battleDefArcherStart;
  
  // Map skill placement system
  public int placedSkill;  // 0=None, 1=Charge, 2=Arrow Rain, 3=Defense, 4=Retreat
  public static final int SKILL_NONE = 0;
  public static final int SKILL_CHARGE = 1;
  public static final int SKILL_ARROW_RAIN = 2;
  public static final int SKILL_DEFENSE = 3;
  public static final int SKILL_RETREAT = 4;
  
  // Turn-based system
  public boolean hasAttackedThisTurn;  // Whether attacked this turn

  public Prefecture(float x, float y, float radius) {
    this.x = x;
    this.y = y;
    this.radius = radius;
    this.type = TYPE_PREFECTURE;
    this.terrainType = TERRAIN_LAND;
    generatePolygonShape();
  }

  public Prefecture(float x, float y, float radius, String name, int type) {
    this.x = x;
    this.y = y;
    this.radius = radius;
    this.name = name;
    this.type = type;
    generatePolygonShape();
  }
  
  // Generate irregular polygon shape (similar to real map regions)
  private void generatePolygonShape() {
    // Generate polygon with 5-8 vertices
    int vertexCount = 5 + (int)(Math.random() * 4); // 5-8 vertices
    polygonVertexCount = vertexCount;
    polygonPoints = new float[vertexCount * 2];
    
    float baseRadius = radius * 1.2f; // Polygon slightly larger
    float angleStep = (float)(Math.PI * 2.0 / vertexCount);
    
    for (int i = 0; i < vertexCount; i++) {
      float angle = angleStep * i;
      // Add random variation to make shape irregular
      float rVariation = 0.7f + (float)(Math.random() * 0.6f); // 0.7-1.3x
      float currentRadius = baseRadius * rVariation;
      float angleVariation = (float)(Math.random() - 0.5) * 0.3f; // 0.15 radians
      angle += angleVariation;
      
      polygonPoints[i * 2] = x + (float)Math.cos(angle) * currentRadius;
      polygonPoints[i * 2 + 1] = y + (float)Math.sin(angle) * currentRadius;
    }
  }
  
  // Check if point is inside polygon (for click detection)
  public boolean containsPoint(float px, float py) {
    if (polygonPoints == null || polygonPoints.length < 6) {
      // If no polygon, use circle detection
      float dx = px - x;
      float dy = py - y;
      return dx * dx + dy * dy <= radius * radius;
    }
    
    // Use ray casting to determine if point is inside polygon
    boolean inside = false;
    int j = polygonVertexCount - 1;
    for (int i = 0; i < polygonVertexCount; i++) {
      float xi = polygonPoints[i * 2];
      float yi = polygonPoints[i * 2 + 1];
      float xj = polygonPoints[j * 2];
      float yj = polygonPoints[j * 2 + 1];
      
      if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
        inside = !inside;
      }
      j = i;
    }
    return inside;
  }

  public int total() {
    return shield + sword + archer + giantShield + giantSword + giantArcher;
  }
  
  public int totalNormal() {
    return shield + sword + archer;
  }
  
  public int totalGiant() {
    return giantShield + giantSword + giantArcher;
  }
  
  public String getBuildingName() {
    switch (buildingType) {
      case BUILDING_CANNON: return "Cannon";
      case BUILDING_BARRACKS: return "Barracks";
      case BUILDING_FORTRESS: return "Fortress";
      case BUILDING_TRAINING: return "Training";
      default: return "";
    }
  }
  
  public String getEffectName() {
    switch (specialEffect) {
      case EFFECT_GIANT_SPAWN: return "Giant";
      case EFFECT_RAPID_GEN: return "Rapid";
      case EFFECT_DEFENSE_BOOST: return "Defense";
      default: return "";
    }
  }
}

