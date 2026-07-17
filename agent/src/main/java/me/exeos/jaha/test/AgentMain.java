package me.exeos.jaha.test;

import me.exeos.jaha.Jaha;
import me.exeos.jaha.test.hooks.AntiAntiAgent;

import java.lang.instrument.Instrumentation;

public class AgentMain {

    public static void premain(String idk, Instrumentation is) {
        Jaha.register(AntiAntiAgent.class);

        Jaha.applyHooks(is);
    }
}
