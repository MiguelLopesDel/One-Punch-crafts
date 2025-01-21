package com.onepunchcrafts.util;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.DateTimeException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class TickScheduler {

    private static final Map<Integer, TickTask> tasks = new ConcurrentHashMap<>();
    private static final Queue<Integer> tasksToCancel = new LinkedList<>();
    private static long ticksElapsed = 0;
    private static final Random rand = new Random();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase == TickEvent.Phase.START) {
            ticksElapsed++;
            for (Map.Entry<Integer, TickTask> task : tasks.entrySet()) {
                TickTask tickTask = task.getValue();
                try {
                    tickTask.executeTask(ticksElapsed);
                    if (tickTask.isDone())
                        tasksToCancel.add(task.getKey());
                } catch (Exception e) {
                    tasksToCancel.add(task.getKey());
                    System.out.println("stranger err " + e);
                }
            }
        }
        while (!tasksToCancel.isEmpty()) {
            Integer task = tasksToCancel.poll();
            tasks.remove(task).executeLastTask();
        }
    }

    public static int scheduleDuringAndWithInterval(Duration duration, Duration interval, Runnable task) {
        return schedule(checkAndConvertToTickTask(duration, interval, task));
    }

    public static int scheduleWithCondition(Duration interval, Callable<Boolean> task) {
        return schedule(checkAndConvertToTickTask(null, interval, task));
    }

    public static int scheduleFromHereWithLastExecution(Duration timeUnit, Runnable task, Runnable lastTask) {
        return schedule(checkAndConvertToTickTask(null, timeUnit, task, lastTask));
    }

    public static int scheduleFromHere(Duration timeUnit, Runnable task) {
        return schedule(checkAndConvertToTickTask(null, timeUnit, task));
    }

    private static int schedule(TickTask timeUnit) {
        int id;
        do {
            id = rand.nextInt();
        } while (tasks.containsKey(id));
        tasks.put(id, timeUnit);
        return id;
    }

    private static TickTask checkAndConvertToTickTask(Duration duration, Duration interval, Object task) {
        return checkAndConvertToTickTask(duration, interval, task, null);
    }

    private static TickTask checkAndConvertToTickTask(Duration duration, Duration interval, Object task, Object lastTask) {
        if (interval.isNegative() || interval.isZero())
            throw new DateTimeException("Duration is negative or zero");
        long millisInterval = interval.toMillis();
        if (millisInterval < 50)
            throw new DateTimeException("Duration less than 50 milliseconds");
        int ticksInterval = Math.round((float) millisInterval / 50);
        if (duration == null)
            return new TickTask(ticksInterval, ticksElapsed, task, lastTask);
        long millisDuration = duration.toMillis();
        return new TickTask(ticksInterval, Math.round((float) millisDuration / 50) / ticksInterval, ticksElapsed, task, lastTask);
    }
}

