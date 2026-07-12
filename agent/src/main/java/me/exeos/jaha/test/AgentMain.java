package me.exeos.jaha.test;

import me.exeos.jaha.Jaha;

import java.lang.instrument.Instrumentation;

public class AgentMain {

    public static void premain(String idk, Instrumentation is) {
        Jaha.register(ZelixBypass.class);

        Jaha.load(is);
    }
}
