package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ScalableThreadPool implements ThreadPool {

    private final int minThreads;
    private final int maxThreads;
    private final BlockingQueue<Runnable> taskQueue;
    private final List<Thread> threads;
    private final AtomicInteger activeThreads; // сколько потоков сейчас реально запущено

    private volatile boolean isRunning;

    public ScalableThreadPool(int minThreads, int maxThreads) {
        if (minThreads <= 0 || maxThreads < minThreads) {
            throw new IllegalArgumentException("Invalid min/max");
        }
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.taskQueue = new LinkedBlockingQueue<>();
        this.threads = new CopyOnWriteArrayList<>();
        this.activeThreads = new AtomicInteger(0);
    }

    @Override
    public synchronized void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        // Запускаем сразу min потоков
        for (int i = 0; i < minThreads; i++) {
            createWorker();
        }
    }

    private void createWorker() {
        Thread worker = new Thread(this::workerRun, "ScalablePool-Worker-" + System.nanoTime());
        threads.add(worker);
        activeThreads.incrementAndGet();
        worker.start();
    }

    private void workerRun() {
        while (isRunning || !taskQueue.isEmpty()) {
            try {
                Runnable task = taskQueue.poll(500, TimeUnit.MILLISECONDS);
                if (task == null) {
                    // Если нет задания
                    if (!isRunning && taskQueue.isEmpty()) {
                        break; // выходим, все дела сделаны
                    }
                    // Если потоков больше min, можем убрать «лишний» поток
                    // т.к. нет работы
                    if (threads.size() > minThreads) {
                        threads.remove(Thread.currentThread());
                        activeThreads.decrementAndGet();
                        break;
                    }
                    // иначе ждём новые задачи
                    continue;
                }
                // получили задачу -> выполняем
                task.run();
            } catch (InterruptedException e) {
                // Прерван
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Выходим из цикла
        threads.remove(Thread.currentThread());
        activeThreads.decrementAndGet();
    }

    @Override
    public void execute(Runnable runnable) {
        if (!isRunning) {
            throw new IllegalStateException("ThreadPool is not running");
        }
        taskQueue.offer(runnable);
        scaleUpIfNeeded();
    }

    private synchronized void scaleUpIfNeeded() {

        if (threads.size() < maxThreads) {
            // Добавляем еще один поток
            createWorker();
        }
    }

    @Override
    public void shutdown() {
        isRunning = false;
    }

    @Override
    public List<Runnable> shutdownNow() {
        isRunning = false;
        // Прерываем все потоки
        for (Thread t : threads) {
            t.interrupt();
        }
        // Возвращаем оставшиеся в очереди задания
        List<Runnable> remains = new ArrayList<>();
        taskQueue.drainTo(remains);
        return remains;
    }
}