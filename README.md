# Productivity-Buddy⚙️📊

A high-performance, multi-threaded desktop application built in Java and JavaFX designed to monitor, analyze, and manage operating system processes in real-time. 

This tool goes beyond a standard task manager by allowing users to categorize active applications (Work, Fun, Other), track time spent, and export detailed system analytics for productivity tracking.

## Key Features

* **Real-Time Hardware Monitoring:** Utilizes the **OSHI** (Operating System & Hardware Information) library to fetch precise CPU and RAM usage directly from the kernel layer.
* **High-Performance Concurrent Scanning:** Implements the *Divide and Conquer* algorithm via a `ForkJoinPool` to rapidly scan hundreds of OS processes without blocking the main application thread.
* **Productivity Analytics & Visualization:** Dynamically groups active processes by category and visualizes resource distribution and total uptime using JavaFX interactive PieCharts.
* **Thread-Safe Data Persistence:** Employs `ReentrantReadWriteLock` and `SingleThreadExecutor` to safely persist custom user configurations (JSON) and log scheduled system snapshots (CSV) without race conditions.
* **Process Control:** Empowers users to rename processes (aliases), freeze time tracking, change categories, or safely terminate (kill) active processes directly from the GUI.
* **Dynamic Configuration:** Reads system parameters and scheduled snapshot times dynamically from a `.properties` file using the Singleton design pattern.

## Tech Stack & Libraries

* **Language:** Java
* **GUI Framework:** JavaFX
* **System Metrics:** OSHI (oshi-core)
* **Data Serialization:** Google Gson
* **Boilerplate Reduction:** Lombok

## Architecture & Concurrency Highlights

This project was heavily focused on safe and efficient multithreading. The central `ProcessRepository` acts as the orchestrator, managing a complex thread lifecycle:
* **ForkJoinPool:** Used for heavy, recursive OS process scanning.
* **ScheduledExecutorService:** Manages periodic GUI updates and schedules future data snapshots without `Thread.sleep()` blocking.
* **SingleThreadExecutor:** Acts as a queueing system to ensure safe, sequential writing to `JSON` and `CSV` files, preventing file corruption and deadlocks.
* **JavaFX Application Thread:** Safely updated from background threads using `Platform.runLater()`.

## How to Run

1. Ensure you have **Java 17** (or higher) installed.
2. Clone the repository.
3. Make sure to include the required dependencies (`oshi-core`, `gson`, `lombok`, `javafx`) via your build tool (Maven/Gradle) or IDE project structure.
4. Run the `GuiApplication` class.
