package me.exeos.jaha.test.hooks;

import me.exeos.jaha.Jaha;
import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;

import java.util.ArrayList;
import java.util.List;

@Hook(target = "sun/management/RuntimeImpl")
public class AntiAntiAgent {

    @Apply
    public synchronized List<String> getInputArguments() {
        List<String> original = (List<String>) Jaha.callOriginalObjectMethod();
        List<String> filtered = new ArrayList<>();
        for (String s : original) {
            if (!s.startsWith("-javaagent:") &&
                    !s.startsWith("-agentlib:") &&
                    !s.startsWith("-agentpath:")) {
                filtered.add(s);
            }
        }

        return filtered;
    }
}
