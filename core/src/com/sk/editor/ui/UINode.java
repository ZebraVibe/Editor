package com.sk.editor.ui;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;

public class UINode<N extends UINode<N,V>, V> extends Tree.Node<N, V, UIBase > {

    private Skin skin;
    private Label label;
    private UIBase container;

    public UINode(String text, Skin skin) {
        this.skin = skin;
        container = new UIBase(skin);
        label = newLabel(text, skin);

        container.add(label).expandX().fillX();
        setActor(container);
    }

    /**
     * used to create the label for the node
     * @param text
     * @param skin
     * @return
     */
    protected Label newLabel(String text, Skin skin){
        return new Label(text,skin);
    }


    public void setText(String text){
        label.setText(text);
    }

    public String getText(){
        return label.getText().toString();
    }

    public GlyphLayout getGlyphLayout(){
        return label.getGlyphLayout();
    }

    public Label.LabelStyle getLabelStyle(){
        return label.getStyle();
    }


    // -- private --


}
