package ru.rufp.tvlauncher;

import android.graphics.Typeface;

final class Fonts {

    private static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    static Typeface get(int weight) {
        if (weight >= 600) return MEDIUM;
        if (weight <= 300) return LIGHT;
        return Typeface.SANS_SERIF;
    }

    private Fonts() {}
}
