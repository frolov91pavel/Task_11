package org.example;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FixedThreadPool implements ThreadPool {

    private final int threadCount;
    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private final List<Thread> threads = new ArrayList<>();
    private volatile boolean isRunning;

    public FixedThreadPool(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public synchronized void start() {
        if (isRunning) {
            return; // уже запущен
        }
        isRunning = true;

        // Создаём заданное количество потоков-воркеров
        for (int i = 0; i < threadCount; i++) {
            Thread worker = new Thread(() -> {
                while (isRunning) {
                    Runnable task;
                    synchronized (taskQueue) {
                        // Ждём, пока появится задача или пул не завершится
                        while (taskQueue.isEmpty() && isRunning) {
                            try {
                                taskQueue.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        if (!isRunning) {
                            break;
                        }
                        // Берём задачу из очереди
                        task = taskQueue.poll();
                    }
                    // Выполняем задачу вне синхронизированного блока
                    if (task != null) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            // обрабатываем ошибку задачи, если нужно
                            e.printStackTrace();
                        }
                    }
                }
            }, "FixedPool-Worker-" + i);
            threads.add(worker);
            worker.start();
        }
    }

    @Override
    public void execute(Runnable runnable) {
        synchronized (taskQueue) {
            taskQueue.add(runnable);
            // Будим один из потоков-воркеров (если он ждёт)
            taskQueue.notify();
        }
    }
}