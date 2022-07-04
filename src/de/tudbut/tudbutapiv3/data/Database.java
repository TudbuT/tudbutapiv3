package de.tudbut.tudbutapiv3.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.tudbut.io.StreamReader;
import tudbut.parsing.AsyncJSON;
import tudbut.parsing.TCN;

public class Database {
    public static TCN data;
    public static boolean initialized = false;

    public static void initiailize() {
        if(initialized) {
            System.err.println("Database#initialize called, but is already initialized. This might be a bug - printing StackTrace");
            new Throwable("Database#initialize called with initialized == true").printStackTrace();
            return;
        }
        initialized = true;
        try {
            StreamReader reader = new StreamReader(new FileInputStream("data.json"));
            String json = reader.readAllAsString();
            AsyncJSON.read(json).then(x -> Database.data = x).ok().await();
            reader.inputStream.close();
        } catch (Exception e) {
            // BACKUP and create new file.
            if(new File("data.json").exists()) {
                if(!(new File("data.json").renameTo(new File("data" + System.currentTimeMillis() + ".json.backup")))) {
                    // failed to backup! exit ASAP.
                    System.exit(20);
                }
            }
            try {
                new File("data.json").createNewFile();
                data = new TCN("AJSON");
            } catch (IOException e1) {
                System.err.println("No write access to file system. Stopping.");
                System.exit(21);
            }
        }
    }
}

