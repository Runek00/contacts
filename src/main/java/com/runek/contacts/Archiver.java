package com.runek.contacts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Archiver {

    private static final Archiver INSTANCE = new Archiver();
    private ArchStatus status = ArchStatus.WAITING;
    String fileName;
    Path filePath;
    int maxElements = 100;
    int doneElements = 0;
    private Thread runningThread;

    public static Archiver get() {
        return INSTANCE;
    }

    public void run() {
        fileName = "test1.txt";
        filePath = Path.of("C:/Users/Ja/losowykod/java/contacts/static/" + fileName);
        status = ArchStatus.RUNNING;
        runningThread = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    Files.writeString(filePath, i + "\n", StandardOpenOption.APPEND , StandardOpenOption.CREATE);
                    doneElements++;
                    Thread.sleep(200);
                }
                status = ArchStatus.COMPLETE;
            } catch (IOException | InterruptedException e) {
                status = ArchStatus.WAITING;
                throw new RuntimeException(e);
            }
        });
        runningThread.start();
    }

    public ArchStatus status() {
        return this.status;
    }

    public double progress() {
        return (double) doneElements / (double) maxElements;
    }

    public void reset() {
        runningThread.interrupt();
        try {
            Files.delete(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        status = ArchStatus.WAITING;
        doneElements = 0;
    }

    public Path archiveFilePath() {
        if (status.equals(ArchStatus.COMPLETE)) {
            return filePath;
        }
        return null;
    }


}

enum ArchStatus {
    WAITING,
    RUNNING,
    COMPLETE
}
