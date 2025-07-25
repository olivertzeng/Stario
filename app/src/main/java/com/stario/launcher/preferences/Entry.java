/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.preferences;

import androidx.annotation.NonNull;

public enum Entry {
    CATEGORY_APPLICATION_MAP("CATEGORY_APPLICATION_MAP"),
    APPLICATION_LABELS("APPLICATION_LABELS"),
    CATEGORY_NAMES("CATEGORY_NAMES"),
    CATEGORY_MAP("CATEGORY_MAP"),
    HIDDEN_APPS("HIDDEN_APPS"),
    CATEGORIES("CATEGORIES"),
    BRIEFING("BRIEFING"),
    WEATHER("WEATHER"),
    WIDGETS("WIDGETS"),
    SEARCH("SEARCH"),
    STARIO("STARIO"),
    DRAWER("DRAWER"),
    SHEET("SHEET"),
    THEME("THEME"),
    ICONS("ICONS");

    private final String serialized;

    Entry(String serialized) {
        this.serialized = "com.stario." + serialized;
    }

    public static boolean isValid(String serialized) {
        if (serialized != null && !serialized.isEmpty()) {
            for (Entry entry : Entry.values()) {
                if (serialized.startsWith(entry.serialized)) {
                    return true;
                }
            }
        }

        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return serialized;
    }

    public String toSubPreference(String name) {
        return toString() + "." + name;
    }
}
