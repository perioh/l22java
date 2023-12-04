import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        int bufSize = 4;
        int minTime = 2;
        int maxTime = 5;
        int numProcesses = 200;

        AtomicBoolean ended = new AtomicBoolean(false);

        CPU cpu = new CPU();

        Thread processThread = new Thread(() -> {
            for (int i = 0; i < numProcesses; i++) {
                sleepRandomDuration(minTime, maxTime);
                CPUProcess process = new CPUProcess(getRandomNumber(minTime, maxTime));
                System.out.println("Inserted new process");
                cpu.insertNew(process, bufSize);

            }
            ended.set(true);
            System.out.println("ended");

        });

        Thread serviceThread = new Thread(() -> {
            boolean sender_dead = false;

            while (true) {

                CPUQueue q;
                    q = cpu.getFirst();
                    if (q == null) {
                        if (sender_dead) {
                            break;
                        }
                        continue;
                    }


                while (true) {
                    CPUProcess process = q.extract();
                    if (process == null) {
                        break;
                    }
                    sleepMillis(process.getInterval());
                    System.out.println("Slept " + process.getInterval()+"ms");
                }

                if (ended.get()) {
                    sender_dead = true;
                }
            }

                System.out.println("avg buffer: " + (double) cpu.getTotalQueueElements() / numProcesses +
                        ", max buffer: " + cpu.getMaxQueueElements());
        });

        processThread.start();
        serviceThread.start();

        processThread.join();
        serviceThread.join();
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int getRandomNumber(int min, int max) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

    private static void sleepRandomDuration(int min, int max) {
        sleepMillis(getRandomNumber(min, max));
    }
}

class CPUQueue {
    private ArrayDeque<CPUProcess> processes;

    CPUQueue() {
        processes = new ArrayDeque<>();
    }


    void insert(CPUProcess process) {
        processes.add(process);
    }

    CPUProcess extract() {
        return processes.poll();
    }
    int len() {
        return processes.size();
    }
}

class CPUProcess {
    private long interval;

    CPUProcess(long interval) {
        this.interval = interval;
    }

    long getInterval() {
        return interval;
    }
}

class CPU {
    private ArrayDeque<CPUQueue> queue;
    private int totalQueueElements;
    private int maxQueueElements;
    CPU() {
        queue = new ArrayDeque<>();
        totalQueueElements = 0;
        maxQueueElements = 0;

    }


    void insertNew(CPUProcess process, int bufferSize) {
        boolean createNew = false;
            if (!queue.isEmpty()) {
                CPUQueue firstQueue = queue.peek();
                if (firstQueue != null && firstQueue.len() < bufferSize) {
                    firstQueue.insert(process);
                } else {
                    createNew = true;
                }
            } else {
                createNew = true;
            }

            if (createNew) {
                CPUQueue newQueue = new CPUQueue();
                newQueue.insert(process);
                queue.add(newQueue);
                System.out.println("Buffer increased to "+queue.size());
            }
            totalQueueElements += queue.size();
            if (queue.size() > maxQueueElements) {
                maxQueueElements = queue.size();
            }

    }

    CPUQueue getFirst() {
        if (queue.size()>0) {
            System.out.println("Buffer decreased to "+(queue.size()-1));
        }
        return queue.poll();

    }

    int getTotalQueueElements() {
        return totalQueueElements;
    }

    int getMaxQueueElements() {
        return maxQueueElements;
    }
}
