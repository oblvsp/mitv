package ru.rufp.tvlauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class Apps {

    static final String[] DOCK = {
            "org.smarttube.beta",
            "ru.kinopoisk.tv",
            "ru.more.play",
            "ru.vk.store.tv",
    };

    static class AppInfo {
        String pkg;
        CharSequence label;
        Drawable icon;
        Intent launch;
        boolean folder;
    }

    static List<AppInfo> loadFolder(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_MAIN);
        probe.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(probe, 0);
        List<String> dock = Arrays.asList(DOCK);
        List<AppInfo> out = new ArrayList<>();
        for (ResolveInfo ri : ris) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName()) || dock.contains(pkg)) continue;
            if ("com.android.tv.settings".equals(pkg)) continue;
            AppInfo a = new AppInfo();
            a.pkg = pkg;
            a.label = ri.loadLabel(pm);
            a.icon = ri.loadIcon(pm);
            a.launch = pm.getLaunchIntentForPackage(pkg);
            if (a.launch == null) continue;
            out.add(a);
        }
        Collator col = Collator.getInstance(new Locale("ru"));
        Collections.sort(out, (x, y) -> col.compare(x.label.toString(), y.label.toString()));
        return out;
    }

    static AppInfo folderItem(List<AppInfo> apps) {
        AppInfo f = new AppInfo();
        f.pkg = "folder";
        f.label = "Приложения";
        f.icon = new FolderIcon(apps);
        f.folder = true;
        return f;
    }

    static Intent launchIntentFor(Context ctx, String pkg) {
        PackageManager pm = ctx.getPackageManager();
        Intent li = pm.getLaunchIntentForPackage(pkg);
        if (li != null) return li;
        Intent probe = new Intent(Intent.ACTION_MAIN);
        probe.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        probe.setPackage(pkg);
        List<ResolveInfo> ris = pm.queryIntentActivities(probe, 0);
        if (!ris.isEmpty()) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            i.setClassName(pkg, ris.get(0).activityInfo.name);
            return i;
        }
        return null;
    }

    static Drawable iconFor(Context ctx, Intent launch, String pkg) {
        PackageManager pm = ctx.getPackageManager();
        try {
            return pm.getActivityIcon(launch);
        } catch (Exception e) {
            try {
                return pm.getApplicationIcon(pkg);
            } catch (Exception e2) {
                return pm.getDefaultActivityIcon();
            }
        }
    }

    static List<AppInfo> loadDock(Context ctx, boolean settingsLast) {
        PackageManager pm = ctx.getPackageManager();
        List<AppInfo> out = new ArrayList<>();
        for (String pkg : DOCK) {
            Intent li = launchIntentFor(ctx, pkg);
            if (li == null) continue;
            AppInfo a = new AppInfo();
            a.pkg = pkg;
            a.launch = li;
            try {
                a.label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                a.icon = pm.getApplicationIcon(pkg);
            } catch (Exception e) {
                continue;
            }
            out.add(a);
        }
        if (settingsLast) {
            Intent si = launchIntentFor(ctx, "com.android.tv.settings");
            if (si != null) {
                AppInfo s = new AppInfo();
                s.pkg = "com.android.tv.settings";
                s.launch = si;
                s.label = "Настройки";
                s.icon = iconFor(ctx, si, "com.android.tv.settings");
                out.add(s);
            }
        }
        return out;
    }

    private Apps() {}
}
