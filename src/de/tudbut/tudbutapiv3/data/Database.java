package de.tudbut.tudbutapiv3.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import de.tudbut.io.StreamReader;
import de.tudbut.io.StreamWriter;
import de.tudbut.tudbutapiv3.Main;
import tudbut.logger.Logger;
import tudbut.parsing.AsyncJSON;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;

public class Database {
    public static Logger logger = Main.logger.subChannel("Database");
    public static TCN data;
    public static boolean initialized = false;

    public static void initiailize() {
        if(initialized) {
            logger.warn("Database#initialize called, but is already initialized. This might be a bug - printing StackTrace");
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
                data = new TCN();
            } catch (IOException e1) {
                logger.error("[Load] No write access to file system. Stopping.");
                e1.printStackTrace();
                System.exit(21);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(Database::save));
    }

    public static void save() {
        synchronized(data) {
            String json = JSON.write(data);
            try {
                StreamWriter writer = new StreamWriter(new FileOutputStream("data.json.tmp"));
                writer.writeChars(json.toCharArray());
                writer.stream.close();
                new File("data.json.tmp").renameTo(new File("data.json"));
            } catch (IOException e) {
                logger.error("[Save] No write access to file system. Stopping.");
                e.printStackTrace();
                System.exit(22);
            }
        }
    }

    public static UserRecord getUser(UUID fromString) {
        return null;
    }
}

