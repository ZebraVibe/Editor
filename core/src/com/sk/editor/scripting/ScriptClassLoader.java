package com.sk.editor.scripting;

import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * Custom class loader for adding logic to register allowed packages using {@link #registerAllowedPackages(String...)}
 */
public class ScriptClassLoader extends URLClassLoader {

    private final Path classPath;
    private static final Array<String> ALLOWED_PACKAGES = new Array<>();

    public ScriptClassLoader(Path classPath){
        this(classPath, new URL[0]);
    }

    public ScriptClassLoader(Path classPath, URL[] urls){
        this(classPath, urls, Thread.currentThread().getContextClassLoader());
    }

    public ScriptClassLoader(Path classPath, URL[] urls, ClassLoader parent){
        super(urls, parent);
        this.classPath = classPath;
    }

    /**
     * Adds the classpath to the list of class paths for the class loader to load
     * @param classFile path of a .class file
     * @throws MalformedURLException
     */
    public void addClassFile(Path classFile) throws MalformedURLException {
        File file = classFile.toFile();
        addURL(file.toURI().toURL());
    }

    // -- private --

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // check if class is allowed to be loaded
        if(hasAllowedPackage(name) == false)
            throw new ClassNotFoundException("Class " + name + " not allowed to be loaded!");
        return super.findClass(name);
    }

    private boolean hasAllowedPackage(String className){
        for(String packageName : ALLOWED_PACKAGES){
            if(className.startsWith(packageName)){
                return true;
            }
        }
        return false;
    }

    // -- static --

    /**
     * All packages starting with the registered package names will be allowed to be laoded
     * Ignores names that are null or already registered.
     * @param packageName
     */
    public void registerAllowedPackages(String ...packageName){
        if(packageName == null)return;
        for(String name : packageName){
            if(name == null || ALLOWED_PACKAGES.contains(name,false))continue;
            else ALLOWED_PACKAGES.add(name);
        }
    }

}
