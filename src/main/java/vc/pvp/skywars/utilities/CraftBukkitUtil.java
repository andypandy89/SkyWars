package vc.pvp.skywars.utilities;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public class CraftBukkitUtil {

    public static final String BUKKIT_PACKAGE;
    public static final String MINECRAFT_PACKAGE;

    public static Object getHandle(@Nonnull Object target) {
        return getMethod(target, "getHandle", new Class<?>[0], new Object[0]);
    }

    public static Object getMethod(@Nonnull Object target, @Nonnull String methodName) {
        return getMethod(target, methodName, new Class<?>[0], new Object[0]);
    }

    public static Object getMethod(@Nonnull Object target, @Nonnull String methodName,
                                   @Nonnull Class<?>[] paramTypes, @Nonnull Object[] params) {
        Preconditions.checkNotNull(target, "Target is null");
        Preconditions.checkNotNull(methodName, "Method name is null");

        Class<?> currentClazz = target.getClass();
        Object returnValue = null;

        do {
            try {
                Method method = currentClazz.getDeclaredMethod(methodName, paramTypes);
                returnValue = method.invoke(target, params);

            } catch (Exception exception) {
                currentClazz = currentClazz.getSuperclass();
            }

        } while (currentClazz != null && currentClazz.getSuperclass() != null && returnValue == null);

        return returnValue;
    }

    public static void forceRespawn(Player player) {
        Preconditions.checkNotNull(player, "Player is null");

        if (!player.isDead()) {
            return;
        }

        Object playerHandle = getHandle(player);
        if (playerHandle == null) return;

        Object serverHandle = getHandle(Bukkit.getServer());
        if (serverHandle == null) return;

        serverHandle = getMethod(serverHandle, "getServer", new Class<?>[0], new Object[0]);
        if (serverHandle == null) return;

        Object playerListHandle = getMethod(serverHandle, "getPlayerList", new Class<?>[0], new Object[0]);
        if (playerListHandle == null) return;

        getMethod(playerListHandle, "moveToWorld", new Class<?>[] { playerHandle.getClass(), int.class, boolean.class }, new Object[] { playerHandle, 0, false });
    }

    public static boolean isRunning() {
        Object minecraftServer = getMethod(Bukkit.getServer(), "getServer");
        if (minecraftServer == null) {
            return false;
        }

        Object isRunning = getMethod(minecraftServer, "isRunning");
        return isRunning instanceof Boolean && (Boolean) isRunning;
    }

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String bukkitVersion = packageName.substring(packageName.lastIndexOf('.') + 1);

        BUKKIT_PACKAGE = "org.bukkit.craftbukkit." + bukkitVersion;
        MINECRAFT_PACKAGE = "net.minecraft.server." + bukkitVersion;
    }
}
