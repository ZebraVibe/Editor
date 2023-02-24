package com.sk.editor.scripting;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;

public class FileTreeWalker {

    /**
     * each bi consumer path attribute will be given an absolut path
     * @param startDir absolute path
     * @param onPreVisitDir
     */
    public void walkDirs(Path startDir, BiConsumer<Path, BasicFileAttributes> onPreVisitDir){
        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.preVisitDirectory(dir, attrs);
                if(onPreVisitDir != null) onPreVisitDir.accept(dir, attrs);
                return result;
            }
        };

        try {
            Files.walkFileTree(startDir, visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void walkFiles(Path startDir, BiConsumer<Path, BasicFileAttributes> onVisitFile){
        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);
                if(onVisitFile != null) onVisitFile.accept(file, attrs);
                return result;
            }
        };

        try {
            Files.walkFileTree(startDir, visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
