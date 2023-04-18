package com.sk.editor.ui.inspector;

import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.utils.Bag;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.scripting.ScriptManager;
import com.sk.editor.ui.UIBase;
import com.sk.editor.ui.logger.EditorLogger;
import com.sk.editor.utils.ArrayPool;
import com.sk.editor.utils.UIUtils;
import com.sk.editor.ecs.components.Transform;

public class Inspector extends UIBase {

    private static final EditorLogger log = new EditorLogger(Inspector.class.toString(), Logger.DEBUG);

    private final ArrayPool arrayPool = new ArrayPool();


    private Skin skin;
    private Stage ecsStage;
    private ECSManager ecsManager;
    private ScriptManager scriptManager;
    private EditorManager editorManager;
    private @Null Entity currentEntity;
    private @Null Table currentScrollContainer;
    private Rectangle clampCoord = new Rectangle();


    public Inspector(Skin skin, Stage ecsStage, ECSManager ecsManager, EditorManager editorManager, ScriptManager scriptManager) {
        super(skin);
        this.skin = skin;
        this.ecsStage = ecsStage;
        this.ecsManager = ecsManager;
        this.editorManager = editorManager;
        this.scriptManager = scriptManager;
    }


    /**
     * @param obj the object containing the desired fields
     * @return the array containing all widgets
     */
    private Array<Actor> createAllFieldWidgets(Object obj) {
        Class cls = obj.getClass();
        Field[] fields = UIUtils.getFieldsOf(cls);
        Array<Actor> actors = new Array<>();

        //log.debug("Collecting fields of: " + cls.getSimpleName());

        for (Field field : fields) {
            if (field == null)
                continue;
            Actor widget = null;
            try {
                widget = createFieldWidget(obj, field);
            } catch (Exception e) {
                log.error("Could not create UI actor for Field.");
                e.printStackTrace();
            }
            actors.add(widget);
        }
        return actors;
    }

    /**
     * @param obj   the object containing the field
     * @param field
     * @return
     * @throws Exception
     */
    private Actor createFieldWidget(Object obj, Field field) throws Exception {
        if (obj == null || field == null) return null;
        Class<?> type = field.getType();

        if (type == int.class || type == float.class || type == double.class ||
                type == long.class || type.isAssignableFrom(String.class)) {
            return createTextField(obj, field);

        } else if (type == boolean.class) {
            return createCheckBox(obj, field);

        } else if (type instanceof Object) {
            /*
            if (type == EntityConsumer.class && objHolder instanceof Entity) {
                return createTextButtonOfField((Entity) objHolder, obj, field);
            }
			}
            else { // texture regions or any other default object are handled as select or dragg in box
                return createSelectOrDragInBox((Entity) objHolder, obj, field, field.getType());
            }*/
            return null;//createSelectOrDragInBox((Entity) objHolder, obj, field, field.getType());

        }

        return null;
    }


    private @Null <T extends Actor> T findWidget(Group start, String widgetName) {
        return start.findActor(widgetName);
    }

    private @Null <T extends Actor> T findFieldValueWidget(Class<? extends Component> componentType, String fieldName) {
        Group actor = findWidget(this, getComponentWidgetName(componentType));
        return actor.findActor(fieldName);
    }


