package com.sk.editor.scripting;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Pools;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CompilationManager {

    private static final Logger log = new Logger(CompilationManager.class.toString(), Logger.DEBUG);
    private final String JAVA_EXTENSION = ".java";
    private final String CLASS_EXTENSION = ".class";
    private FileTreeWalker fileWalker;
    private final Path inputDir, outputDir;

    // --constructor --

    /**
     * @param inputDir the directory to read .java files from (a.k.a sry path).
     *                 Its immediate children must contain packages or .java files
     * @param outputDir the directory to write the .class files to (a.k.a bin bath)
     * @param fileWalker
     */
    public CompilationManager(Path inputDir, Path outputDir, FileTreeWalker fileWalker){
        this.fileWalker = fileWalker;
        this.inputDir = inputDir;
        this.outputDir = outputDir;
    }




    // -- public --


    /**
     *
     * @return the directory to read .java files from
     */
    public Path getInputDir() {
        return inputDir;
    }

    /**
     *
     * @return the directory to write .class files to
     */
    public Path getOutputDir() {
        return outputDir;
    }

    /**
     * compiles all .java files from the input directory into .class files to the output directory
     * @return if the compilation was successfull
     */
    public boolean compileAll() throws Exception{
        return compile(inputDir, outputDir);
    }

    /**
     * Compiles all .java files from the input directory into .class files to the output directory.
     * @param srcPath (input directory )the directory from which to read .java files from
     *                   (immediate children consist of packages and .java files)
     * @param outputDir (class path) the directory to which to write .class files to
     * @return if the compilation was successfull
     */
    private boolean compile(Path srcPath, Path outputDir) throws Exception {
        // create output folder if absent
        File file = outputDir.toFile();
        if(!file.exists())file.mkdirs();

        // set up compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        // set output directory
        //fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(outputDir.toFile())); // adds -d output

        // collect java files
        Array<File> javaFiles = Pools.obtain(Array.class);
        javaFiles.clear();
        collectJavaFiles(srcPath, javaFiles);
        Iterable<? extends JavaFileObject> javaObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles);

        // create options
        Array<String> options = new Array<>();//Arrays.asList(new String[]{"-d", outputDir.toString()});//(new String[]{"-d", binPath, "-classpath", System.getProperty("java.class.path")});//
        options.add("-d"); // -d specifies output directory, compiled in same package strucute
        options.add(outputDir.toString());

        //collect classes to be processed by annotations
        Array<String> classesToBeProcessedByAnnotations = null;

        // get & call compilation task
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                classesToBeProcessedByAnnotations,
                javaObjects);

        boolean success = task.call();

        // debug
        log.debug("--- Compilation diagnostics { ---");
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()){
            log.error("Line" + d.getLineNumber() + ", " + d.getMessage(null) + " in " + d.getSource().getName());
            //System.out.format("Line %d, %s in %s", d.getLineNumber(), d.getMessage(null), d.getSource().getName());
        }
        log.debug("--- } ---");

        // clean up
        cleanUpOutdatedFiles(srcPath, outputDir);

        javaFiles.clear();
        Pools.free(javaFiles);
        return success;
    }

    /**
     * Collects all .java files from the given directory and its sub-directories
     * @param inputDir the dir from which to read .java files from
     * @param array the array to add the files into
     */
    private Array<File> collectJavaFiles(Path inputDir, Array<File> array){
        if (Files.isDirectory(inputDir)){
            fileWalker.walkFiles(inputDir, (filePath, attrs) -> {
                if(filePath.toString().endsWith(JAVA_EXTENSION)) {
                    array.add(filePath.toFile());
                }
            });
        }
        return array;
    }

    private void cleanUpOutdatedFiles(Path srcPath, Path outputDir) throws IOException {
        log.debug("Cleaning up outdated files...");
        // clean .class files
        Files.walk(outputDir)
                .filter(path -> {
                    return path.toString().endsWith(CLASS_EXTENSION);
                })
                .filter(path -> {
                    Path relativePath = outputDir.relativize(path);
                    String className = relativePath.toString().replace(CLASS_EXTENSION, "");
                    Path srcFile = srcPath.resolve(className.replace(".", "/") + JAVA_EXTENSION);
                    return Files.exists(srcFile) == false; // only delete if file does not exist in src anymore
                }).forEach(path -> {
                    try {
                        Files.delete(path);
                        log.debug("Deleting outdated file: " + path);
                    } catch (IOException e) {
                        log.error("Could not delete outdated file: " + path);
                    }
                });
        // clean directories
        Files.walk(outputDir)
                 .filter(path -> {
                         return Files.isDirectory(path);
                 })
                 .filter(path -> {
                     Path relativePath = outputDir.relativize(path);
                     Path srcFile = srcPath.resolve(relativePath);
                     return Files.exists(srcFile) == false; // only delete if file does not exist in src anymore
                 }).forEach(path -> {
                    try {
                        Files.delete(path);
                        log.debug("Deleting outdated dir: " + path);

                    } catch (IOException e) {
                        log.error("Could not delete dir: " + path);
                    }
                });

        log.debug("..done cleaning up.");
    }
}
