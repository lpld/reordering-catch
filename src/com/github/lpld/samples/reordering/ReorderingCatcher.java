package com.github.lpld.samples.reordering;

import java.util.concurrent.Semaphore;

/**
 * This class is running two parallel threads that do the following:
 * <p/>
 * Thread1
 * 1. X = 1
 * 2. r1 = Y
 * <p/>
 * Thread2
 * 1. Y = 1
 * 2. r2 = X
 * <p/>
 * In sequential consistent memory model execution of these two threads would
 * lead to result where either r1 or r2 (or both) is equal to 1. If after the
 * execution both r1 and r2 equal to 0 then a reordering is caught. Next thing
 * to do is to understand whether this is a reordering done by JIT or by the CPU
 * itself. Theoretically this type of reordering should be able to catch on
 * multiprocessor environment with x86 architecture (or architecture with weaker
 * memory model).
 * <p/>
 *
 * @author leopold
 * @since 4/27/14
 */
public class ReorderingCatcher {

    private static int x = 0;
    private static int y = 0;

    private static int r1 = -1;
    private static int r2 = -1;

    private static Semaphore start = new Semaphore(2);
    private static Semaphore end = new Semaphore(2);

    private static long t1start;
    private static long t1end;

    private static long t2start;
    private static long t2end;

    private static Runnable run1 = new Runnable() {
        @Override
        public void run() {
            try {
                start.acquire();
                t1start = System.nanoTime();

                x = 1;
                Thread.sleep(1);
                r1 = y;

                t1end = System.nanoTime();
                end.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    };

    private static Runnable run2 = new Runnable() {
        @Override
        public void run() {
            try {
                start.acquire();
                t2start = System.nanoTime();

                y = 1;
                Thread.sleep(1);
                r2 = x;

                t2end = System.nanoTime();
                end.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    };

    public static void main(String[] args) throws Exception {

        int totalOverlap = 0;
        int totalReorderings = 0;

        for (int i = 0; i < 100000; i++) {
            // initial values
            x = 0;
            y = 0;
            r1 = -1;
            r2 = -1;

            Thread t1 = new Thread(run1);
            Thread t2 = new Thread(run2);

            start.acquire(2);
            end.acquire(2);

            // starting the threads
            t1.start();
            t2.start();

            start.release(2);

            // waiting for the threads to end
            end.acquire(2);

            // if the threads do overlap with each other
            boolean overlap = t1end > t2start && t2end > t1start;
            if (overlap) totalOverlap++;

            if (i % 1000 == 0) {
                System.out.println((i + 1) + ": overlaps: " + totalOverlap);
            }

            // checking for reordering
            if (r1 == 0 && r2 == 0) {
                totalReorderings++;
                System.out.println("Reordering caught on iteration " + i);
            }

            start.release(2);
            end.release(2);
        }

        System.out.print("Total overlap :" + totalOverlap + ", reorderings: " + totalReorderings);
    }
}
