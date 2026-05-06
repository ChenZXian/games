package com.android.boot.core;

public class StageManager {
  private final StageWave[][] stages;
  private int stageIndex;
  private int waveIndex;

  public StageManager() {
    stages = new StageWave[][]{
      {
        new StageWave(4, 0, 0, false),
        new StageWave(5, 1, 0, false),
        new StageWave(0, 0, 1, true)
      },
      {
        new StageWave(5, 1, 1, false),
        new StageWave(6, 2, 1, false),
        new StageWave(0, 1, 2, true)
      },
      {
        new StageWave(6, 2, 1, false),
        new StageWave(6, 2, 2, false),
        new StageWave(0, 0, 0, true)
      }
    };
    reset();
  }

  public void reset() {
    stageIndex = 0;
    waveIndex = 0;
  }

  public int getStageNumber() {
    return stageIndex + 1;
  }

  public int getWaveNumber() {
    return waveIndex + 1;
  }

  public StageWave getCurrentWave() {
    return stages[stageIndex][waveIndex];
  }

  public boolean isBossWave() {
    return stageIndex == 2 && waveIndex == 2;
  }

  public boolean advanceWave() {
    waveIndex++;
    if (waveIndex >= stages[stageIndex].length) {
      waveIndex = 0;
      stageIndex++;
    }
    return stageIndex < stages.length;
  }

  public static class StageWave {
    public final int internCount;
    public final int pmCount;
    public final int qaCount;
    public final boolean elite;

    public StageWave(int internCount, int pmCount, int qaCount, boolean elite) {
      this.internCount = internCount;
      this.pmCount = pmCount;
      this.qaCount = qaCount;
      this.elite = elite;
    }
  }
}
