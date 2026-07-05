package ac.grim.grimac.platform.minestom.manager;

import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.minestom.sender.MinestomSenderFactory;

/**
 * Minestom has no server-side permission registry; registered defaults feed the
 * sender factory's resolution table (consulted when the network's
 * PermissionResolver abstains).
 */
public final class MinestomPermissionRegistrationManager implements PermissionRegistrationManager {

    private final MinestomSenderFactory senderFactory;

    public MinestomPermissionRegistrationManager(MinestomSenderFactory senderFactory) {
        this.senderFactory = senderFactory;
    }

    @Override
    public void registerPermission(String name, PermissionDefaultValue defaultValue) {
        senderFactory.registerPermissionDefault(name, defaultValue);
    }
}
