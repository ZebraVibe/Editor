package com.sk.editor.ui.console;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.config.Config;
import com.sk.editor.ui.UILabel;
import com.sk.editor.ui.UIWindow;

public class Console extends UIWindow {

    private Table content;
    private TextField commandLine;
    private ScrollPane scroll;
    private UILabel currentLabel;

    public Console(Skin skin) {
        super("Console", skin);
        init();
    }

    private void init() {
        Skin skin = getSkin();

        // scroll pane content
        content = new Table();
        content.top().left();
        content.defaults().left();
        content.add(currentLabel).row();

        // scroll pane
        scroll = new ScrollPane(content, skin);
        scroll.addListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(scroll);
            }
        });


        // command line
        commandLine = new TextField("", skin);
        commandLine.setMessageText("enter command");
        commandLine.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if(keycode == Input.Keys.ESCAPE){
                    event.getStage().setKeyboardFocus(null);
                    return true;
                }
                if (keycode != Input.Keys.ENTER) return false;
                print(commandLine.getText());
                // clear text field
                commandLine.setText("");
                return true;
            }
        });


        this.add(scroll).expand().fill().row();
        this.add(commandLine).expandX().fillX();
    }

    public void print(String text){
        print(text, Color.WHITE);
    }

    public void print(String text, int logLevel){
        Color color = null;
        if(logLevel == Logger.ERROR)color = Color.RED;
        else if(logLevel == Logger.INFO)color = Color.YELLOW;
        else {
            color = Config.GREEN;
        }
        print(text, color);
    }

    public void print(String text, Color color){
        if(currentLabel == null || !color.equals(currentLabel.getStyle().fontColor)){
            currentLabel = createLabel(color);
            content.add(currentLabel).row();
        }
        currentLabel.getText().append((currentLabel.getText().isEmpty() ? "" : "\n") + text);
        currentLabel.invalidateHierarchy();

        // scroll to bottom
        scroll.layout();
        scroll.setScrollPercentY(1);
    }

    // -- private --

    private UILabel createLabel(Color color){
        UILabel label = new UILabel("", new Label.LabelStyle(getSkin().get(Label.LabelStyle.class)));
        label.getStyle().fontColor = color.cpy();
        return label;
    }


}