    /**
     * @param obj   the object containing the field
     * @param field
     * @return
     */
    private <T> Actor createTextField(Object obj, Field field) {
        UIBase container = newUIBase();

        try {
            Label label = createLabel(obj, field);// sets widget name too

            TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
            String value = "" + UIUtils.getFieldValue(field, obj);
            TextField tf = new TextField(value, tfStyle);
            // set widget name
            tf.setName(field.getName());

            // update field and value text on unfocus
            tf.addCaptureListener(new FocusListener() {
                @Override
                public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {

                    if (event.isFocused())
                        return;

                    try {
                        UIUtils.setFieldValue(field, obj, parsePrimitiveType(tf.getText(), field.getType()));
                        //log.debug("Changing field value on focus lost");
                    } catch (Exception e) {
                        //log.error("[!!] Resetting value since couldnt set field value");

                        try {// reset value
                            tf.setText("" + UIUtils.getFieldValue(field, obj));
                        } catch (Exception e1) {
                            log.error("Could not reset value!");
                            e1.printStackTrace();
                        }
                        //e.printStackTrace();
                    }

                }

            });

            // update field
            tf.addListener(new InputListener() {

                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    // remove keyboard focus on enter
                    if(keycode == Input.Keys.ENTER){
                        event.getStage().setKeyboardFocus(null);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean keyTyped(InputEvent event, char character) {
                    try {
                        UIUtils.setFieldValue(field, obj, parsePrimitiveType(tf.getText(), field.getType()));
                        //log.debug("Changing field value");
                    } catch (Exception e) {
                        log.error("Could not set value!");
                        e.printStackTrace();
                    }
                    return true;
                }
            });

            container.add(label).expandX().fillX();
            container.add(tf);

        } catch (Exception e) {
            log.error("Could not create field instance and therefore no textfield.");
            e.printStackTrace();
        }

        return container;
    }

    private UIBase newUIBase() {
        UIBase actor = new UIBase(skin);
        UIStyle style = actor.getStyle();
        style.backgroundColor.a = 0;
        style.shadowSize = 0;
        return actor;
    }


    /**
     * @param obj   the object containing the desired field
     * @param field
     * @return
     */
    private Actor createCheckBox(Object obj, Field field) {
        CheckBox box = new CheckBox("", skin);
        box.setName(field.getName());
        box.clearChildren();

        Label label = createLabel(obj, field);
        Image img = box.getImage();
        box.add(label).expandX().fillX();
        box.add(img).expandX();

        try {
            box.setChecked((boolean) UIUtils.getFieldValue(field, obj));
        } catch (Exception e) {
            log.error("[!!] couldnt change checked state of checkbox");
            e.printStackTrace();
        }

        // update field
        box.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    UIUtils.setFieldValue(field, obj, box.isChecked());
                    //log.debug("Changing field value");
                } catch (Exception e) {
                    log.error("could not change checked state of checkbox on click");
                    e.printStackTrace();
                }
            }
        });
        return box;
    }

    /**
     * @param obj   the object containing the desired field
     * @param field
     * @return
     */
    private Label createLabel(Object obj, Field field) {
        String text = UIUtils.getNameOfSerializedFieldIfPresent(field);
        Label label = createLabel(text);
        return label;
    }

    private Label createLabel(String text) {
        Label label = new Label(text, skin);
        return label;
    }

    private ScrollPane createScrollPane(Table scrollContainer) {
        scrollContainer.setTouchable(Touchable.enabled);
        ScrollPane scrollPane = new ScrollPane(scrollContainer, skin);
        //scrollPane.setFillParent(true);
        scrollPane.setFlickScroll(false);
        scrollPane.setVariableSizeKnobs(true);
        scrollContainer.defaults().expandX().fillX();
        scrollContainer.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                event.getStage().setScrollFocus(event.getListenerActor());
            }
        });
        return scrollPane;
    }

    private Actor createComponentContainer(Component c) {
        UIBase componentContainer = newUIBase();
        String componentName = getComponentWidgetName(c.getClass());
        componentContainer.setName(componentName);

        componentContainer.defaults().spaceBottom(1);
        // create component label
        Label label = createLabel(componentName);

        // add component label
        componentContainer.add(label).row();

        // add component attributes
        Array<Actor> widgets = createAllFieldWidgets(c);
        for (Actor widget : widgets)
            componentContainer.add(widget).expandX().fillX().row();
        return componentContainer;
    }

    private String getComponentWidgetName(Class<? extends Component> componentType) {
        return componentType.getSimpleName();
    }


    private Button createAddScriptButton() {
        TextButton button = createTextButton();
        button.setText("Add Script");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                //.. add script
                Class componentClass = com.artemis.Component.class;
                Class systemClass = com.artemis.BaseSystem.class;
                Array<Class<?>> array = arrayPool.obtain();

                // debug components
                try {
                    log.debug("-----------------------------------------------");
                    log.debug("Listing loaded scripts with super class: " + componentClass.getName());
                    scriptManager.getSubTypesOf(componentClass, array);
                    array.forEach(e -> log.debug(e.getName()));
                } catch (ReflectionException e) {
                    log.error("Could not get sub types of " + componentClass.getName(), e);
                }
                array.clear();

                // debug systems
                try {
                    log.debug("-----------------------------------------------");
                    log.debug("Listing loaded scripts with super class: " + systemClass.getName());
                    scriptManager.getSubTypesOf(systemClass, array);
                    array.forEach(e -> log.debug(e.getName()));
                } catch (ReflectionException e) {
                    log.error("Could not get sub types of " + systemClass.getName(), e);
                }

                arrayPool.free(array);
            }
        });
        return button;
    }
    private Button createAddComponentButton() {
        TextButton button = createTextButton();
        button.setText("Add Comp");

        button.addListener(new ChangeListener() {
            ScrollPane scroll;
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // hide list again
                if(scroll != null){
                    removeScroll();
                    return;
                }

                // create component list
                List<Class<? extends Component>> list = new List<>(skin){
                    @Override
                    public String toString(Class<? extends Component> object) {
                        return object.getSimpleName();
                    }
                };
                list.setSelected(null);
                scroll = new ScrollPane(list, skin);
                scroll.setFlickScroll(false);
                scroll.setSize(getWidth(),128);


                Array<Class<? extends Component>> componentTypes = arrayPool.obtain();
                try {
                    scriptManager.getSubTypesOf(Component.class, componentTypes);
                    list.setItems(componentTypes);

                    // debug
                    log.debug("-----------------------------------------------");
                    log.debug("Listing loaded scripts with super class: " + Component.class.getName());
                    componentTypes.forEach(e -> log.debug(e.getName()));

                    // add listener
                    list.addListener(new ClickListener(){
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            Class<? extends Component> c = list.getSelected();
                            log.debug("selecting from list: " + c);
                            if(c == null)return;
                            update(currentEntity, c);
                            removeScroll();
                        }
                    });

                    addActor(scroll);
                    scroll.setPosition(0, 0, Align.topLeft);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                arrayPool.free(componentTypes);
            }

            private void removeScroll(){
                if(scroll == null)return;
                scroll.remove();
                scroll = null;
            }

        });
        return button;
    }

    private TextButton createTextButton() {
        TextButton button = new TextButton("", skin);
        return button;
    }

    // -- util --

    /**
     * doesnt include objects /classes/enums/interfaces
     */
    private @Null Object parsePrimitiveType(String value, Class<?> type) throws Exception {
        if (type == int.class)
            return Integer.parseInt(value);
        else if (type == float.class)
            return Float.parseFloat(value);
        else if (type == double.class)
            return Double.parseDouble(value);
        else if (type == long.class)
            return Long.parseLong(value);
        else if (type.isAssignableFrom(String.class))
            return value;
        else if (type == boolean.class)
            return Boolean.parseBoolean(value);
        // else if(castType instanceof Object) {}
        return null;
    }


    // -- public --

    /**
     *
     * @param entity if null and the return is true no entity is currently displayed on
     * @return true if the inspector shows information about the given entity
     */
    public boolean isCurrent(@Null Entity entity){
        return entity == currentEntity;
    }

    /**
     * May update the inspectors information via {@link #fill(Entity)}
     * if the current inspected entity differs from
     * the new selected one
     *
     * @param newSelected the entity which is inspected
     */
    public <T extends Component> void update(@Null Entity newSelected, Class<T>...newComponentTypes) {
        // update current content
        if (isCurrent(newSelected)) {

            // add new components to already selected
            if(currentEntity != null && newComponentTypes != null){
                for(Class<T> c : newComponentTypes){
                    ComponentMapper<?> mapper =  currentEntity.getWorld().getMapper(c);

                    // check if component is already present
                    if(mapper.has(currentEntity)) {
                        log.debug("Component " + c +" already added to entity");
                        return;
                    }

                    // add component
                    Component newComponent;
                    log.debug("Creating component " + c +" for entity");
                    try{
                        newComponent = mapper.create(currentEntity);
                    }catch (Exception e){
                        log.error("Could not create component" + c, e);
                        return;
                    }

                    if(currentScrollContainer != null){
                        log.debug("Adding component " + c +" to entity");
                        Actor componentContainer = createComponentContainer(newComponent);
                        currentScrollContainer.add(componentContainer).row();
                    }
                }
            }
            return;
        }

        // fill with new content
        currentEntity = newSelected;
        fill(newSelected);
    }

    /**
     * Updates a widget value of a components' field from the current selected entity.
     * Currently only checks for TextField and checkbox widgets
     *
     * @param componentType
     * @param fieldName
     * @param newValue
     * @param <T>
     * @return true if update was successfull
     */
    public <T extends Component> boolean updateWidgetValue(Class<T> componentType, String fieldName, Object newValue) {
        ComponentMapper<T> mapper = ecsManager.getMapper(componentType);;
        if (currentEntity == null) return false;
        try {
            Object obj = mapper.get(currentEntity);
            Field field = UIUtils.getDeclaredFieldOf(componentType, fieldName);

            Actor widget = findFieldValueWidget(componentType, fieldName);
            if (widget == null) throw new GdxRuntimeException("widget not found");

            Object value;
            try {
                value = UIUtils.getFieldValue(field, obj);
            } catch (Exception e) {
                throw new GdxRuntimeException("could not get field value");
            }

            // text field
            if (widget instanceof TextField) {
                TextField tf = (TextField) widget;
                tf.setText("" + value);

                // check box
            } else if (widget instanceof CheckBox) {
                CheckBox cb = (CheckBox) widget;
                cb.setChecked((boolean) value);
            }

            return true;
        } catch (ReflectionException e) {
            log.error("Could not update widget value");
            e.printStackTrace();
        }

        return false;
    }


    /**
     * Clears the inspector then fills it with component information of the current entity.
     * Call this when the components of the current inspected entity change
     *
     * @param entity
     */
    public void fill(Entity entity) {
        if (entity == null) return;
        // set up
        clearChildren(true);
        // Note: the inspectors' pad is set in EditorScreen for a coherent layout

        // create scrollPane
        currentScrollContainer = new Table();
        currentScrollContainer.top();
        currentScrollContainer.defaults().expandX().fillX();
        ScrollPane scrollPane = createScrollPane(currentScrollContainer);

        // get components and fill bag
        Bag<Component> bag = Pools.obtain(Bag.class);
        bag.clear();
        entity.getComponents(bag);
        for (Component c : bag) {
            Actor componentContainer = createComponentContainer(c);
            currentScrollContainer.add(componentContainer).row();
        }
        Pools.free(bag);

        // create add buttons
        Button addComponentButton = createAddComponentButton();
        Button addScriptButton = createAddScriptButton();
        Table buttons = new Table();
        buttons.defaults().expandX().fillX();
        buttons.add(addComponentButton);
        buttons.add(addScriptButton);

        // add to inspector
        //top();
        defaults();//.expandX().fillX().spaceBottom(1);
        add(scrollPane).expand().fill().row();
        add(buttons).expandX().fillX();
        //pack();
        //setDebug(true);
        // inform to redo layout
        invalidateHierarchy();
    }


    public void setClampedBounds(float x, float y, float width, float height ){
        clampCoord.set(x, y, x + width,  y + height);
    }

    /**
     * positions the inspector relative to the given to be inspected entity
     * @param inspected nullable.
     */
    public void setPositionRelativeTo(@Null Entity inspected) {
        if (inspected == null) return;
        Stage uiStage = getStage();
        Viewport uiViewport = uiStage.getViewport();
        Transform transform = ecsManager.getTransformMapper().get(inspected);
        float zoom = ((OrthographicCamera) uiViewport.getCamera()).zoom;

        Vector2 newCoord = Pools.obtain(Vector2.class);

        // entity top coord
        transform.getPosition(newCoord, Align.top);

        //ecsStage.stageToScreenCoordinates(newCoord); // world to screen coord
        transform.parentToScreenCoord(newCoord); // uses its correct viewport
        uiStage.screenToStageCoordinates(newCoord);

        // add gap
        float gap = 16;
        newCoord.y += gap;

        // position bottom left corner properly
        newCoord.x -= getWidth() /2f;


        //clamp inside bounds
        float clampX1 = (clampCoord.x >= 0 ? clampCoord.x : 0);
        float clampX2 = (clampCoord.width > 0 ? clampCoord.width : getParent().getWidth());

        float clampY1 = (clampCoord.y >= 0 ? clampCoord.y : 0);
        float clampY2 = (clampCoord.height > 0 ? clampCoord.height : getParent().getHeight());


        int toX = (int)MathUtils.clamp(newCoord.x, clampX1, clampX2 - getWidth());
        int toY = (int)MathUtils.clamp(newCoord.y, clampY1, clampY2 - getHeight());
        setPosition(toX, toY);


        Pools.free(newCoord);

    }


}
