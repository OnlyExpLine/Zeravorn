package com.zeravorn.minion;

public record WaveComposition(int melee, int ranged, int siege) {
    public WaveComposition { if (melee < 0 || ranged < 0 || siege < 0) throw new IllegalArgumentException("negative composition"); }
}
