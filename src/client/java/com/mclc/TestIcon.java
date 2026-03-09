package com.lux;

import net.minecraft.client.util.Icons;

public class TestIcon {
    public static void printIcons() {
        for (Object o : Icons.values()) {
            System.out.println("TEST_ICON_OUTPUT: " + o.toString());
        }
        for (java.lang.reflect.Method m : Icons.class.getDeclaredMethods()) {
            System.out.println("TEST_ICON_METHOD: " + m.getName() + " returns " + m.getReturnType().getName());
        }
        for (java.lang.reflect.Field f : Icons.class.getDeclaredFields()) {
            System.out.println("TEST_ICON_FIELD: " + f.getName() + " type " + f.getType().getName());
        }
    }
}