//package com.onepunchcrafts.util;
//
//import lombok.Getter;
//import net.minecraftforge.event.TickEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//import java.time.DateTimeException;
//import java.time.Duration;
//import java.util.*;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ConcurrentHashMap;
//
////In tests
//@Mod.EventBusSubscriber
//public class TickScheduler {
//
//    private static final Map<Integer, TickTask> tasks = new ConcurrentHashMap<>();
//    private static final Queue<Integer> tasksToCancel = new LinkedList<>();
//    private static long ticksElapsed = 0;
//    private static final Random rand = new Random();
//
//    @SubscribeEvent
//    public static void onServerTick(TickEvent.ServerTickEvent event) throws Exception {
//        if (event.phase == TickEvent.Phase.START) {
//            //Minha intenção é fazer isso fica mais preciso então como eu quero usar essa minha classe
//            //Eu quero só precisar puxas as TickTask que eu tenho e então executar elas
//            //caso elas não tenham um intervalo para cada execução por exemplo executar a cada 5 segundos
//            //Significa que é uma task de execução unica e então ela memso deve se remover
//            //Então aqui colocaria como eu acho que deveria pode usar ela
//            ///LivingJumpEventHandler logica daquele código era que se o intervalo de ticks era -1 ou seja não tinha invervalo entre as execuções
//            // então era uma tarefa que não tinha repetições e que só deveria ser executada uma vez
//            //por isso eu só verificava se ela tinha um delay inicial e se sim eu executava e cancelava e se não eu
//            //esperava a proxima vez até pode executar.
//            //ja no else if a logica era analisar se ja estava no intervalo de execuções se sim eu via quantas faltavam
//            //se não faltasse nenhuma então eu matava a task e se faltava eu executava e esperava
//            ticksElapsed++;
//            for (Map.Entry<Integer, TickTask> task : tasks.entrySet()) {
//                TickTask tickTask = task.getValue();
//                tickTask.executeTask(ticksElapsed);
//                if (tickTask.isDone())
//                    tasksToCancel.add(task.getKey());
//            }
//        }
//
//
//        ////
////normal
////            for (Integer id : tasks.keySet()) {
////                TickTask tickTask = tasks.get(id);
////                int tickInterval = tickTask.getTickInterval();
////                if (tickInterval == -1) {
////                    if (tickTask.elapsedTicks == tickTask.initialDelay) {
////                        tickTask.executeTask();
////                        tasksToCancel.add(id);
////                    } else tickTask.elapsedTicks++;
////                } else if (ticksElapsed % tickInterval == 0) {
////                    if (tickTask.checkAndUpdateExecutionStatus()) {
////                        tickTask.executeTask();
////                    } else {
////                        tasksToCancel.add(id);
////                    }
////                }
////            }
////        }
//        while (!tasksToCancel.isEmpty()) {
//            Integer task = tasksToCancel.poll();
//            tasks.remove(task);
//        }
//        if (tasks.isEmpty())
//            ticksElapsed = 0;
//    }
//
//    public static int scheduleWithCondition(Duration timeUnit, Callable<Boolean> task) {
//        int id;
//        do {
//            id = rand.nextInt();
//        } while (tasks.containsKey(id));
//        tasks.put(id, checkAndConvertToTickTask(timeUnit, task));
//        return id;
//    }
//
//    public static int scheduleFromHere(Duration timeUnit, Runnable task) {
//        int id;
//        do {
//            id = rand.nextInt();
//        } while (tasks.containsKey(id));
//        tasks.put(id, checkAndConvertToTickTask(timeUnit, task));
//        return id;
//    }
//
//    private static TickTask checkAndConvertToTickTask(Duration duration, Object task) {
//        if (duration.isNegative() || duration.isZero())
//            throw new DateTimeException("Duration is negative or zero");
//        long millis = duration.toMillis();
//        if (millis < 50)
//            throw new DateTimeException("Duration less than 50 milliseconds");
//        return new TickTask(Math.round((float) millis / 50), task);
//    }
//
//    /**
//     * Essa Classe é um auxiliar para lidar com operações envolvendo ticks, ela contem campos e mercadores para controlar
//     * em suas tasks o tempo que passou desde sua ultima execução quantas execuções faltam para o final o initialDelay que a task
//     * deve ter etc...
//     */
//    //normal
////    private static class TickTask {
////        private static final int hasNoPredeterminedExecutionsLeft = -100;
////        private final int initialDelay;
////        private int elapsedTicks;
////        private int executionsLeft;
////        @Getter
////        private final int tickInterval;
////        @Getter
////        private final Object task;
////
////        public TickTask(int tickInterval, int executions, Object task) {
////            this.initialDelay = 0;
////            this.tickInterval = tickInterval;
////            this.executionsLeft = executions;
////            this.task = task;
////        }
////
////        public TickTask(int tickInterval, int executions) {
////            this(tickInterval, executions, null);
////        }
////
////        public TickTask(int tickInterval, Object task) {
////            this(tickInterval, hasNoPredeterminedExecutionsLeft, task);
////        }
////
////        public TickTask(int tickInterval) {
////            this(tickInterval, hasNoPredeterminedExecutionsLeft);
////        }
////
////        public void executeTask() {
////            if (task instanceof Runnable runnable)
////                runnable.run();
//////            else if (task instanceof Consumer consumer)
//////                consumer.accept(null);
////        }
////
////        public boolean checkAndUpdateExecutionStatus() {
////            if (executionsLeft > 0) {
////                executionsLeft--;
////                return true;
////            } else return executionsLeft == hasNoPredeterminedExecutionsLeft;
////        }
////    }
//    private static class TickTask {
//        private static final int hasNoPredeterminedExecutionsLeft = -100;
//        private final int initialDelay;
//        private int elapsedTicks;
//        private boolean hasCondition;
//        private boolean taskWillCancel;
//        private long creationTick;
//        private int executionsLeft;
//        @Getter
//        private final int tickInterval;
//        @Getter
//        private final Object task;
//
//        public TickTask(int tickInterval, int executions, Object task) {
//            this.initialDelay = 0;
//            this.tickInterval = tickInterval;
//            this.executionsLeft = executions;
//            this.task = task;
//        }
//
//        public TickTask(int tickInterval, int executions) {
//            this(tickInterval, executions, null);
//            this.creationTick = ticksElapsed;
//        }
//
//        public TickTask(int tickInterval, Object task) {
//            this(tickInterval, hasNoPredeterminedExecutionsLeft, task);
//        }
//
//        public TickTask(int tickInterval) {
//            this(tickInterval, hasNoPredeterminedExecutionsLeft);
//        }
//
//        /**
//         * @return true significa que a
//         */
//        public void executeTask(long ticksElapsed) throws Exception {
//            if (task instanceof Runnable runnable) {
//                if (ticksElapsed - creationTick == tickInterval) {
//                    runnable.run();
//                    taskWillCancel = true;
//                }
//            } else if (task instanceof Callable<?> callable) {
//                if (((Callable<Boolean>) callable).call())
//                    taskWillCancel = true;
//            }
//
////            else if (task instanceof Consumer consumer)
////                consumer.accept(null);
//        }
//
////        public boolean checkAndUpdateExecutionStatus() {
////            if (executionsLeft > 0) {
////                executionsLeft--;
////                return true;
////            } else
////                return executionsLeft == hasNoPredeterminedExecutionsLeft;
////        }
//
//        public boolean isDone() {
//            if (hasPredeterminedExecutionsLeft()) {
//                if (executionsLeft > 0) {
//                    executionsLeft--;
//                    return false;
//                } else return true;
//            } else if (taskWillCancel) {
//                return true;
//            }
//            return false;
//        }
//
//        private boolean hasPredeterminedExecutionsLeft() {
//            return executionsLeft != hasNoPredeterminedExecutionsLeft;
//        }
//    }
//}
