package com.onepunchcrafts.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;

import java.time.DateTimeException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class TickClientScheduler {

    private static final Map<Integer, TickTask> tasks = new ConcurrentHashMap<>();
    private static final Queue<Integer> tasksToCancel = new LinkedList<>();
    private static long ticksElapsed = 0;
    private static final Random rand = new Random();

    public static void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase == TickEvent.Phase.START) {
            if (Minecraft.getInstance().isPaused())
                return;
            ticksElapsed++;
            for (Map.Entry<Integer, TickTask> task : tasks.entrySet()) {
                TickTask tickTask = task.getValue();
                tickTask.executeTask(ticksElapsed);
                if (tickTask.isDone())
                    tasksToCancel.add(task.getKey());
            }
        }
        cancelAllTasks();
    }

    private static void cancelAllTasks() {
        while (!tasksToCancel.isEmpty()) {
            Integer task = tasksToCancel.poll();
            tasks.remove(task);
        }
    }

    public static void cancelTask(int id) {
        tasksToCancel.add(id);
    }

    public static int scheduleDuringAndWithInterval(Duration duration, Duration interval, Runnable task) {
        return schedule(checkAndConvertToTickTask(duration, interval, task));
    }

    public static int scheduleWithCondition(Duration interval, Callable<Boolean> task) {
        return schedule(checkAndConvertToTickTask(null, interval, task));
    }

    public static int scheduleFromHere(Duration timeUnit, Runnable task) {
        return schedule(checkAndConvertToTickTask(null, timeUnit, task));
    }

    private static int schedule(TickTask duration) {
        int id;
        do {
            id = rand.nextInt();
        } while (tasks.containsKey(id));
        tasks.put(id, duration);
        return id;
    }

    private static TickTask checkAndConvertToTickTask(Duration duration, Duration interval, Object task) {
        if (interval.isNegative() || interval.isZero())
            throw new DateTimeException("Duration is negative or zero");
        long millisInterval = interval.toMillis();
        if (millisInterval < 50)
            throw new DateTimeException("Duration less than 50 milliseconds");
        int ticksInterval = Math.round((float) millisInterval / 50);
        if (duration == null)
            return new TickTask(ticksInterval, ticksElapsed, task);
        long millisDuration = duration.toMillis();
        return new TickTask(ticksInterval, Math.round((float) millisDuration / 50) / ticksInterval, ticksElapsed, task);
    }
}
