package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.sender.Sender;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.server.ServerTickMonitorEvent;

public final class MinestomPlatformServer implements PlatformServer {

    private static final int TPS_SAMPLES = 100;

    private final double[] tickTimesMillis = new double[TPS_SAMPLES];
    private volatile int tickCursor;
    private volatile boolean bufferFilled;

    public MinestomPlatformServer(EventNode<net.minestom.server.event.Event> eventNode) {
        eventNode.addListener(ServerTickMonitorEvent.class, event -> {
            int cursor = tickCursor;
            tickTimesMillis[cursor] = event.getTickMonitor().getTickTime();
            int next = cursor + 1;
            if (next >= TPS_SAMPLES) {
                bufferFilled = true;
                next = 0;
            }
            tickCursor = next;
        });
    }

    @Override
    public String getPlatformImplementationString() {
        return "Minestom " + MinecraftServer.VERSION_NAME;
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        sender.performCommand(command);
    }

    @Override
    public Sender getConsoleSender() {
        return MinestomPlatformServices.senderFactory()
                .wrap(MinecraftServer.getCommandManager().getConsoleSender());
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        // Minestom needs no channel registration to send plugin messages.
    }

    @Override
    public double getTPS() {
        int samples = bufferFilled ? TPS_SAMPLES : tickCursor;
        if (samples == 0) return ServerFlag.SERVER_TICKS_PER_SECOND;
        double total = 0;
        for (int i = 0; i < samples; i++) {
            total += tickTimesMillis[i];
        }
        double averageTickMillis = total / samples;
        double tickPeriodMillis = 1000.0 / ServerFlag.SERVER_TICKS_PER_SECOND;
        double effectivePeriod = Math.max(averageTickMillis, tickPeriodMillis);
        return Math.min(ServerFlag.SERVER_TICKS_PER_SECOND, 1000.0 / effectivePeriod);
    }
}
