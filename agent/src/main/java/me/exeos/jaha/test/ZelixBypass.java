package me.exeos.jaha.test;

import me.exeos.jaha.Jaha;
import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;

@Hook(target = "java/lang/Integer")
public class ZelixBypass {

    @Apply
    public String toString() {
        return (String) Jaha.callOriginalObjectMethod();
    }
}
