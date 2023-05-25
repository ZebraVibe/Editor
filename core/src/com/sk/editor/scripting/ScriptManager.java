package com.sk.editor.scripting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.sk.editor.config.Config;
import com.sk.editor.ui.logger.EditorLogger;
import org.reflections.Reflections;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class ScriptManager {

    private static final EditorLogger log = new EditorLogger(ScriptManager.class.toString(), Logger.DEBUG);

    private final String ASSETS_DIR = "/assets";
    private final String CORE_DIR = "/core";
    private final String SRC_PATH = CORE_DIR + "/src";
    private final String CLASS_PATH = CORE_DIR + "/" + Config.CLASS_PATH_DIR_NAME;
    private FileHandle projectPath;

    private FileTreeWalker fileTreeWalker;
    private ClassLoadingManager classLoadingManager;
    private CompilationManager compilationManager;
    private FileWatcher fileWatcher;

    private boolean init;

    /**
     * @param projectPath the absolute path of the project directory containing core, assets and desktop dir
     */
    public ScriptManager(FileHandle projectPath){
        // #init called in compile and load
        try {
            setProjectPath(projectPath, true); // tries to compile and load
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -- init --
    private void init() throws Exception {
        log.debug("Initializing " + getClass().getSimpleName());

        // check project path must be set yet
        Path srcPath = getSrcPath().file().toPath();//Paths.get(getSrcPath());
        Path classPath = getClassPath().file().toPath();//Paths.get(getClassPath());
        String packageName = getPackageName();


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
                log.debug("Recompile & load", "button", (Runnable) () -> {
                    try {
                        compileAndLoad();
                    } catch (Exception e) {
                        log.error("Could not Recompile and load.",e);
                    }
                });
            }
        };
        fileWatcher.start();

        // class loading manager
        classLoadingManager = new ClassLoadingManager(classPath, fileTreeWalker) {
            @Override
            public URLClassLoader createURLCLassLoader(URL[] urls) {
                log.debug("Creating ScriptClassLoader.");
                ScriptClassLoader loader = new ScriptClassLoader(classPath, urls, (ClassLoader) ClassLoader.getSystemClassLoader());
                loader.registerAllowedPackages(packageName);

                // set the class loader as current context classloader of the thread
                Thread.currentThread().setContextClassLoader(loader);

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
            init();
            init = true;
        }


        log.debug("projectPath: " + getProjectPath());
        log.debug("srcPath: " + getSrcPath());
        log.debug("classPath: " + getClassPath());
        log.debug("packageName: " + getPackageName());


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
    public <T> Array<Class<? extends T>> getSubTypesOf(Class<T> superClass, Array<Class<? extends T>> array)throws ReflectionException {
        if(array == null)throw new ReflectionException("array can not be null.");
        if(superClass == null)throw new ReflectionException("superClass can not be null.");

        ClassLoader loader = classLoadingManager.getCurrentClassLoader();
        if(loader == null)throw new GdxRuntimeException("Loader can not be null.");

        Reflections reflections = new Reflections(getPackageName(), loader);
        Set<Class <? extends T>> set = reflections.getSubTypesOf(superClass);
        Class<? extends T>[] classes = new Class[0];
        array.addAll(set.toArray(classes));
        return array;
    }


    public FileHandle getProjectPath() {
        return projectPath;
    }
    public void setProjectPath(FileHandle projectPath, boolean compileAndLoad) throws Exception{
        if(projectPath == null)throw new NullPointerException("Project path can't be null");

        Path path = null;
        try {
            path = projectPath.file().toPath();//Paths.get(projectPath);
        } catch (Exception e){
            throw new GdxRuntimeException("Project path invalid: " + projectPath, e );
        }

        if(Files.exists(path) == false && path.toFile().mkdirs() == false)
            throw new GdxRuntimeException("Could not create mkdirs from " + projectPath);

        this.projectPath = projectPath;
        this.init = false; // notify for re-init

        if(compileAndLoad == false)return;
        // try to compile and load
        try {
            compileAndLoad();
        } catch (Exception e) {
            log.error("Could not compile and load on setProjectPath(path).", e);
        }
    }

    public FileHandle getSrcPath() {
        return projectPath.child(SRC_PATH);
    }

    public FileHandle getClassPath() {
        return projectPath.child(CLASS_PATH);// + getPackageName();
    }

    public String getPackageName() {
        String srcPath = getSrcPath().toString();
        FileHandle current = Gdx.files.absolute(srcPath);

        // get first sub dir with more than one child
        while(current.list().length == 1){
            current = current.list()[0];
        }

        // format name from </a/b/c> to <a.b.c>
        Path relative = Paths.get(srcPath).relativize(current.file().toPath());
        String name = relative.toString();
        name = name.replace(File.separator, ".");
        if(name.startsWith("."))name = name.substring(1);
        if(name.endsWith("."))name = name.substring(0, name.length() -1);

        return name;
    }

    public FileHandle getAssetsPath(){
        return projectPath.child(ASSETS_DIR);
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
