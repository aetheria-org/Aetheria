package io.hamlook.aetheria.features.waypoints;

import io.hamlook.aetheria.repo.RepoLinks;
import io.hamlook.aetheria.utils.UrlOpener;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;

public final class WaypointLinks {

    private WaypointLinks() {
    }

    public static String site9() {
        return RepoLinks.site9();
    }

    public static boolean hasSite9() {
        return site9() != null;
    }

    public static WaypointResult openSite9() {
        String url = site9();
        if (url == null) return WaypointResult.fail(Strings.TOAST_SITE9_UNSET);
        if (UrlOpener.open(url)) return WaypointResult.ok(Strings.TOAST_SITE9_OPENED);
        ClipboardCompat.setClipboard(url);
        return WaypointResult.fail(Strings.TOAST_SITE9_COPIED);
    }
}
