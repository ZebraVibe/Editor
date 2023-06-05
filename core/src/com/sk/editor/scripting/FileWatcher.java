package com.sk.editor.scripting;

import com.badlogic.gdx.utils.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.TimerTask;

public class FileWatcher {

    private static final Logger log = new Logger(FileWatcher.class.toString(), Logger.DEBUG);

    /**
     * this suffix is used by some text editors (including intellij)
     * for creating tmp files of a file in case of a crash
     */
    private final String TMP_FILE_NAME_SUFFIX = "~";
    private final WatchEvent.Kind<?>[] KINDS = new WatchEvent.Kind<?>[]{
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.OVERFLOW};

    private final ArrayMap<Path, WatchKey> watchKeys = new ArrayMap<>();
    private final Array<FileEvent> events = new Array<>();
    private WatchService watchService;
    private java.util.Timer watchServiceTimer;
    //private FileTreeWalker fileWalker;
    private Path startDir;
    private boolean hasTask;

    // -- constructor --

    /**
     * Creates a new thread to watch the given directory.
     * Auto registeres & unregisteres newly created and removed dirs and their trees
     * <p>
     * Call {@link #start()} to start the watchservice & its thread
     *
     * @param dir        to watch
     * @throws Exception
     */
    public FileWatcher(Path dir) throws Exception {
        watchService = FileSystems.getDefault().newWatchService();
        //this.fileWalker = fileWalker;
        this.startDir = dir;
        watchServiceTimer = new java.util.Timer(true);
    }

    // -- init --

