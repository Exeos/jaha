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
        String os = System.getProperty("os.name").toLowerCase();
        String libName = os.contains("win") ? "jahanative.dll"
                : os.contains("mac") ? "libjahanative.dylib"
                  : "libjahanative.so";

        try (InputStream in = NativeDefine.class.getResourceAsStream("/" + libName)) {
            if (in == null) throw new FileNotFoundException(libName + " not found in resources");
            Path temp = Files.createTempFile("jahanative", libName.substring(libName.lastIndexOf('.')));
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.toAbsolutePath().toString());
        }
    }

    public static native Class<?> defineBootstrapClass(String name, byte[] data);
}
