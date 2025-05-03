package me.zombie_striker.psudocommands;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * From me.lucko.commodore.Commodore, then modified
 */
final class ReflectionUtil {
    private static final String SERVER_VERSION = getServerVersion();
    public static final boolean USING_PAPER;
    public static final int VERSION, VERSION_MINOR;

    static {
        // Example value of getBukkitVersion: 1.20.1-R0.1-SNAPSHOT
        String[] versions = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        VERSION = Integer.parseInt(versions[1]);
        VERSION_MINOR = versions.length == 2 ? 0 : Integer.parseInt(versions[2]);

        boolean paper = true;
        try {
            // TODO: find a more rebust way to detect Paper
            Class.forName("com.destroystokyo.paper.entity.Pathfinder");
        } catch (ClassNotFoundException e) {
            paper = false;
        }
        USING_PAPER = paper;
    }

    /**
     * Returns true if the running version is below or is the Minecraft version given in input, e.g. "1.21.1".
     */
    public static boolean runningBelowVersion(String testedVersion) {
        String[] parts = testedVersion.split("\\.");

        int inputVersion = Integer.parseInt(parts[1]);
        int inputVersionMinor = parts.length == 2 ? 0 : Integer.parseInt(parts[2]);

        return ReflectionUtil.VERSION < inputVersion || (ReflectionUtil.VERSION == inputVersion && ReflectionUtil.VERSION_MINOR <= inputVersionMinor);
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... params) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(name, params);
        method.setAccessible(true);
        return method;
    }

    private static String getServerVersion() {
        Class<?> server = Bukkit.getServer().getClass();
        if (!server.getSimpleName().equals("CraftServer")) {
            return ".";
        }
        if (server.getName().equals("org.bukkit.craftbukkit.CraftServer")) {
            // Non versioned class
            return ".";
        } else {
            String version = server.getName().substring("org.bukkit.craftbukkit".length());
            return version.substring(0, version.length() - "CraftServer".length());
        }
    }

    public static String mc(String name) {
        return "net.minecraft." + name;
    }

    public static String nms(String className) {
        return "net.minecraft.server" + SERVER_VERSION + className;
    }

    public static Class<?> mcClass(String className) throws ClassNotFoundException {
        return Class.forName(mc(className));
    }

    public static Class<?> nmsClass(String className) throws ClassNotFoundException {
        return Class.forName(nms(className));
    }

    public static String obc(String className) {
        return "org.bukkit.craftbukkit" + SERVER_VERSION + className;
    }

    public static Class<?> obcClass(String className) throws ClassNotFoundException {
        return Class.forName(obc(className));
    }

    public static int minecraftVersion() {
        try {
            final Matcher matcher = Pattern.compile("\\(MC: (\\d)\\.(\\d+)\\.?(\\d+?)?( .*)?\\)").matcher(Bukkit.getVersion());
            if (matcher.find()) {
                return Integer.parseInt(matcher.toMatchResult().group(2), 10);
            } else {
                throw new IllegalArgumentException(String.format("No match found in '%s'", Bukkit.getVersion()));
            }
        } catch (final IllegalArgumentException ex) {
            throw new RuntimeException("Failed to determine Minecraft version", ex);
        }
    }
}