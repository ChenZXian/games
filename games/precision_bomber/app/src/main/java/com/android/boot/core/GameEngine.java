package com.android.boot.core;

import com.android.boot.entity.Bomb;
import com.android.boot.entity.ChaserEnemy;
import com.android.boot.entity.Enemy;
import com.android.boot.entity.Player;
import com.android.boot.entity.Pickup;
import com.android.boot.entity.SpawnerEnemy;
import com.android.boot.entity.WalkerEnemy;
import com.android.boot.fx.Explosion;
import com.android.boot.grid.Grid;
import com.android.boot.grid.TileType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameEngine {
  private final int gridWidth;
  private final int gridHeight;
  private final Grid grid;
  private final Player player;
  private final List<Enemy> enemies;
  private final List<Bomb> bombs;
  private final List<Pickup> pickups;
  private final List<Explosion> explosions;
  private final Random random;
  private GameState state;
  private Direction inputDirection;
  private boolean bombPressed;
  private boolean remotePressed;
  private int stage;
  private int score;
  private float stageTimer;
  private int chainCounter;

  public GameEngine(int width, int height) {
    this.gridWidth = width;
    this.gridHeight = height;
    this.grid = new Grid(width, height);
    this.player = new Player();
    this.enemies = new ArrayList<>();
    this.bombs = new ArrayList<>();
    this.pickups = new ArrayList<>();
    this.explosions = new ArrayList<>();
    this.random = new Random();
    this.state = GameState.MENU;
    this.stage = 1;
    this.score = 0;
    this.stageTimer = 0f;
    this.chainCounter = 0;
  }

  public GameState getState() {
    return state;
  }

  public Grid getGrid() {
    return grid;
  }

  public Player getPlayer() {
    return player;
  }

  public List<Enemy> getEnemies() {
    return enemies;
  }

  public List<Bomb> getBombs() {
    return bombs;
  }

  public List<Pickup> getPickups() {
    return pickups;
  }

  public List<Explosion> getExplosions() {
    return explosions;
  }

  public void setInputDirection(Direction direction) {
    this.inputDirection = direction;
  }

  public void pressBomb() {
    bombPressed = true;
  }

  public void pressRemote() {
    remotePressed = true;
  }

  public void startGame() {
    stage = 1;
    score = 0;
    initStage();
    setState(GameState.PLAYING);
  }

  public void restartStage() {
    initStage();
    setState(GameState.PLAYING);
  }

  public void nextStage() {
    stage = Math.min(15, stage + 1);
    initStage();
    setState(GameState.PLAYING);
  }

  public void pause() {
    if (state == GameState.PLAYING) {
      setState(GameState.PAUSED);
    }
  }

  public void resume() {
    if (state == GameState.PAUSED) {
      setState(GameState.PLAYING);
    }
  }

  public void goToMenu() {
    setState(GameState.MENU);
  }

  private void setState(GameState newState) {
    state = newState;
  }

  private void initStage() {
    stageTimer = 0f;
    chainCounter = 0;
    bombs.clear();
    pickups.clear();
    explosions.clear();
    enemies.clear();
    generateGrid();
    player.gridX = 1;
    player.gridY = 1;
    player.snapToGrid();
    player.maxBombs = 1 + stage / 4;
    player.speed = 1.0f + stage * 0.05f;
    player.bombFuse = Math.max(0.9f, 1.6f - stage * 0.03f);
    player.shield = 0;
    player.remoteDetonator = false;
    spawnEnemies();
  }

  private void generateGrid() {
    for (int y = 0; y < gridHeight; y++) {
      for (int x = 0; x < gridWidth; x++) {
        boolean border = x == 0 || y == 0 || x == gridWidth - 1 || y == gridHeight - 1;
        boolean pillar = x % 2 == 0 && y % 2 == 0;
        if (border || pillar) {
          grid.set(x, y, TileType.SOLID);
        } else {
          grid.set(x, y, TileType.FLOOR);
        }
      }
    }
    int softBlocks = 25 + stage * 2;
    int placed = 0;
    while (placed < softBlocks) {
      int x = 1 + random.nextInt(gridWidth - 2);
      int y = 1 + random.nextInt(gridHeight - 2);
      if ((x <= 2 && y <= 2) || (x >= gridWidth - 3 && y >= gridHeight - 3)) {
        continue;
      }
      if (grid.get(x, y) == TileType.FLOOR) {
        grid.set(x, y, TileType.SOFT);
        placed++;
      }
    }
    int exitX;
    int exitY;
    do {
      exitX = 1 + random.nextInt(gridWidth - 2);
      exitY = 1 + random.nextInt(gridHeight - 2);
    } while (grid.get(exitX, exitY) != TileType.SOFT);
    grid.set(exitX, exitY, TileType.EXIT_HIDDEN);
  }

  private void spawnEnemies() {
    int walkers = 2 + stage;
    int chasers = 1 + stage / 3;
    int spawners = stage >= 5 ? 1 + stage / 5 : 0;
    for (int i = 0; i < walkers; i++) {
      WalkerEnemy enemy = new WalkerEnemy();
      placeEnemy(enemy);
      enemy.speed = 0.7f + stage * 0.02f;
      enemies.add(enemy);
    }
    for (int i = 0; i < chasers; i++) {
      ChaserEnemy enemy = new ChaserEnemy();
      placeEnemy(enemy);
      enemy.speed = 0.8f + stage * 0.03f;
      enemies.add(enemy);
    }
    for (int i = 0; i < spawners; i++) {
      SpawnerEnemy enemy = new SpawnerEnemy();
      placeEnemy(enemy);
      enemy.speed = 0.6f;
      enemy.spawnInterval = Math.max(2.5f, 4.5f - stage * 0.15f);
      enemies.add(enemy);
    }
  }

  private void placeEnemy(Enemy enemy) {
    int x;
    int y;
    do {
      x = 1 + random.nextInt(gridWidth - 2);
      y = 1 + random.nextInt(gridHeight - 2);
    } while (!grid.isWalkable(x, y) || isOccupied(x, y));
    enemy.gridX = x;
    enemy.gridY = y;
    enemy.snapToGrid();
  }

  public void update(float dt) {
    if (state != GameState.PLAYING) {
      return;
    }
    stageTimer += dt;
    updatePlayer(dt);
    updateBombs(dt);
    updateExplosions(dt);
    updatePickups();
    updateEnemies(dt);
    checkStageClear();
  }

  private void updatePlayer(float dt) {
    if (player.moving) {
      updateMovement(player, dt);
    } else if (inputDirection != null) {
      tryStartMove(player, inputDirection, player.speed);
    }
    if (bombPressed) {
      placeBomb();
      bombPressed = false;
    }
    if (remotePressed) {
      if (player.remoteDetonator) {
        detonateRemote();
      }
      remotePressed = false;
    }
  }

  private void updateBombs(float dt) {
    for (Bomb bomb : bombs) {
      if (!bomb.exploded) {
        bomb.timer += dt;
        if (bomb.timer >= bomb.fuse) {
          explodeBomb(bomb, 0);
        }
      }
    }
    bombs.removeIf(bomb -> bomb.exploded);
  }

  private void updateExplosions(float dt) {
    Iterator<Explosion> iterator = explosions.iterator();
    while (iterator.hasNext()) {
      Explosion explosion = iterator.next();
      explosion.timer -= dt;
      if (explosion.timer <= 0f) {
        iterator.remove();
      }
    }
  }

  private void updatePickups() {
    Iterator<Pickup> iterator = pickups.iterator();
    while (iterator.hasNext()) {
      Pickup pickup = iterator.next();
      if (pickup.x == player.gridX && pickup.y == player.gridY) {
        applyPickup(pickup.type);
        iterator.remove();
      }
    }
  }

  private void updateEnemies(float dt) {
    for (Enemy enemy : enemies) {
      if (!enemy.alive) {
        continue;
      }
      if (enemy.moving) {
        updateMovement(enemy, dt);
      } else if (enemy instanceof WalkerEnemy) {
        updateWalker((WalkerEnemy) enemy, dt);
      } else if (enemy instanceof ChaserEnemy) {
        updateChaser((ChaserEnemy) enemy, dt);
      } else if (enemy instanceof SpawnerEnemy) {
        updateSpawner((SpawnerEnemy) enemy, dt);
      }
      if (enemy.gridX == player.gridX && enemy.gridY == player.gridY) {
        hitPlayer();
      }
    }
  }

  private void updateWalker(WalkerEnemy enemy, float dt) {
    enemy.decisionTimer -= dt;
    if (enemy.decisionTimer <= 0f) {
      enemy.decisionTimer = 0.4f + random.nextFloat() * 0.6f;
      Direction[] dirs = Direction.values();
      enemy.desired = dirs[random.nextInt(dirs.length)];
    }
    tryStartMove(enemy, enemy.desired, enemy.speed);
  }

  private void updateChaser(ChaserEnemy enemy, float dt) {
    enemy.thinkTimer -= dt;
    if (enemy.thinkTimer <= 0f) {
      enemy.thinkTimer = 0.35f;
      Direction chase = findChaseDirection(enemy.gridX, enemy.gridY);
      if (chase != null) {
        enemy.facing = chase;
        tryStartMove(enemy, chase, enemy.speed);
        return;
      }
    }
    Direction[] dirs = Direction.values();
    Direction randomDir = dirs[random.nextInt(dirs.length)];
    tryStartMove(enemy, randomDir, enemy.speed);
  }

  private void updateSpawner(SpawnerEnemy enemy, float dt) {
    enemy.spawnTimer += dt;
    if (enemy.spawnTimer >= enemy.spawnInterval) {
      enemy.spawnTimer = 0f;
      spawnWalkerNear(enemy.gridX, enemy.gridY);
    }
    Direction[] dirs = Direction.values();
    Direction randomDir = dirs[random.nextInt(dirs.length)];
    tryStartMove(enemy, randomDir, enemy.speed);
  }

  private void spawnWalkerNear(int x, int y) {
    Direction[] dirs = Direction.values();
    for (int i = 0; i < dirs.length; i++) {
      Direction dir = dirs[random.nextInt(dirs.length)];
      int nx = x + dir.dx;
      int ny = y + dir.dy;
      if (grid.inBounds(nx, ny) && grid.isWalkable(nx, ny) && !isOccupied(nx, ny)) {
        WalkerEnemy enemy = new WalkerEnemy();
        enemy.gridX = nx;
        enemy.gridY = ny;
        enemy.snapToGrid();
        enemy.speed = 0.7f + stage * 0.02f;
        enemies.add(enemy);
        return;
      }
    }
  }

  private Direction findChaseDirection(int x, int y) {
    if (x == player.gridX) {
      int step = player.gridY > y ? 1 : -1;
      for (int yy = y + step; yy != player.gridY; yy += step) {
        if (!grid.isWalkable(x, yy)) {
          return null;
        }
      }
      return step > 0 ? Direction.DOWN : Direction.UP;
    }
    if (y == player.gridY) {
      int step = player.gridX > x ? 1 : -1;
      for (int xx = x + step; xx != player.gridX; xx += step) {
        if (!grid.isWalkable(xx, y)) {
          return null;
        }
      }
      return step > 0 ? Direction.RIGHT : Direction.LEFT;
    }
    return null;
  }

  private void updateMovement(Enemy enemy, float dt) {
    enemy.moveTimer += dt;
    float t = Math.min(1f, enemy.moveTimer / enemy.moveDuration);
    enemy.posX = enemy.gridX + (enemy.targetX - enemy.gridX) * t;
    enemy.posY = enemy.gridY + (enemy.targetY - enemy.gridY) * t;
    if (t >= 1f) {
      enemy.gridX = enemy.targetX;
      enemy.gridY = enemy.targetY;
      enemy.snapToGrid();
    }
  }

  private void updateMovement(Player entity, float dt) {
    entity.moveTimer += dt;
    float t = Math.min(1f, entity.moveTimer / entity.moveDuration);
    entity.posX = entity.gridX + (entity.targetX - entity.gridX) * t;
    entity.posY = entity.gridY + (entity.targetY - entity.gridY) * t;
    if (t >= 1f) {
      entity.gridX = entity.targetX;
      entity.gridY = entity.targetY;
      entity.snapToGrid();
    }
  }

  private void tryStartMove(Entity entity, Direction direction, float speed) {
    int nx = entity.gridX + direction.dx;
    int ny = entity.gridY + direction.dy;
    if (!grid.inBounds(nx, ny)) {
      return;
    }
    if (!grid.isWalkable(nx, ny)) {
      return;
    }
    if (entity instanceof Player) {
      if (isBlockedForPlayer(nx, ny)) {
        return;
      }
      player.facing = direction;
    } else if (isOccupied(nx, ny)) {
      return;
    }
    entity.targetX = nx;
    entity.targetY = ny;
    entity.moving = true;
    entity.moveTimer = 0f;
    entity.moveDuration = Math.max(0.08f, 0.25f / speed);
  }

  private boolean isBlockedForPlayer(int x, int y) {
    if (isBombAt(x, y)) {
      return true;
    }
    for (Enemy enemy : enemies) {
      if (enemy.alive && enemy.gridX == x && enemy.gridY == y) {
        return true;
      }
    }
    return false;
  }

  private boolean isOccupied(int x, int y) {
    if (player.gridX == x && player.gridY == y) {
      return true;
    }
    if (isBombAt(x, y)) {
      return true;
    }
    for (Enemy enemy : enemies) {
      if (enemy.alive && enemy.gridX == x && enemy.gridY == y) {
        return true;
      }
    }
    return false;
  }

  private boolean isBombAt(int x, int y) {
    for (Bomb bomb : bombs) {
      if (!bomb.exploded && bomb.x == x && bomb.y == y) {
        return true;
      }
    }
    return false;
  }

  private void placeBomb() {
    if (bombs.size() >= player.maxBombs) {
      return;
    }
    for (Bomb bomb : bombs) {
      if (bomb.x == player.gridX && bomb.y == player.gridY && !bomb.exploded) {
        return;
      }
    }
    Bomb bomb = new Bomb(player.gridX, player.gridY, player.bombFuse, player.facing);
    bombs.add(bomb);
  }

  private void detonateRemote() {
    for (Bomb bomb : bombs) {
      if (!bomb.exploded) {
        explodeBomb(bomb, 0);
      }
    }
  }

  private void explodeBomb(Bomb bomb, int chainDepth) {
    if (bomb.exploded) {
      return;
    }
    bomb.exploded = true;
    chainCounter = Math.max(chainCounter, chainDepth);
    applyExplosionTile(bomb.x, bomb.y, chainDepth);
    int nx = bomb.x + bomb.direction.dx;
    int ny = bomb.y + bomb.direction.dy;
    if (grid.inBounds(nx, ny) && !grid.isBlockingExplosion(nx, ny)) {
      applyExplosionTile(nx, ny, chainDepth);
    }
  }

  private void applyExplosionTile(int x, int y, int chainDepth) {
    explosions.add(new Explosion(x, y, 0.25f));
    TileType type = grid.get(x, y);
    if (type == TileType.SOFT) {
      grid.set(x, y, TileType.FLOOR);
      maybeDropPickup(x, y);
    } else if (type == TileType.EXIT_HIDDEN) {
      grid.set(x, y, TileType.EXIT_REVEALED);
      maybeDropPickup(x, y);
    }
    for (Bomb other : bombs) {
      if (!other.exploded && other.x == x && other.y == y) {
        explodeBomb(other, chainDepth + 1);
      }
    }
    for (Enemy enemy : enemies) {
      if (enemy.alive && enemy.gridX == x && enemy.gridY == y) {
        enemy.alive = false;
        score += enemy instanceof SpawnerEnemy ? 200 : 120;
        if (chainDepth > 0) {
          score += chainDepth * 50;
        }
      }
    }
    if (player.gridX == x && player.gridY == y) {
      hitPlayer();
    }
  }

  private void maybeDropPickup(int x, int y) {
    float roll = random.nextFloat();
    if (roll < 0.2f) {
      pickups.add(new Pickup(x, y, Pickup.Type.BOMB_PLUS));
    } else if (roll < 0.35f) {
      pickups.add(new Pickup(x, y, Pickup.Type.SPEED_PLUS));
    } else if (roll < 0.45f) {
      pickups.add(new Pickup(x, y, Pickup.Type.FUSE_MINUS));
    } else if (roll < 0.52f) {
      pickups.add(new Pickup(x, y, Pickup.Type.SHIELD));
    } else if (roll < 0.56f) {
      pickups.add(new Pickup(x, y, Pickup.Type.REMOTE));
    }
  }

  private void applyPickup(Pickup.Type type) {
    switch (type) {
      case BOMB_PLUS:
        player.maxBombs += 1;
        break;
      case SPEED_PLUS:
        player.speed = Math.min(2.0f, player.speed + 0.1f);
        break;
      case FUSE_MINUS:
        player.bombFuse = Math.max(0.7f, player.bombFuse - 0.1f);
        break;
      case SHIELD:
        player.shield = 1;
        break;
      case REMOTE:
        player.remoteDetonator = true;
        break;
    }
  }

  private void hitPlayer() {
    if (player.shield > 0) {
      player.shield = 0;
      return;
    }
    setState(GameState.GAME_OVER);
  }

  private void checkStageClear() {
    if (state != GameState.PLAYING) {
      return;
    }
    boolean enemiesAlive = false;
    for (Enemy enemy : enemies) {
      if (enemy.alive) {
        enemiesAlive = true;
        break;
      }
    }
    TileType tile = grid.get(player.gridX, player.gridY);
    if (!enemiesAlive && tile == TileType.EXIT_REVEALED) {
      int timeSeconds = (int) stageTimer;
      int bonus = Math.max(0, 300 - timeSeconds) * 5;
      score += bonus;
      setState(GameState.STAGE_CLEAR);
    }
  }

  public StatsSnapshot getStatsSnapshot() {
    int timeSeconds = (int) stageTimer;
    return new StatsSnapshot(stage, score, player.maxBombs, player.speed, player.shield, timeSeconds);
  }
}
