package com.android.boot.grid;

public class Grid {
  private final int width;
  private final int height;
  private final TileType[] tiles;

  public Grid(int width, int height) {
    this.width = width;
    this.height = height;
    this.tiles = new TileType[width * height];
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean inBounds(int x, int y) {
    return x >= 0 && y >= 0 && x < width && y < height;
  }

  public TileType get(int x, int y) {
    return tiles[x + y * width];
  }

  public void set(int x, int y, TileType type) {
    tiles[x + y * width] = type;
  }

  public boolean isWalkable(int x, int y) {
    TileType type = get(x, y);
    return type == TileType.FLOOR || type == TileType.EXIT_REVEALED;
  }

  public boolean isBlockingExplosion(int x, int y) {
    return get(x, y) == TileType.SOLID;
  }
}