    private TimerTask createTask(){
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                FileWatcher.this.run();
                if (!events.isEmpty()) notifyForChanges();
            }
        };
        return task;
    }

    // -- private --

    /**
     * called in timer runnable every interval
     */
    private void run() {
        WatchKey key = watchService.poll();

        // loop through keys
        while (key != null) {
            //WatchKey key = //watchService.take();
            Path dir = (Path) key.watchable(); //keyMap.get(key);

            // iterate over events
            for (WatchEvent event : key.pollEvents()) {
                Path relativeToDirPath = (Path) event.context();
                Path absoluteFilePath = dir.resolve(relativeToDirPath);

                // registered dirs' watchkey is queued when: an entry is created in this dir or renamed into this dir
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    log.debug("file created: " + absoluteFilePath);
                    // if file is a dir register it & all sub dirs as keys
                    if (Files.isDirectory(absoluteFilePath)) {
                        registerToWatchService(absoluteFilePath);
                    }
                    onFileCreated(absoluteFilePath);

                    // registered dirs' watchkey is queued when: an entry in the dir has been modified
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    log.debug("file modified: " + absoluteFilePath);
                    onFileModified(absoluteFilePath);

                    // registered dirs' watchkey is queued when: an entry is deleted or renamed out of the dir
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    log.debug("file deleted: " + absoluteFilePath);
                    // Files.isDirectory(absoluteFilePath); does not work when file doesnt exist anymore
                    unregisterFromWatchService(absoluteFilePath);// if registered
                    onFileDeleted(absoluteFilePath);

                    // event indicating when events may have been lost or discarded
                } else if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    log.error("watch event overflow: events may have been lost or discarded");
                    onOverflow(absoluteFilePath);
                }
            }

            //immediately requeued if pending events are present else remains in ready state if not canceled
            // no effect if key has been canceled already
            key.reset();

            // get new key
            key = watchService.poll();
        }

    }

    /**registers potential sub dirs too**/
    private void registerToWatchService(Path createdDir) {
        log.info("registering dir and sub dirs of: " + createdDir);
        try {
            Files.walkFileTree(createdDir,new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult result =  super.preVisitDirectory(dir, attrs);
                    try {
                        WatchKey key = dir.register(watchService, KINDS);// register events to be notified
                        watchKeys.put(dir, key);
                    } catch (IOException e) {}
                    return result;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        /*
        fileWalker.walkDirs(createdDir, (dir, attrs) -> {
            try {
                WatchKey key = dir.register(watchService, KINDS);// register events to be notified
                watchKeys.put(dir, key);
            } catch (IOException e) {}
        });*/
        printWatchedDirs(); // debug
    }

    /**unregisters potential sub dirs too**/
    private void unregisterFromWatchService(Path deletedDir) {
        log.info("unregistering dir and sub dirs of: " + deletedDir);
        // Files.isDirectory(absoluteFilePath); does not work if file doesnt exist anymore
        Array<Path> array = Pools.obtain(Array.class);
        array.clear();
        WatchKey key = watchKeys.removeKey(deletedDir);
        if(key == null)return; // dir and sub dirs not present
        key.cancel();

        // remove sub dirs
        array.addAll(watchKeys.keys().toArray());
        for(Path subDir : array){
            if(subDir.startsWith(deletedDir)){
                key = watchKeys.removeKey(subDir);
                key.cancel();
            }
        }
        array.clear();
        Pools.free(array);
        printWatchedDirs(); // debug
    }


    private FileEvent queueFileEvent(Path absolutePath, WatchEvent.Kind kind) {
        FileEvent event = Pools.obtain(FileEvent.class);
        event.path = absolutePath;
        event.kind = kind;
        events.add(event);
        return event;
    }

    /**
     * returns if no events are queued;
     */
    private final void notifyForChanges() {
        log.info("processing gathered file events...");
        processEvents(events);
        // free events
        events.forEach(e -> Pools.free(e) );
        events.clear();
    }


    // -- public --


    /**
     * called after all current existing file events have been gathered. only
     * called if processable events exist.
     * @param events all gathered events
     */
    public void processEvents(Array<FileEvent> events) {
        events.forEach(e -> process(e));
    }

    /**
     * processes a single event at a time inside {@link #processEvents(Array)}
     * @param event
     */
    public void process(FileEvent event) {}


    /**
     * @param array to fill the watch keys in
     * @return the given array
     */
    public Array<Path> getCurrentWatchedDirs(Array<Path> array) {
        array.addAll(watchKeys.keys().toArray());
        return array;
    }

    /**
     * @param filePath the absolute file path
     * @return true if the file is a editor created tmp file (has "~" suffix: a.java~ )
     */
    public boolean isTempFile(Path filePath) {
        return filePath.getFileName().endsWith(TMP_FILE_NAME_SUFFIX);
    }

    /**
     * starts watching the given directory. If already watching ignores the call
     */
    public final void start() {
        if(hasTask)return;

        // register startDir & sub dirs in hierarchy as keys
        registerToWatchService(startDir);
        onPostInitialRegistry();

        watchServiceTimer.schedule(createTask(), 0L, 1000L);
        hasTask = true;
    }

    /**
     * terminates watching permanently. can not be undone. To continue watching
     * a new file watcher needs to be created.
     * @return
     */
    public final void stop() {
        watchServiceTimer.cancel();
    }

    /**
     *
     * @return true if the file watcher has already started to watch the given directory
     */
    public boolean isWatching(){
        return hasTask;
    }

    /**
     * @param filePath the absolut path of the file
     */
    public void onFileModified(Path filePath) {
        queueFileEvent(filePath, StandardWatchEventKinds.ENTRY_MODIFY);
    }


    /**
     * @param filePath the absolut path of the file
     */
    public void onFileCreated(Path filePath) {
        queueFileEvent(filePath, StandardWatchEventKinds.ENTRY_CREATE);
    }

    /**
     * @param filePath the absolut path of the file
     */
    public void onFileDeleted(Path filePath) {
        queueFileEvent(filePath, StandardWatchEventKinds.ENTRY_DELETE);
    }

    /**
     * @param filePath the absolut path of the file
     */
    public void onOverflow(Path filePath) {

    }


    /**
     * called after all initial dirs are registered
     */
    public void onPostInitialRegistry() {}

    public void printWatchedDirs() {
        log.debug("--- printing watched dirs: ---");
        watchKeys.forEach(e -> log.debug(e.key.toString()));
        log.debug("--- ---");
    }

    public static class FileEvent implements Pool.Poolable {
        public WatchEvent.Kind kind;
        /**
         * the absolute file path
         */
        public Path path;

        @Override
        public void reset() {
            kind = null;
            path = null;
        }

    }


}
