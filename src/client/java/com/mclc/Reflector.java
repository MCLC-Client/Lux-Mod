package com.lux;

import net.minecraft.client.gui.screen.SplashOverlay;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflector {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.minecraft.client.gui.screen.SplashOverlay");
        System.out.println("--- FIELDS ---");
        for (Field f : clazz.getDeclaredFields()) {
            System.out.println(f.getName() + " : " + f.getType().getName() + " : " + f.getGenericType().getTypeName());
        }
        System.out.println("--- METHODS ---");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println(m.getName() + " : " + m.getReturnType().getName());
        }
    }
}
