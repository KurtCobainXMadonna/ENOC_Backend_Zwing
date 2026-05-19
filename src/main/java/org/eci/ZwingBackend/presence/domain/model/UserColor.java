package org.eci.ZwingBackend.presence.domain.model;

import java.util.Arrays;
import java.util.List;


public enum UserColor {
    PINK     ("#E91E63"),
    BLUE     ("#2196F3"),
    GREEN    ("#4CAF50"),
    ORANGE   ("#FF9800"),
    PURPLE   ("#9C27B0"),
    TEAL     ("#009688"),
    RED      ("#F44336"),
    AMBER    ("#FFC107");

    private final String hex;

    UserColor(String hex) {
        this.hex = hex;
    }

    public String hex() {
        return hex;
    }

    public static List<String> allHex() {
        return Arrays.stream(values()).map(UserColor::hex).toList();
    }
}
