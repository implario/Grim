package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.platform.minestom.api.ClientVersionResolver;
import ac.grim.grimac.platform.minestom.api.PermissionResolver;
import ac.grim.grimac.platform.minestom.sender.MinestomSenderFactory;
import com.github.retrooper.packetevents.PacketEvents;

import java.util.Objects;

/**
 * Static wiring shared between the loader and the player/sender wrappers,
 * mirroring the FabricPlatformServices pattern. Populated once by
 * {@code GrimMinestom.builder().build()} before GrimAPI loads.
 */
public final class MinestomPlatformServices {

    private static volatile MinestomSenderFactory senderFactory;
    private static volatile PermissionResolver permissionResolver = PermissionResolver.DEFAULTS_ONLY;
    private static volatile ClientVersionResolver clientVersionResolver =
            player -> PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();

    private MinestomPlatformServices() {
    }

    public static void init(MinestomSenderFactory factory,
                            PermissionResolver permissions,
                            ClientVersionResolver clientVersions) {
        senderFactory = Objects.requireNonNull(factory);
        permissionResolver = Objects.requireNonNull(permissions);
        clientVersionResolver = Objects.requireNonNull(clientVersions);
    }

    public static MinestomSenderFactory senderFactory() {
        return Objects.requireNonNull(senderFactory, "GrimMinestom has not been initialized yet");
    }

    public static PermissionResolver permissionResolver() {
        return permissionResolver;
    }

    public static ClientVersionResolver clientVersionResolver() {
        return clientVersionResolver;
    }
}
