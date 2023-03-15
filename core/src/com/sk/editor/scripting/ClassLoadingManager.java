package com.sk.editor.scripting;

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import org.reflections.Reflections;

import javax.tools.*;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClassLoadingManager {

    private static final Logger log = new Logger(ClassLoadingManager.class.toString(), Logger.DEBUG);

    private final String CLASS_EXTENSION = ".class";
    private Path classPath;
    private FileTreeWalker fileWalker;
    private URLClassLoader classLoader;
    private SnapshotArray<Class<?>> loadedClasses = new SnapshotArray<>();


    /**
     * @param classPath the directory to load the .class files from (bin dir)
     * @param fileWalker
     */
    public ClassLoadingManager(Path classPath, FileTreeWalker fileWalker){
        this.fileWalker = fileWalker;
        this.classPath = classPath;
    }


    public void loadAllClasses() throws Exception{
        loadClassesFromDir(this.classPath);
    }

    /**
     * subject to change. not advised to store the classes.
     * @param array the array to fill with the current loaded classes
     * @return the array
     */
    public Array<Class<?>> getCurrentLoadedClasses(Array<Class<?>> array){
        if(array != null){
            Object[] items = loadedClasses.begin();
            for(int i = 0, n = loadedClasses.size;i < n; i++){
                array.add((Class<?>)items[i]);
            }
            loadedClasses.end();
        }
        return array;
    }

    /**
     * subject to change. not advised to store the classes.
     * @return the current loaded classes
     */
    public Class<?>[] getCurrentLoadedClasses(){
        return loadedClasses.toArray(Class.class);
    }

    /**
     * Returns a class with the provided name if present that is
     * loaded by this class loader
     * @param className provided as packageName.className
     * @return the loaded class or null
     */
    public Class<?> forName(String className) throws ClassNotFoundException {
        if(className == null)return null;
        // probably does not work
        //return ClassReflection.forName(className); // since this class is not loaded with the script manager loader
        return Class.forName(className, true, this.classLoader);
        /*
        for(Class<?> cls : loadedClasses){
            if(cls.getName().equals(className))return cls;
        }
        return null;*/
    }


    public URLClassLoader createURLCLassLoader(URL[] urls){
        return new URLClassLoader(null, urls, (ClassLoader)ClassLoader.getSystemClassLoader());
    }

    /**
     *
     * @return Maybe null. Since the class loader is newly created on each recompilation its not advised
     * to store the loader.
     */
    public @Null URLClassLoader getCurrentClassLoader(){
        return classLoader;
    }

    // -- private --

    /**
     * collects all .class files from the given directory and its sub-directories
     * @param classPath
     * @return
     */
    private void loadClassesFromDir(Path classPath) throws Exception{
        /*
        //ArrayList<File> classFiles = getAllClassFileFrom(binPath);
        ArrayList<URL> urlList = new ArrayList<>();

        // only binPath URL suffices if every class is loaded via <packageName>.<className>
        File f = binPath.toFile();
        try {
            urlList.add(f.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        URL[] urls = urlList.toArray(new URL[0]); // URL[] urls = new URL[]{binPath.toUri().toURL()};
        printURLS(urls); //debug
        cl = new URLClassLoader("classpath", urls, (ClassLoader)ClassLoader.getSystemClassLoader());
        */


        Array<File> classFiles = Pools.obtain(Array.class);
        classFiles.clear();
        collectClassFiles(classPath, classFiles);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null ,null);
        Iterable<? extends JavaFileObject> javaObjects = fileManager.getJavaFileObjectsFromFiles(classFiles);

        // only binPath URL suffices if every class is loaded via <packageName>.<className>
        URL[] urls = new URL[]{classPath.toUri().toURL()};
        printURLS(urls); //debug

        // creating a new classloader
        if(this.classLoader != null)this.classLoader.close();
        this.classLoader = createURLCLassLoader(urls);

        // loading classes
        try {
            loadClasses(classFiles, classLoader, classPath);
        } catch (Exception e) {
            throw new GdxRuntimeException("Failed to invoke loading classes method! " + e.toString());
        }

        classFiles.clear();
        Pools.free(classFiles);


        // test running a class
        /*
        try {
            // loads compiled .class file
            // IMPORTANT: the to be loaded class name must also contain its package name (<packagename>.<classname>)
            // i.e.: MyClass from package com.my.class -> com.my.class.MyClass
            final Class c = cl.loadClass("testpackage.stuff.JClass3"); //
            Method m = c.getDeclaredMethod("print");
            Constructor constructor = c.getConstructor();
            m.invoke(constructor.newInstance());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        */
    }

    private void loadClasses(Array<File> classFiles, URLClassLoader cl, Path classPath) {
        String classesDir = classPath.toString();
        loadedClasses.clear();
        for(File file : classFiles){
            Path filePath = file.toPath();
            if(!file.getName().endsWith(CLASS_EXTENSION))continue;
            if(!Files.isRegularFile(filePath))continue;
            String fullClassName = file.getAbsolutePath().replace(classesDir, "");

            // remove ".class" extension
            int index = fullClassName.lastIndexOf(".");
            fullClassName = fullClassName.substring(0,index);
            fullClassName = fullClassName.replace(File.separator, ".");
            if(fullClassName.startsWith("."))fullClassName = fullClassName.replaceFirst(".","");


            try {
                loadedClasses.add(cl.loadClass(fullClassName));
                log.debug("Loading class: " + fullClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not load class: classesDir: " + classesDir +  ", className: " + fullClassName,e);
            }



        }
    }

    /**
     * collects all .class files from the given directory and its sub-directories
     * @param dir
     * @param array the array to add the files to
     * @return
     */
    private Array<File> collectClassFiles(Path dir, Array<File> array){
        fileWalker.walkFiles(dir, (filePath, attrs) -> {
            if(filePath.toString().endsWith(CLASS_EXTENSION)) {
                array.add(filePath.toFile());
            }
        });
        return array;
    }

    // -- debug --

    private void printURLS(URL[] urls){
        log.debug("--- printing urls to load classes from: ---");
        for(URL url : urls)log.debug(url.toString());
        log.debug("--- ---");
    }
}
