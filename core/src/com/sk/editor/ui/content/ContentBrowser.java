package com.sk.editor.ui.content;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.sk.editor.assets.RegionNames;
import com.sk.editor.scripting.FileWatcher;
import com.sk.editor.scripting.FileWatcherAdapter;
import com.sk.editor.scripting.ScriptManager;
import com.sk.editor.ui.UITree;
import com.sk.editor.ui.UIWindow;
import com.sk.editor.utils.ArrayPool;
import com.sk.editor.utils.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;

public class ContentBrowser extends UIWindow {

    private static final Logger log = new Logger(ContentBrowser.class.toString(), Logger.DEBUG);
    private ArrayPool arrayPool = new ArrayPool();

    private UITree<FileNode, FileHandle> dirTree;
    private Table contentTable;
    private FileWatcher assetsFileWatcher;
    private ScriptManager scriptManager;
    private AssetManager assetManager;


    public ContentBrowser(Skin skin, ScriptManager scriptManager, AssetManager assetManager) {
        super("Content", skin);
        this.scriptManager = scriptManager;
        this.assetManager = assetManager;
        init();
    }

    private void init() {
        //add(createDirPane()).expand().fill();
        //add(createFilePane()).expand().fill();

        // creates split pane containing tree and content pane
        add(createSplitPane()).expand().fill();


        // fills the tree
        appendDirTreeWith(scriptManager.getAssetsPath(), null);
        initFileWatcher();
    }

