package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import net.minestom.server.timer.Task;

import java.util.concurrent.Future;

public final class MinestomTaskHandle implements TaskHandle {

    private final Runnable canceller;
    private final java.util.function.BooleanSupplier cancelledCheck;
    private final boolean sync;

    private MinestomTaskHandle(Runnable canceller, java.util.function.BooleanSupplier cancelledCheck, boolean sync) {
        this.canceller = canceller;
        this.cancelledCheck = cancelledCheck;
        this.sync = sync;
    }

    public static MinestomTaskHandle of(Task task) {
        return new MinestomTaskHandle(task::cancel, () -> !task.isAlive(), true);
    }

    public static MinestomTaskHandle of(Future<?> future) {
        return new MinestomTaskHandle(() -> future.cancel(false), future::isCancelled, false);
    }

    @Override
    public boolean isSync() {
        return sync;
    }

    @Override
    public boolean isCancelled() {
        return cancelledCheck.getAsBoolean();
    }

    @Override
    public void cancel() {
        canceller.run();
    }
}
