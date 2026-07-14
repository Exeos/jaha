package me.exeos.jaha.runtime;

import me.exeos.jaha.util.NativeDefine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeLoader {

    private static boolean loaded = false;

    public static void ensureLoaded() {
        if (!loaded) {
            String libName = getLibName();

            try (InputStream in = NativeDefine.class.getResourceAsStream("/" + libName)) {
                if (in == null) {
                    throw new FileNotFoundException(libName + " not found in resources. Platform might not be supported.");
                }

                Path temp = Files.createTempFile(libName, "");
                temp.toFile().deleteOnExit();
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                System.load(temp.toAbsolutePath().toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            loaded = true;
        }
    }

    private static String getLibName() {
        String os = System.getProperty("os.name").split(" ")[0].toLowerCase();
        String arch = System.getProperty("os.arch")
                .replace("amd64", "x64")
                .replace("x86_64", "x64");

        return "jaha." + os + "-" + arch;
    }
}
