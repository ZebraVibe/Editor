package com.sk.editor.ui;

import com.artemis.Entity;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.config.Config;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.scripting.ScriptManager;
import com.sk.editor.ui.console.Console;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.ecs.components.Transform;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MenuBar extends UIBase{

    private static final EditorLogger log = new EditorLogger(MenuBar.class.toString(), Logger.DEBUG);

    private Skin skin;
    private UIStage uiStage;

    private ECSManager ecsManager;
    private EditorManager editorManager;
    private ScriptManager scriptManager;
    private Viewport ecsViewport;

    public MenuBar(Skin skin, UIStage uiSTage, Viewport ecsViewport, ECSManager ecsManager, EditorManager editorManager, ScriptManager scriptManager) {
        super(skin);
        this.skin = getSkin();
        this.uiStage = uiSTage;
        this.ecsViewport = ecsViewport;
        this.ecsManager = ecsManager;
        this.editorManager = editorManager;
        this.scriptManager = scriptManager;
        init();
    }

    private void init(){
        Skin skin = getSkin();

        // save button
        TextButton saveButton = new TextButton("Save", skin);
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ecsManager.saveWorld();
            }
        });


        // create entity button
        TextButton createButton = new TextButton("Create", skin);
        createButton.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Entity e = ecsManager.createCanvas();
                Transform transform = ecsManager.getTransformMapper().get(e);
                transform.setSize(64, 64);
                Vector3 camCoords = ecsViewport.getCamera().position;
                transform.setPosition(camCoords.x, camCoords.y);
            }
        });

        // source path button
        TextButton srcPathButton = new TextButton("Src-path", skin);
        srcPathButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Window window = new UIWindow("Set Source Path", skin);

                // src text field
                Label srcLabel = new Label("source path:", skin);
                TextField srcTF = new TextField("", skin);
                srcTF.setMessageText("gdx v1.11.0, artemis v2.30");
                String srcPath =  editorManager.getPrefKeys().SRC_PATH.get();
                if(!srcPath.isEmpty())srcTF.setText(srcPath);

                Table srcTable = new Table();
                srcTable.add(srcLabel).spaceRight(1);
                srcTable.add(srcTF).expandX().fillX();

                // package name text field
                Label packageLabel = new Label("package name:", skin);
                TextField packageTF = new TextField("", skin);
                packageTF.setMessageText("i.e. com.my.game");
                String packageName =  editorManager.getPrefKeys().PACKAGE_NAME.get();
                if(!packageName.isEmpty())packageTF.setText(packageName);

                Table packageTable = new Table();
                packageTable.add(packageLabel).spaceRight(1);
                packageTable.add(packageTF).expandX().fillX();


                // ok button
                TextButton okButton = new TextButton("ok", skin);
                okButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        updateScriptManager(srcTF.getText(), packageTF.getText());
                        Console console = uiStage.findUIActor(Console.class);
                        if(console != null)console.setVisible(true);
                    }
                });

                // close button
                TextButton closeButton = new TextButton("close", skin);
                closeButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        window.remove();
                    }
                });

                Table buttons = new Table();
                buttons.defaults().expandX().fillX();
                buttons.add(okButton).spaceRight(1);
                buttons.add(closeButton);

                window.pad(Config.DEFAULT_UI_PAD);
                window.defaults().spaceBottom(1).expandX().fillX();
                window.add(srcTable).row();
                window.add(packageTable).row();
                window.add(buttons);
                window.setSize(512,256);

                window.setPosition(event.getStage().getWidth() / 2f, event.getStage().getHeight() / 2f, Align.center);
                event.getStage().addActor(window);
            }
        });

        // chatGPT button
        Actor chatGPTButton = createChatGPTButton();

        // console button
        Actor consoleButton = createConsoleButton();

        // add to bar
        left();
        defaults().spaceRight(8);
        add(saveButton, createButton, consoleButton, srcPathButton, chatGPTButton);
    }

    private Actor createChatGPTButton() {
        TextButton button = new TextButton("ChatGPT", skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                UIWindow window = new UIWindow("ChatGPT",skin);
                window.setSize(256, 128);
                window.pad(Config.DEFAULT_UI_PAD);

                // content
                Table content = new Table(skin);
                content.left().top();
                content.defaults().expandX().fillX();
                ScrollPane scroll = new ScrollPane(content,skin);

                // conversation id
                String id =  editorManager.getPrefKeys().GPT_3_CONVERSATION_ID.get();
                if(id.isEmpty()){
                    UUID uuid = UUID.randomUUID();
                    id = uuid.toString();
                    editorManager.getPrefKeys().GPT_3_CONVERSATION_ID.set(id);
                }
                final String uniqueConversationID = id;

                content.add("[conversation ID] " + id).row();
                content.add("Enter openAI API key: ").row();


                // text field
                TextField textField = new TextField("",skin);
                String apiKey = editorManager.getPrefKeys().GPT3_API_KEY.get();
                if(!apiKey.isEmpty())textField.setText(apiKey);

                textField.addListener(new InputListener(){

                    OpenAiService service;

                    @Override
                    public boolean keyDown(InputEvent event, int keycode) {
                        if(keycode != Input.Keys.ENTER)return false;
                        String user = "test-user";
                        //models:(chatGPT, 10% price of davinci) gpt-3.5-turbo,(GPT) text-davinci-003
                        //note: chatGPT mpdel first message is system message is to tell the system its rolte
                        String model = "gpt-3.5-turbo";
                        String userText = textField.getText();
                        String resultText = "";

                        int maxTokens = 100;
                        ChatMessage message = null;

                        // initialising
                        if(service == null){
                            service = new OpenAiService(userText);
                            editorManager.getPrefKeys().GPT3_API_KEY.set(userText);

                            //content.add("Key obtained. Please enter your messages then:").row();
                            //textField.setText("");

                            String systemInstrcution = "You're an editor that uses libgdx and artemis-odb ECS. Help the " +
                                    "user as short & concisely as possible.";

                            // initial system instruction
                            message = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemInstrcution);
                            maxTokens = 10; // no to need to answer

                            // after entering api key
                        } else {
                            // roles: system, user, assistant
                            message = new ChatMessage(ChatMessageRole.USER.value(), userText);

                            // prompt / message
                            Label promptLabel = new Label("[user: " + user + "] " + userText, skin);
                            promptLabel.setWrap(true);
                            content.add(promptLabel).row();
                            content.add().row();

                        }
                        // to continue with an existing conversation
                        //String metadata = "{\"conversation_id\": \"" + uniqueConversationID +"\"}";

                        List<ChatMessage> messages = Arrays.asList(message);

                        ChatCompletionRequest request = ChatCompletionRequest.builder()
                                .model(model)
                                .user(user)
                                .maxTokens(maxTokens)
                                .messages(messages)// used in ChatCompletionRequest
                                //.prompt(metadata + promptText) // used in CompletionRequest
                                .temperature(0.2)//[0,2](default: 1):lower values like 0.2 more focued and deterministic, higher values like 0.8 more random
                                .n(1) // number of completions, can consume a lot of tokens
                                .build();
                        try {
                            ChatCompletionResult result = service.createChatCompletion(request);

                            for(ChatCompletionChoice choice : result.getChoices()){
                                //resultText = choice.toString();
                                resultText = choice.getMessage().getContent();
                            }
                        } catch (Exception e){
                            content.add("Exception while waiting for an Result:" +
                                    "\n" + e.toString() + "\n" +
                                    "Please enter your openAI API key again." ).row();
                            service = null;
                            textField.setText(editorManager.getPrefKeys().GPT3_API_KEY.get());
                            return true;
                        }

                        // result
                        Label resultLabel = new Label("[assistant: " + model + "] " + resultText, skin);
                        resultLabel.setWrap(true);
                        content.add(resultLabel).row();

                        //clear
                        textField.setText("");
                        return true;
                    }

                });

                // close
                TextButton closeButton = new TextButton("close",skin);
                closeButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        window.remove();
                    }
                });

                window.add(scroll).expand().fill().row();
                window.add(textField).expandX().fillX();
                window.add(closeButton);
                window.setPosition(getStage().getWidth() / 2, getStage().getHeight() /2, Align.center);
                getStage().addActor(window);
            }
        });
        return button;
    }


    private Actor createConsoleButton(){
        TextButton button = new TextButton("Console",skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Console console = uiStage.findUIActor(Console.class);
                console.toFront();
                if(console != null)console.setVisible(!console.isVisible());
            }
        });
        return button;
    }


    /**
     *
     * @param srcDir
     * @param packageName
     * @return true if the script manager could be successfully compiled and loaded
     */
    private boolean updateScriptManager(String srcDir, String packageName){
        Path newSrcPath = null;
        Path newClassPath = null;
        String newPackageName = packageName;

        try {
            newSrcPath = Paths.get(srcDir);
            newClassPath = Paths.get(newSrcPath.getParent().toString(), Config.CLASS_PATH_DIR_NAME);
        } catch (InvalidPathException e){
            log.error("Path in invalid." + e.toString());
            return false;
        }

        if(scriptManager.setSrcPath(newSrcPath)){
            // update prefs
            editorManager.getPrefKeys().SRC_PATH.set(newSrcPath.toString());
        }
        if(scriptManager.setClassPath(newClassPath)){
            // update prefs
            editorManager.getPrefKeys().CLASS_PATH.set(newClassPath.toString());
        }
        if(scriptManager.setPackageName(packageName)){
            // update prefs
            editorManager.getPrefKeys().PACKAGE_NAME.set(packageName);
        }

        boolean success = false;

        try {
            success = scriptManager.compileAndLoad();
        } catch (Exception e) {
            log.error("Could not compile and load. ", e);
            return false;
        }
        if (success)scriptManager.debugLoadedClasses();
        return true;
    }


}
