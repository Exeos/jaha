package me.exeos.jaha.test;

import me.exeos.jaha.Jaha;
import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;

@Hook(target = "SecureClass")
public class SCHook {

    @Apply
    public String deriveKey(int n) {
        String original = (String) Jaha.callOriginalObjectMethod(1);

        return original + "_lol";
    }
}
