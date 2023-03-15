package com.sk.editor.scripting;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.sk.editor.ui.logger.EditorLogger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

public class ScriptManager {

    private static final EditorLogger log = new EditorLogger(ScriptManager.class.toString(), Logger.DEBUG);

    private Path srcPath, classPath;
    private String packageName;

    private FileTreeWalker fileTreeWalker;
    private ClassLoadingManager classLoadingManager;
    private CompilationManager compilationManager;
    private FileWatcher fileWatcher;

    private boolean init;

    /**
     * Input directory, output directory and package name must be set manually
     */
    public ScriptManager(){}

    /**
     * @param inputDir    the source directory containing packages and .java files
     * @param outputDir   the directory to which .class files are compiled to and loaded from
     * @param packageName th root package name (i.e. com.my.game) of the project
     */
    public ScriptManager(Path inputDir, Path outputDir, String packageName){
        this.srcPath = inputDir;
        this.classPath = outputDir;
        this.packageName = packageName;
        // #init called in compile and load
    }

    // -- init --
    private void init() throws Exception {
        //file tree walker
        fileTreeWalker = new FileTreeWalker();

        // compilation manager
        compilationManager = new CompilationManager(srcPath, classPath, fileTreeWalker);

        // file watcher
        if(fileWatcher != null)fileWatcher.stop();
        fileWatcher = new FileWatcherAdapter(srcPath, fileTreeWalker) {
            @Override
            public void processEvents(Array<FileEvent> events) {
                super.processEvents(events);
                log.info("Changes were made in source path. Recompilation and loading advised");
            }
        };
        fileWatcher.start();

        // class loading manager
        classLoadingManager = new ClassLoadingManager(classPath, fileTreeWalker) {
            @Override
            public URLClassLoader createURLCLassLoader(URL[] urls) {
                ScriptClassLoader loader = new ScriptClassLoader(classPath, urls, (ClassLoader) ClassLoader.getSystemClassLoader());
                loader.registerAllowedPackages(packageName);
                return loader;
            }
        };
    }

    // -- compile & load --

    /**
     * @return true if the compilation and loading was successful
     */
    public boolean compileAndLoad() throws Exception {
        if(!init){
            // src path does not exist
            if (srcPath == null || !Files.isDirectory(srcPath)){
                log.error("Input dir invalid!");
                return false;
            }
            log.debug("Input dir valid.");

            // get or create output dir
            if (classPath != null && !Files.exists(classPath)) {
                classPath.toFile().mkdirs();
                log.debug("Output file created: " + classPath);
            }
            // class path still does not exist
            if (!Files.isDirectory(classPath)){
                log.error("Output dir invalid!");
                return false;
            }
            log.debug("Output dir valid.");

            if(packageName == null){
                log.error("Package name is missing!");
                return false;
            }


            init();
            init = true;
        }




        log.debug("srcPath: " + srcPath.toString());
        log.debug("classPath" + classPath.toString());


        // compiling
        log.debug("Trying to compile classes from src path...");
        boolean success = false;
        try {
            success = compilationManager.compileAll();
        } catch (Exception e) {
            log.error("...Compilation Failed!" , e);
            return false;
        }

        if (!success) {
            log.error("...Compilation Failed!");
            return false;
        }
        log.debug("...Compilation Successful!");

        //loading
        log.debug("Trying to load compiled classes...");
        try {
            classLoadingManager.loadAllClasses();
        } catch (Exception e) {
            log.error("...Loading Failed." + " " + e.toString());
            return false;
        }
        log.debug("...Loading Successful!");



        return true;
    }

    /**
     * Does nothing if no classes are currently loaded. Make sure {@link #compileAndLoad()}
     * has been at least called once. Since the classes provided with this call are subject to change
     * whenever a recompilation and loading occurs it is not advised to store the array or classes.
     * @param classes the array to fill the loaded classes to
     * @return the given array tihe the current loaded classes added
     */
    public Array<Class<?>> getCurrentLoadedClasses(Array<Class<?>> classes){
        return classLoadingManager.getCurrentLoadedClasses(classes);
    }

    /**
     * @return the current loaded classes
     */
    public Class<?>[] getCurrentLoadedClasses(){
        return classLoadingManager.getCurrentLoadedClasses();
    }


    /**
     * @see @{@link ClassLoadingManager#forName(String)}
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> forName(String className) throws ClassNotFoundException {
        return classLoadingManager.forName(className);
    }

    /**
     *
     * @param superClass
     * @param array the array to fill the found class into
     * @return the array
     * @param <T>
     * @throws ReflectionException
     */
    public <T> Array<Class<?>> getSubTypesOf(Class<T> superClass, Array<Class<?>> array) throws ReflectionException {
        if(array == null)throw new ReflectionException("array can not be null.");
        if(superClass == null)throw new ReflectionException("superClass can not be null.");

        ClassLoader loader = classLoadingManager.getCurrentClassLoader();
        if(loader == null)throw new ReflectionException("Loader can not be null.");

        Reflections reflections = new Reflections(packageName, loader);
        Set<Class <? extends T>> set = reflections.getSubTypesOf(superClass);
        array.addAll(set.toArray(new Class<?>[0]));
        return array;
    }
    

    /**
     * Changes are being read in the next {@link #compileAndLoad()} call.
     * @return true if the given path differs from the current.
     */
    public boolean setSrcPath(Path srcPath) {
        if(this.srcPath != null && this.srcPath.equals(srcPath))return false;
        this.srcPath = srcPath;
        init = false;
        return true;
    }

    public Path getSrcPath() {
        return srcPath;
    }

    /**
     * Changes are being read in the next {@link #compileAndLoad()} call.
     * @return true if the given path differs from the current.
     */
    public boolean setClassPath(Path classPath) {
        if(this.classPath != null && this.classPath.equals(classPath))return false;
        this.classPath = classPath;
        init = false;
        return true;
    }

    public Path getClassPath() {
        return classPath;
    }

    /**
     * Changes are being read in the next {@link #compileAndLoad()} call.
     * @return true if the given name differs from the current.
     */
    public boolean setPackageName(String packageName) {
        if(this.packageName != null && this.packageName.equals(packageName))return false;
        this.packageName = packageName;
        init = false;
        return true;
    }

    public String getPackageName() {
        return packageName;
    }


    // -- debug --

    public void debugLoadedClasses(){
        log.debug("Listing loaded classes:");
        Class<?>[] classes = getCurrentLoadedClasses();
        if(classes == null || classes.length == 0){
            log.debug("List of loaded classes is empty.");
            return;
        }

        for (Class<?> clazz : classes) {
            log.debug(clazz.getName());
        }
    }



}
