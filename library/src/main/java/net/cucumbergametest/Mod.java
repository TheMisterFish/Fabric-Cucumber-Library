package net.cucumbergametest;

import net.fabricmc.api.ModInitializer;

public class Mod implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("Mod Thread: " + Thread.currentThread().getContextClassLoader());
        System.out.println("Mod Classloader: " + Mod.class.getClassLoader());
    }
}
