package com.sk.editor.ui;

import com.artemis.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
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

        // project path Button
        Actor projPathButton = createProjPathButton();


        // chatGPT button
        Actor chatGPTButton = createChatGPTButton();

        // console button
        Actor consoleButton = createConsoleButton();

        // add to bar
        left();
        defaults().spaceRight(8);
        add(saveButton,
                createButton,
                consoleButton,
                projPathButton,
                chatGPTButton);
    }

    private Actor createProjPathButton() {
        TextButton button = new TextButton("Project-Path", getSkin());
        button.addListener(new ChangeListener() {

            UIWindow window;

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // create
                if(window == null){
                    window = new UIWindow("Set Project Path", skin);

                    // project text field
                    Label projLabel = new Label("proj path:", skin);
                    TextField projTF = new TextField("", skin);
                    projTF.setMessageText("gdx v1.11.0, artemis v2.30");
                    // get existing project path from prefs
                    String projPath =  editorManager.getPrefKeys().PROJECT_PATH.get();
                    if(!projPath.isEmpty())projTF.setText(projPath);

                    Table projTable = new Table();
                    projTable.add(projLabel).spaceRight(1);
                    projTable.add(projTF).expandX().fillX();


                    // ok button
                    TextButton okButton = new TextButton("ok", skin);
                    okButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            updateScriptManager(Gdx.files.absolute(projTF.getText()));
                            Console console = uiStage.findUIActor(Console.class);
                            if(console != null)console.setVisible(true);
                        }
                    });

                    window.pad(Config.DEFAULT_UI_PAD);
                    window.defaults().spaceBottom(1).expandX().fillX();
                    window.add(projTable).row();
                    window.add(okButton);
                    window.setSize(512,256);

                    window.setPosition(event.getStage().getWidth() / 2f, event.getStage().getHeight() / 2f, Align.center);
                    event.getStage().addActor(window);

                } else { // change visibility
                    window.setVisible(!window.isVisible());
                }
            }
        });
        return button;
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
     * @param projectPath an absolute path to a libgdx project
     * @return true if the script manager could be successfully compiled and loaded
     */
    private boolean updateScriptManager(FileHandle projectPath){
        try {
            scriptManager.setProjectPath(projectPath, true);
        } catch (Exception e){
            throw new GdxRuntimeException(e);
        }
        // update prefs
        editorManager.getPrefKeys().PROJECT_PATH.set(projectPath.path());

        // debug
        scriptManager.debugLoadedClasses();
        return true;
    }
}
