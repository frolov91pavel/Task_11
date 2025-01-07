package org.example;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ScalableThreadPool implements ThreadPool {

    private final int minThreads;
    private final int maxThreads;

    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private final List<Thread> threads = new ArrayList<>();

    private volatile boolean isRunning;

    public ScalableThreadPool(int minThreads, int maxThreads) {
        if (minThreads <= 0 || maxThreads < minThreads) {
            throw new IllegalArgumentException("Некорректные параметры min/max");
        }
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
    }

    @Override
    public synchronized void start() {
        if (isRunning) {
            return; // уже запущен
        }
        isRunning = true;

        // Изначально запускаем min потоков
        for (int i = 0; i < minThreads; i++) {
            createWorker();
        }
    }

    @Override
    public void execute(Runnable runnable) {
        if (!isRunning) {
            throw new IllegalStateException("Пул не запущен или остановлен");
        }

        // Добавляем задачу в очередь
        synchronized (taskQueue) {
            taskQueue.add(runnable);
            taskQueue.notify(); // будим один поток
        }

        // Проверяем, можно ли создать новый поток (если все заняты и не достигнут max)
        scaleUpIfNeeded();
    }

    // Создаём поток-воркер
    private void createWorker() {
        Thread worker = new Thread(() -> {
            while (isRunning) {
                Runnable task;
                synchronized (taskQueue) {
                    // Если нет задач, подождём какое-то время
                    while (taskQueue.isEmpty() && isRunning) {
                        try {
                            // Ждём 1 секунду
                            taskQueue.wait(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        // Если снова пусто и потоков больше, чем min, можно завершиться
                        if (taskQueue.isEmpty() && threads.size() > minThreads) {
                            // Удаляем текущий поток из списка и выходим
                            threads.remove(Thread.currentThread());
                            return;
                        }
                    }
                    if (!isRunning) {
                        break;
                    }
                    // Берём задачу
                    task = taskQueue.poll();
                }
                // Выполняем задачу вне synchronized-блока
                if (task != null) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "ScalablePool-Worker-" + System.identityHashCode(this));
        threads.add(worker);
        worker.start();
    }

    // При необходимости создаём новый поток (scale-up),
    // если в очереди есть задания и все потоки заняты
    private void scaleUpIfNeeded() {
        synchronized (taskQueue) {
            // Проверка грубая: если число задач > 0 и
            // число потоков < maxThreads, то добавить ещё один поток.
            // Можно усложнить логику, проверять занятость и т.д.
            if (!taskQueue.isEmpty() && threads.size() < maxThreads) {
                createWorker();
            }
        }
    }
}