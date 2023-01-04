package de.tudbut.tudbutapiv3.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import de.tudbut.io.StreamReader;
import de.tudbut.io.StreamWriter;
import de.tudbut.tools.Hasher;
import de.tudbut.tools.Tools;
import de.tudbut.tudbutapiv3.Main;
import tudbut.logger.Logger;
import tudbut.parsing.AsyncJSON;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;
import tudbut.parsing.TCNArray;
import tudbut.tools.Lock;
import tudbut.tools.encryption.RawKey;

public class Database {
    public static Logger logger = Main.logger.subChannel("Database");
    public static TCN data;
    public static boolean initialized = false;
    public static RawKey key;
    public static int currentVersion = 8;
    private static Lock lock = new Lock();

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
                data.set("users", new TCN());
                data.set("services", new TCN());
                data.set("nameToUUID", new TCN());
                data.set("password", Hasher.sha512hex(Hasher.sha512hex(Tools.getStdInput().readLine())));
                data.set("key", new RawKey().toString());
                data.set("version", currentVersion);
                logger.warn("[SETUP] Cool! Admin password setting set to " + data.getString("password") + ".");
                logger.info("[SETUP] Setup complete. Thank you!");
            } catch (IOException e1) {
                logger.error("[Load] No write access to file system. Stopping.");
                e1.printStackTrace();
                System.exit(21);
            }
        }
        key = new RawKey(data.getString("key"));
        if(currentVersion != data.getInteger("version")) {
            migrate(data.getInteger("version"));
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

    private static void migrate(int oldVersion) {
        logger.warn("[Load] Data migration needed. Version changed from " + oldVersion + " to " + currentVersion + ".");
        int allModifications = 0;
        if(oldVersion == 1) {
            TCN tcn = data.getSub("usersByUUID");
            AtomicInteger modifications = new AtomicInteger(0);
            tcn.map.entries().forEach(entry -> {
                TCN user = (TCN) entry.val;
                user.set("passwordHash", "");
                modifications.getAndIncrement();
                TCN services = user.getSub("services");
                services.map.keys().forEach(key -> {
                    services.getSub(key).set("premiumStatus", 0);
                    modifications.getAndIncrement();
                });
            });
            allModifications += modifications.get();
            logger.info("[Load] Data migration from " + oldVersion + " to " + (oldVersion + 1) + " successful. " + modifications.get() + " modifications were made.");
            oldVersion++;
        }
        if(oldVersion == 2) {
            TCN tcn = data.getSub("usersByUUID");
            AtomicInteger modifications = new AtomicInteger(0);
            tcn.map.entries().forEach(entry -> {
                TCN user = (TCN) entry.val;
                TCN services = user.getSub("services");
                services.map.keys().forEach(key -> {
                    services.getSub(key).set("version", "v0.0.0a");
                    modifications.getAndIncrement();
                });
            });
            data.set("usersByUUID", null);
            modifications.getAndIncrement();
            data.set("users", tcn);
            modifications.getAndIncrement();
            allModifications += modifications.get();
            logger.info("[Load] Data migration from " + oldVersion + " to " + (oldVersion + 1) + " successful. " + modifications.get() + " modifications were made.");
            oldVersion++;
        }
        if(oldVersion == 3) {
            TCN tcn = data.getSub("users");
            AtomicInteger modifications = new AtomicInteger(0);
            tcn.map.entries().forEach(entry -> {
                TCN user = (TCN) entry.val;
                TCN services = user.getSub("services");
                services.map.keys().forEach(key -> {
                    services.getSub(key).set("data", new TCN());
                    modifications.getAndIncrement();
                    services.getSub(key).set("dataMessages", new TCNArray());
                    modifications.getAndIncrement();
                });
            });
            allModifications += modifications.get();
            logger.info("[Load] Data migration from " + oldVersion + " to " + (oldVersion + 1) + " successful. " + modifications.get() + " modifications were made.");
            oldVersion++;
        }
        if(oldVersion == 4) {
            TCN tcn = data.getSub("services");
            AtomicInteger modifications = new AtomicInteger(0);
            tcn.map.entries().forEach(entry -> {
                TCN service = (TCN) entry.val;
                service.set("allowChat", false);
                modifications.getAndIncrement();
            });
            allModifications += modifications.get();
            logger.info("[Load] Data migration from " + oldVersion + " to " + (oldVersion + 1) + " successful. " + modifications.get() + " modifications were made.");
            oldVersion++;
        }
        if(oldVersion == 5) {
            TCN tcn = data.getSub("users");
            AtomicInteger modifications = new AtomicInteger(0);
            tcn.map.entries().forEach(entry -> {
                TCN user = (TCN) entry.val;
                TCN services = user.getSub("services");
                services.map.keys().forEach(key -> {
                    services.getSub(key).set("lastMessageSent", System.currentTimeMillis());
                    modifications.getAndIncrement();
                });
            });
            allModifications += modifications.get();
            logger.info("[Load] Data migration from " + oldVersion + " to " + (oldVersion + 1) + " successful. " + modifications.get() + " modifications were made.");
            oldVersion++;
        }
        if(oldVersion == 6) {
            TCN tcn = data.getSub("services");
            AtomicInteger modifications = new AtomicInteger(0);
            tcn.map.entries().forEach(entry -> {
                TCN service = (TCN) entry.val;
                service.set("data", new TCN());
                modifications.getAndIncrement();
            });
            allModifications += modifications.get();
            logger.info("[Load] Data migration from " + oldVersion + " to " + (oldVersion + 1) + " successful. " + modifications.get() + " modifications were made.");
            oldVersion++;
        }
        if(oldVersion == 7) {
            TCN tcn = data.getSub("users");
            AtomicInteger modifications = new AtomicInteger(0);
            tcn.map.entries().forEach(entry -> {
                TCN user = (TCN) entry.val;
                if(user.getString("name").startsWith("FETCH_ERROR_")) {
                    user.set("lastNameFetch", 0);
                    modifications.getAndIncrement();
                }
            });
            allModifications += modifications.get();
            logger.info("[Load] Data migration from " + oldVersion + " to " + (oldVersion + 1) + " successful. " + modifications.get() + " modifications were made.");
            oldVersion++;
        }
        if(oldVersion == currentVersion) {
            logger.info("[Load] Data migration was successful. " + allModifications + " modifications were made. Thank you for your patience.");
        }
        data.set("version", currentVersion);
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
        lock.waitHere();
        return getUser(uuid, true);
    }

    public static UserRecord getUser(UUID uuid, boolean create) {
        lock.waitHere();
        TCN tcn = data.getSub("users").getSub(uuid.toString());
        if(tcn != null) {
            return new UserRecord(uuid, tcn);
        }
        else if(create) {
            UserRecord record = new UserRecord(uuid);
            data.getSub("users").set(uuid.toString(), record.data);
            return record;
        }
        return null;
    }

    public static UserRecord getUser(String uuid, String name) {
        lock.waitHere();
        return getUser(uuid, name, true);
    }

    public static UserRecord getUser(String uuid, String name, boolean create) {
        lock.waitHere();
        if(name != null)
            uuid = data.getSub("nameToUUID").getString(name);
        if(uuid != null)
            return getUser(UUID.fromString(uuid));
        return null;
    }

    public static boolean serviceExists(String service) {
        lock.waitHere();
        return data.getSub("services").getSub(service) != null;
    }

    public static ServiceData service(String service) {
        lock.waitHere();
        return new ServiceData(service, data.getSub("services").getSub(service));
    }

    public static ServiceData makeService(String name, String servicePassword) {
        lock.waitHere();
        ServiceData service = new ServiceData(name, servicePassword);
        data.getSub("services").set(name, service.data);
        logger.warn("Service created: " + name + ". Thank you!");
        return service;
    }

    public static void migrateServiceDelete(String name) {
        lock.waitHere();
        lock.lock();
        data.getSub("services").set(name, null);
        logger.warn("[Load] Data migration needed. Need to delete service data.");
        TCN tcn = data.getSub("users");
        AtomicInteger modifications = new AtomicInteger(0);
        tcn.map.entries().forEach(entry -> {
            TCN user = (TCN) entry.val;
            TCN services = user.getSub("services");
            if(services.get(name) != null) {
                services.set(name, null);
                modifications.getAndIncrement();
            }
        });
        logger.info("[Load] Data migration was successful. " + modifications + " modifications were made. Thank you for your patience.");
        lock.unlock();
    }

    public static void migrateServiceRename(String name, String newName) {
        lock.waitHere();
        lock.lock();
        data.getSub("services").set(newName, data.getSub("services").get(name));
        data.getSub("services").set(name, null);
        logger.warn("[Load] Data migration needed. Need to move service data.");
        TCN tcn = data.getSub("users");
        AtomicInteger modifications = new AtomicInteger(0);
        tcn.map.entries().forEach(entry -> {
            TCN user = (TCN) entry.val;
            TCN services = user.getSub("services");
            if(services.get(name) != null) {
                services.getSub(name).set("service", newName);
                services.set(newName, services.get(name));
                services.set(name, null);
                modifications.getAndIncrement();
            }
        });
        logger.info("[Load] Data migration was successful. " + modifications + " modifications were made. Thank you for your patience.");
        lock.unlock();
    }
}

