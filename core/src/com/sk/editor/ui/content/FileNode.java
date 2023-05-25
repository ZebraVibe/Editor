package com.sk.editor.ui.content;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Logger;
import com.sk.editor.assets.RegionNames;
import com.sk.editor.ui.UINode;

public class FileNode extends UINode<FileNode, FileHandle> {
    private static final Logger log = new Logger(FileNode.class.toString(), Logger.DEBUG);
    private TextureRegionDrawable folder, folderOpen;

    public FileNode(FileHandle file, Skin skin) {
        super("", skin);
        folder = new TextureRegionDrawable(skin.getRegion(RegionNames.FOLDER_SMALL));
        folderOpen = new TextureRegionDrawable(skin.getRegion(RegionNames.FOLDER_OPEN_SMALL));
        setIcon(folder);
        setValue(file); // sets name in here
    }

    @Override
    public void setValue(FileHandle value) {
        super.setValue(value);
        // set label text
        setText(value.nameWithoutExtension());
    }

    @Override
    public void setExpanded(boolean expanded) {
        super.setExpanded(expanded);
        // set icon
        setIcon(expanded && hasChildren()? folderOpen : folder);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        //log.info("Renaming file logic not implemented yet.");
    }
}
