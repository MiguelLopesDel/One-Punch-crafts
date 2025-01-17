package com.onepunchcrafts.util;

import lombok.Getter;

import java.util.concurrent.Callable;

public class TickTask {

    private static final int hasNoPredeterminedExecutionsLeft = -100;
    private final int initialDelay;
    private int elapsedTicks;
    private boolean hasCondition;
    private boolean taskWillCancel;
    private long creationTick;
    private int executionsLeft;
    @Getter
    private final int tickInterval;
    @Getter
    private final Object task;

    public TickTask(int tickInterval, int executions, long creationTick, Object task) {
        this.initialDelay = 0;
        this.tickInterval = tickInterval;
        this.executionsLeft = executions;
        this.creationTick = creationTick;
        this.task = task;
    }

    public TickTask(int tickInterval, long tickCreation, Object task) {
        this(tickInterval, hasNoPredeterminedExecutionsLeft, tickCreation, task);
    }

    public TickTask(int tickInterval, int executions, int creationTick) {
        this(tickInterval, executions, creationTick, null);
    }

    public void executeTask(long currentTick) throws Exception {
        if (task instanceof Runnable runnable) {
            if ((currentTick - creationTick) % tickInterval == 0) {
                runnable.run();
                taskWillCancel = true;
            }
        } else if (task instanceof Callable<?> callable && currentTick - creationTick >= tickInterval && ((Callable<Boolean>) callable).call())
            taskWillCancel = true;
    }

    public boolean isDone() {
        if (hasPredeterminedExecutionsLeft()) {
            if (executionsLeft > 0) {
                executionsLeft--;
                return false;
            } else return true;
        } else return taskWillCancel;
    }

    private boolean hasPredeterminedExecutionsLeft() {
        return executionsLeft != hasNoPredeterminedExecutionsLeft;
    }
}
