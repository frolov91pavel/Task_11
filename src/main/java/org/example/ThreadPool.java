package org.example;

import java.util.List;

public interface ThreadPool {
    void start();

    void execute(Runnable runnable);

    void shutdown();

    List<Runnable> shutdownNow(); // возвращает невыполненные задачи
}