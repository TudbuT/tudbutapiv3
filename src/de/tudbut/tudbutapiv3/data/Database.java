package de.tudbut.tudbutapiv3.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import de.tudbut.io.StreamReader;
import de.tudbut.io.StreamWriter;
import de.tudbut.tools.Hasher;
import de.tudbut.tools.Tools;
import de.tudbut.tudbutapiv3.Main;
import tudbut.logger.Logger;
import tudbut.parsing.AsyncJSON;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;
import tudbut.tools.Lock;

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
            logger.warn("[Load] Unable to read file.");
            // BACKUP and create new file.
            if(new File("data.json").exists()) {
                logger.info("[Load] File exists, trying to make a backup...");
                if(!(new File("data.json").renameTo(new File("data" + System.currentTimeMillis() + ".json.backup")))) {
                    logger.error("[Load] Backup could not be saved!!! Crashing to prevent potential corruption. Bye!");
                    // failed to backup! exit ASAP.
                    System.exit(20);
                }
                logger.info("[Load] Backup is saved. Continueing load process by creating a new database.");
            }
            try {
                new File("data.json").createNewFile();
                logger.warn("[SETUP] To stop the setup from creating the file, press CTRL+C to stop the process immediately.");
                logger.warn("[SETUP] If you try to do this later, the file WILL be autosaved.");
                logger.warn("[SETUP] Please enter your desired admin password. It will be hashed.");
                data = new TCN();
                data.set("usersByUUID", new TCN());
                data.set("services", new TCN());
                data.set("nameToUUID", new TCN());
                data.set("password", Hasher.sha512hex(Hasher.sha512hex(Tools.getStdInput().readLine())));
                logger.warn("[SETUP] Cool! Admin password setting set to " + data.getString("password") + ".");
                logger.info("[SETUP] Setup complete. Thank you!");
            } catch (IOException e1) {
                logger.error("[Load] No write access to file system. Stopping.");
                e1.printStackTrace();
                System.exit(21);
            }
        }
        new Thread(() -> {
            Lock lock = new Lock();
            while(true) {
                lock.lock(15000);
                save();
                lock.waitHere();
            }
        }, "DB save").start();
        Runtime.getRuntime().addShutdownHook(new Thread(Database::save));
    }

    public static void save() {
        synchronized(data) {
            String json = JSON.write(data);
            try {
                logger.info("[Save] Saving database...");
                StreamWriter writer = new StreamWriter(new FileOutputStream("data.json.tmp"));
                writer.writeChars(json.toCharArray());
                writer.stream.close();
                logger.info("[Save] Temporary file written.");
                new File("data.json.tmp").renameTo(new File("data.json"));
                logger.info("[Save] Done.");
            } catch (IOException e) {
                logger.error("[Save] No write access to file system. Stopping.");
                e.printStackTrace();
                System.exit(22);
            }
        }
    }

    public static UserRecord getUser(UUID uuid) {
        TCN tcn = data.getSub("usersByUUID").getSub(uuid.toString());
        if(tcn != null) {
            return new UserRecord(uuid, tcn);
        }
        else {
            UserRecord record = new UserRecord(uuid);
            data.getSub("usersByUUID").set(uuid.toString(), record.data);
            return record;
        }
    }

    public static UserRecord getUser(String uuid, String name) {
        if(name != null) {
            uuid = data.getSub("nameToUUID").getString(name);
        }
        return getUser(UUID.fromString(uuid));
    }

    public static boolean serviceExists(String service) {
        return data.getSub("services").getSub(service) != null;
    }

    public static ServiceData service(String service) {
        return new ServiceData(service, data.getSub("services").getSub(service));
    }

    public static ServiceData makeService(String name) {
        ServiceData service = new ServiceData(name);
        data.getSub("services").set(name, service.data);
        logger.warn("Service created: " + name + ". Thank you!");
        return service;
    }
}

