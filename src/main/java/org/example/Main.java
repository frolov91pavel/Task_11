package org.example;

/*
1. Как получить ссылку на текущий поток ?
2. Зачем нужно ключевое слово synchronized ? На что его можно вещать(поле, метод, класс, конструктор..) ?
3. Захват какого монитора происходит при входе в synchronized метод/статик метод/блок ?
4. Зачем нужно ключевое слово volatile ? На что его можно вещать(поле, метод, класс, конструктор..) ?
5. Что делает метод Object#wait, Object#notify, Object#notifyAll
6. Что за исключение IllegalMonitorStateException ?
7. Что делает метод Thread#join ?
8. Что делает метод Thread#interrupt ?
*/

/*
1.  Thread currentThread = Thread.currentThread();
2.  •	Ключевое слово synchronized обеспечивает взаимное исключение (mutual exclusion),
        чтобы код внутри синхронизированного участка выполнялся строго одним потоком в момент времени,
        а также гарантирует видимость изменений (memory visibility).
    •	Его можно использовать:
    •	На нестатический метод (захватывается монитор текущего объекта this).
    •	На статический метод (захватывается монитор объекта Class).
	•	На блок кода вида synchronized (someObject) { ... } (захватывается монитор объекта someObject).
	•	Нельзя применять к полям, конструкторам и классам напрямую.

3.  • 	Нестатический метод, помеченный synchronized: захватывается монитор текущего экземпляра (this).
	•	Статический метод, помеченный synchronized: захватывается монитор класса (объект Class).
	•	Синхронизированный блок: захватывается монитор того объекта, который указан в synchronized(obj) { ... }.

4.  •	volatile гарантирует, что запись в переменную и чтение переменной происходят без кеширования между потоками,
        а также запрещает переупорядочивание операций (reordering) относительно этой переменной.
	•	Применять его можно только к полям класса: private volatile int counter;
    •	Нельзя применять к методам, конструкторам или классам.

5.  •	wait(): текущий поток (который уже владеет монитором объекта) отдаёт монитор и переходит
        в состояние ожидания (WAITING), пока не будет разбужен notify() или notifyAll().
	•	notify(): будит один случайный поток, ожидающий на мониторе данного объекта.
	•	notifyAll(): будит все потоки, ожидающие на мониторе данного объекта.
	•	Все эти методы должны вызываться внутри синхронизированного блока/метода, иначе будет IllegalMonitorStateException.

6.  • 	Бросается, когда вызываются wait(), notify(), notifyAll() или некоторые другие действия с монитором, не владея им.
        Например, если вызывается object.wait() вне synchronized (object) { ... }.

7.  •	Заставляет текущий поток ждать, пока у целевого потока (Thread t) не завершится выполнение.
	•	Например, t.join() в текущем потоке приостановит его, пока поток t не закончится (перейдёт в состояние TERMINATED).

8.  •	Устанавливает флаг прерывания для потока.
	•	Если поток в данный момент спит (sleep), ждёт (wait) или заблокирован на I/O / join(),
	    то он бросит InterruptedException и выйдет из состояния ожидания.
	•	Если поток не заблокирован, то прерывание просто ставит флаг, и дальнейшие действия зависят от проверок
	    в коде (например, через isInterrupted()).

*/

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== FixedThreadPool demo ===");
        ThreadPool fixedPool = new FixedThreadPool(3);
        fixedPool.start();

        for (int i = 1; i <= 10; i++) {
            int jobNumber = i;
            fixedPool.execute(() -> {
                System.out.println(Thread.currentThread().getName()
                        + " выполняет задачу #" + jobNumber);
                try {
                    Thread.sleep(500); // эмуляция работы
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Небольшая задержка
        Thread.sleep(4000);

        System.out.println("\n=== ScalableThreadPool demo ===");
        ThreadPool scalablePool = new ScalableThreadPool(2, 5);
        scalablePool.start();

        for (int i = 1; i <= 10; i++) {
            int jobNumber = i;
            scalablePool.execute(() -> {
                System.out.println(Thread.currentThread().getName()
                        + " выполняет задачу #" + jobNumber);
                try {
                    Thread.sleep(500); // эмуляция работы
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
}