package com.sk.editor.scripting;

import java.nio.file.Path;

public class FileWatcherAdapter extends FileWatcher{
    /**
     * Creates a new thread to watch the given directory.
     * Auto registeres & unregisteres newly created and removed dirs and their trees
     * <p>
     * Call {@link #start()} to start the watchservice & its thread
     *
     * @param dir        to watch
     * @param fileWalker
     * @throws Exception
     */
    public FileWatcherAdapter(Path dir, FileTreeWalker fileWalker) throws Exception {
        super(dir, fileWalker);
    }

    @Override
    public void onFileModified(Path filePath) {
        super.onFileModified(filePath);
    }

    @Override
    public void onFileCreated(Path filePath) {
        super.onFileCreated(filePath);
    }

    @Override
    public void onFileDeleted(Path filePath) {
        super.onFileDeleted(filePath);
    }

    @Override
    public void onOverflow(Path filePath) {
        super.onOverflow(filePath);
    }
}
