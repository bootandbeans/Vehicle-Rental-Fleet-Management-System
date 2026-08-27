package com.rental;

import com.rental.demo.DemoRunner;
import com.rental.ui.ConsoleMenu;

import java.util.Arrays;
import java.util.Scanner;

/**
 * Entry point of the Vehicle Rental &amp; Fleet Management System.
 *
 * <p>On an interactive terminal the console menu is started. Without a
 * terminal (e.g. {@code mvn exec:java} in a pipeline or CI) — or when the
 * {@code --demo} argument is given — the scripted demo scenario runs
 * instead, which exercises every feature end to end.
 */
public final class Main {

    private Main() {
        // entry point only
    }

    /**
     * Application entry point.
     *
     * @param args optional; {@code --demo} forces the scripted demo
     */
    public static void main(String[] args) {
        RentalApp app = RentalApp.createWithSampleData();
        System.out.println("=== Vehicle Rental & Fleet Management System ===");
        System.out.println("Loaded sample data: " + app.getFleetService().size()
                + " vehicles, " + app.getCustomers().size() + " customers.");

        if (wantsDemo(args) || !isInteractiveTerminal()) {
            new DemoRunner(app).run();
        } else {
            new ConsoleMenu(app, new Scanner(System.in)).run();
        }
    }

    private static boolean wantsDemo(String[] args) {
        return Arrays.asList(args).contains("--demo");
    }

    /**
     * @return {@code true} when standard input is an interactive terminal
     */
    private static boolean isInteractiveTerminal() {
        return System.console() != null;
    }
}