    private void initFileWatcher() {
        FileHandle assetsFile = scriptManager.getAssetsPath();
        Path assetsPath = assetsFile.file().toPath();
        // watch assets dir and sub dirs
        try {
            assetsFileWatcher = new FileWatcherAdapter(assetsPath) {
                @Override
                public void process(FileEvent event) {

                    boolean isDir = Files.isDirectory(event.path);
                    //boolean isFile = Files.isRegularFile(e.path);

                    //created, named to, moved to
                    if (event.kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (isDir) {
                            // add new dir to dir tree
                            FileHandle dir = Gdx.files.absolute(event.path.toAbsolutePath().toString());
                            FileNode node = new FileNode(dir, getSkin());
                            // can it be a new file object since equals() is overridden ?
                            Path parentPath = event.path.getParent().toAbsolutePath();
                            FileHandle parentDir = Gdx.files.absolute(parentPath.toString());
                            FileNode parentNode = dirTree.findNode(parentDir);

                            if(parentNode != null){
                                // get index
                                int index = parentNode.getChildren().size;
                                // list child dirs
                                FileHandle[] children = parentDir.list(f -> f.isDirectory());
                                for (int i = 0; i < children.length; i++) {
                                    if (children[i].name().equals(dir.name())) {
                                        index = i;
                                        break;
                                    }
                                }
                                boolean success = dirTree.insertNode(node, parentNode, index);

                            };


                        }

                        // check content table whether file (regular file or dir) is displayed there
                        FileNode node = dirTree.getSelectedNode();
                        if (node != null){
                            FileHandle selectedDir = node.getValue();
                            Path selectedDirPath = selectedDir.file().toPath();

                            // update content table - if parent dir is current selected node
                            if (event.path.getParent().equals(selectedDirPath.toAbsolutePath())) {
                                // update content table
                                fillContentTableWith(selectedDir);
                            }
                        }


                        // deleted, renamed, moved away
                    } else if (event.kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        // check if file(dir or regular file) was displayed in content table
                        FileNode selectedNode = dirTree.getSelectedNode();
                        if (selectedNode == null) return;
                        FileHandle selectedDir = selectedNode.getValue();
                        Path selectedDirPath = selectedDir.file().toPath();

                        // if parent dir is selected node update content table
                        if (event.path.getParent().equals(selectedDirPath.toAbsolutePath())) {
                            // update content table
                            fillContentTableWith(selectedDir);
                        }
                        // remove node
                        FileHandle dir = Gdx.files.absolute(event.path.toAbsolutePath().toString());
                        FileNode node = dirTree.findNode(dir);
                        // can't check if isDirectory since file was deleted and would return false
                        if (node != null) node.remove();
                    }
                }

            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assetsFileWatcher.start();
    }

    private Actor createSplitPane() {
        Actor first = createDirPane();
        Actor second = createContentPane();
        SplitPane splitPane = new SplitPane(first, second, false, getSkin());
        return splitPane;
    }


    private Actor createDirPane() {
        dirTree = createDirTree();

        // scroll pane
        Table scrollTable = new Table();
        ScrollPane scroll = new ScrollPane(scrollTable, getSkin());
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                //event.getStage().setScrollFocus(scroll);
            }
        });
        scroll.setFlickScroll(false);
        scrollTable.add(dirTree).expand().fill();
        return scroll;
    }

    private UITree<FileNode, FileHandle> createDirTree() {
        return new UITree<FileNode, FileHandle>(getSkin()) {
            @Override
            public void nodeSelected(FileNode selectedNode) {
                // fill content table
                fillContentTableWith(selectedNode.getValue());
            }

            @Override
            public boolean insertNode(FileNode node, FileNode parent, int index) {
                // setup index
                FileHandle file = node.getValue();
                FileHandle parentFile = parent.getValue();
                index = Math.min(index, (parent == null ? getRootNodes().size : parent.getChildren().size));

                /*
                // if file already exists don't insert and override
                if (parentFile.child(file.name()).exists()) {
                    log.error("not inserting node since parent already contains child with equal name");
                    return false;
                }*/

                // insert
                if (super.insertNode(node, parent, index)) {
                    file.moveTo(parentFile.child(file.name()));
                    return true;
                }
                return false;
            }
        };
    }

    private Actor createContentPane() {
        // scroll pane
        contentTable = new Table();
        ScrollPane scroll = new ScrollPane(contentTable, getSkin());
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(scroll);
            }
        });
        scroll.setFlickScroll(false);
        return scroll;
    }


    /**
     * clears the content table and fills it with the
     * children of the given parent directory
     *
     * @param parent
     */
    private void fillContentTableWith(FileHandle parent) {
        // clear content
        contentTable.clearChildren();

        // fill table
        fillTableWith(parent, contentTable);
    }

    /**
     * fills the given table with the child directories of
     * the given parent file
     *
     * @param parent
     */
    private void fillTableWith(FileHandle parent, Table content) {
        if (parent.isDirectory() == false) throw new GdxRuntimeException("parent must be existing directory");
        Array<FileHandle> children = arrayPool.obtain();
        children.addAll(parent.list());
        // sort children by directory
        children.sort((f1, f2) -> {
            if (f1.isDirectory()) return 1;
            else if (f2.isDirectory()) return -1;
            else return 0;
        });

        // fill table with children
        for (FileHandle child : children) {
            addFileAsActorToTable(child, content);
        }

        arrayPool.free(children);

    }

    /**
     * @param file
     * @param parentNode if null adds the given file as root
     */
    private void appendDirTreeWith(FileHandle file, @Null FileNode parentNode) {
        // append tree with file
        FileNode node = new FileNode(file, getSkin());
        if (parentNode == null) dirTree.add(node);
        else parentNode.add(node);

        log.debug("appended dir tree with: " + file.path());

        // append children
        FileHandle[] childDirs = file.list(f -> {
            return f.isDirectory();
        });
        for (FileHandle dir : childDirs) {
            appendDirTreeWith(dir, node);
        }
    }


    private void addFileAsActorToTable(FileHandle file, Table content) {
        if (file == null || content == null) throw new NullPointerException("file and content can not be null");
        Actor button = createFileActor(file);
        button.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //log.debug("clicking on folder");
                //if(!button.isChecked())return;
                if (getTapCount() == 2 && file.isDirectory()) {
                    log.debug("Trying to open folder: " + file.name());
                    FileNode node = dirTree.findNode(file);
                    dirTree.chooseNode(node);
                }
            }
        });

        content.add(button).spaceRight(4);

    }

    public Actor createFileActor(FileHandle file) {
        ImageTextButton.ImageTextButtonStyle style = new ImageTextButton.ImageTextButtonStyle(getSkin().get(ImageTextButton.ImageTextButtonStyle.class));
        ImageTextButton button = new ImageTextButton(file.nameWithoutExtension(), style);

        // placeholder image if texture not loaded
        TextureRegionDrawable trd = new TextureRegionDrawable(createFileIcon(file));
        style.up = null;
        style.imageUp = trd;

        button.getImageCell().size(64).row();
        Label label = button.getLabel();
        label.remove();
        button.add(label);
        button.invalidateHierarchy();

        // add action to set texture
        addAction(new Action() {
            @Override
            public boolean act(float delta) {
                if (assetManager.isLoaded(file.path(), Texture.class)) {
                    log.debug("Setting Texture of " + file.name());
                    button.getStyle().imageUp = new TextureRegionDrawable(createFileIcon(file));
                    //button.invalidateHierarchy();
                    return true;
                }
                return false;
            }
        });

        // give button file to hold
        button.setUserObject(file);

        return button;
    }

    /**
     * check if texture is loaded via @{@link AssetManager#isLoaded(String, Class)}
     *
     * @param file
     */
    private TextureRegion createFileIcon(FileHandle file) {
        Skin skin = getSkin();
        TextureRegion defaultFileRegion = skin.getRegion(RegionNames.FILE);
        if (file.isDirectory()) return skin.getRegion(RegionNames.FOLDER_BIG);
        else if (FileUtils.isImage(file)) return getRegionOf(file, true, defaultFileRegion);
        else if (FileUtils.isFont(file)) return skin.getRegion(RegionNames.FONT_FILE);
        else if (FileUtils.isAudio(file)) return skin.getRegion(RegionNames.AUDIO_FILE);
        //else if(isScene(file))return skin.getRegion(RegionNames.SCENE_FILE);
        return defaultFileRegion;// placeholder
    }

    /**
     * @param file
     * @param fallback will be returned if no texture of the file is loaded
     * @param load     if true loads the texture using the asset manager
     * @return
     */
    private TextureRegion getRegionOf(FileHandle file, boolean load, TextureRegion fallback) {
        if (assetManager.isLoaded(file.path(), Texture.class)) {
            log.debug("Texture Is Loaded: " + file.name());
            return new TextureRegion(assetManager.get(file.path(), Texture.class));
        }
        log.debug("Texture Not Loaded: " + file.name() + " -> loading? " + (load ? "[true]" : "[false]"));
        if (load) {
            assetManager.load(file.path(), Texture.class);
        }
        return fallback;
    }


}
