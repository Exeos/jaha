package me.exeos.jaha.test;

import me.exeos.jaha.Jaha;
import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;

@Hook(target = "java/lang/Integer")
public class ToStringHook {

    @Apply
    public String toString() {
        String original = (String) Jaha.callOriginalObjectMethod();
        return "This has been hooked: " + original;
    }
}
