package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class FixedThreadPool implements ThreadPool {

    private final int threadCount;
    private final BlockingQueue<Runnable> taskQueue;
    private final List<Thread> threads;

    private volatile boolean isRunning;

    public FixedThreadPool(int threadCount) {
        this.threadCount = threadCount;
        this.taskQueue = new LinkedBlockingQueue<>();
        this.threads = new CopyOnWriteArrayList<>();
    }

    @Override
    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        for (int i = 0; i < threadCount; i++) {
            Thread worker = new Thread(this::workerRun, "FixedPool-Worker-" + i);
            threads.add(worker);
            worker.start();
        }
    }

    private void workerRun() {
        while (isRunning || !taskQueue.isEmpty()) {
            try {
                // Если очередь пуста, блокируемся
                // Если isRunning = false, но в очереди еще могут быть задачи
                Runnable task = taskQueue.poll();
                if (task == null) {
                    // Пытаемся взять задачу блокирующим методом,
                    // но если isRunning=false и очередь пуста, выйдем
                    if (!isRunning && taskQueue.isEmpty()) {
                        break;
                    }
                    task = taskQueue.take(); // блокируемся
                }
                task.run();
            } catch (InterruptedException e) {
                // Поток прерван (shutdownNow?)
                // Прерываемся, если пул остановлен
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void execute(Runnable runnable) {
        if (!isRunning) {
            throw new IllegalStateException("ThreadPool is not running");
        }
        taskQueue.offer(runnable);
    }

    @Override
    public void shutdown() {
        // Запрещаем принимать новые задачи
        isRunning = false;
        // Не прерываем потоки, они добьют очередь
    }

    @Override
    public List<Runnable> shutdownNow() {
        // Запрещаем принимать новые задачи
        isRunning = false;
        // Прерываем потоки
        for (Thread t : threads) {
            t.interrupt();
        }
        // Возвращаем список непросмотренных задач
        List<Runnable> remainingTasks = new ArrayList<>();
        taskQueue.drainTo(remainingTasks);
        return remainingTasks;
    }
}