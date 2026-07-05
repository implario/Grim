package ac.grim.grimac.platform.minestom;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

import java.util.Objects;

/**
 * Holds the {@link EventNode} all of Grim's Minestom listeners attach to.
 * Created by the bootstrap before GrimAPI loads; detaching this node from its
 * parent removes every Grim listener at once.
 */
public final class GrimMinestomPlatform {

    private static volatile EventNode<Event> eventNode;

    private GrimMinestomPlatform() {
    }

    public static void init(EventNode<Event> node) {
        eventNode = Objects.requireNonNull(node);
    }

    public static EventNode<Event> eventNode() {
        return Objects.requireNonNull(eventNode, "GrimMinestom has not been initialized yet");
    }
}
