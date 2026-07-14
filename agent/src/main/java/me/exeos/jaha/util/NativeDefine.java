package me.exeos.jaha.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeDefine {

    static {
        try {
            loadFromResources();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    private static void loadFromResources() throws IOException {
        String libName = getLibName();

        try (InputStream in = NativeDefine.class.getResourceAsStream("/" + libName)) {
            if (in == null) {
                throw new FileNotFoundException(libName + " not found in resources. Platform might not be supported.");
            }

            Path temp = Files.createTempFile(libName, "");
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.toAbsolutePath().toString());
        }
    }

    private static String getLibName() {
        String os = System.getProperty("os.name").split(" ")[0].toLowerCase();
        String arch = System.getProperty("os.arch")
                .replace("amd64", "x64")
                .replace("x86_64", "x64");

        return "jaha." + os + "-" + arch;
    }

    public static native Class<?> defineBootstrapClass(String name, byte[] data);
}
