package com.sk.editor.ui.inspector;

import com.artemis.Component;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.utils.Bag;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.sk.editor.EditorManager;
import com.sk.editor.assets.SkinNames;
import com.sk.editor.ecs.ECSManager;
import com.sk.editor.scripting.ScriptManager;
import com.sk.editor.ui.UIActor;
import com.sk.editor.ui.UINames;
import com.sk.editor.utils.UIUtils;
import com.sk.editor.world.components.Transform;

public class Inspector extends UIActor {

    private static final Logger log = new Logger(Inspector.class.toString(), Logger.DEBUG);

    private Skin skin;
    private TextureRegion pixelRegion;
    private ShaderProgram roundedCorners, roundedCornersShadow;
    private ECSManager ecsManager;
    private ScriptManager scriptManager;
    private EditorManager editorManager;


    public Inspector(Skin skin, ShaderProgram roundedCorners, ShaderProgram roundedCornersShadow,
                     ECSManager ecsManager, EditorManager editorManager, ScriptManager scriptManager) {
        super(skin.get(SkinNames.PIXEL_REGION, TextureRegion.class), roundedCorners, roundedCornersShadow);
        this.skin = skin;
        this.pixelRegion = skin.get(SkinNames.PIXEL_REGION, TextureRegion.class);
        this.roundedCorners = roundedCorners;
        this.roundedCornersShadow = roundedCornersShadow;
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
        UIActor container = new UIActor(pixelRegion, roundedCorners, roundedCornersShadow);
        container.setBackgroundAlpha(0);
        //container.defaults().expandX().fillX();

        try {
            Label label = createLabel(obj, field);// sets widget name too

            TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle(skin.get(SkinNames.TEXT_FIELD_STYLE10, TextField.TextFieldStyle.class));
            String value = "" + UIUtils.getFieldValue(field, obj);
            TextField tf = new TextField(value, tfStyle) {

                //Object oldValue = UIUtils.getFieldValue(field, obj);

                @Override
                public void act(float delta) {
                    /*
                    try {
                        // upate displayed value on external field change
                        if (!hasKeyboardFocus() && !oldValue.equals(getFieldValue(field, obj))) {
                            oldValue = getFieldValue(field, obj);
                            setText("" + getFieldValue(field, obj));
                        }
                    } catch (Exception e) {
                    }
                    */

                    super.act(delta);
                }
            };
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
        UIActor componentContainer = new UIActor(pixelRegion, roundedCorners, roundedCornersShadow);
        componentContainer.setBackgroundAlpha(0);
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
            }
        });
        return button;
    }
    private Button createAddComponentButton() {
        TextButton button = createTextButton();
        button.setText("Add Comp");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                //...
            }
        });
        return button;
    }

    private TextButton createTextButton() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
                skin.get(SkinNames.IMAGE_TEXT_BUTTON_STYLE_10, ImageTextButton.ImageTextButtonStyle.class));
        TextButton button = new TextButton("", style);
        return button;
    }

    // -- public --


    /**
     * May update the inspectors information via {@link #fill(Entity)}
     * if the given inspected entity differs from
     * the new selected one
     *
     * @param newSelected the entity which is inspected
     * @return whether the refreshment was successfull
     */
    public boolean update(Entity newSelected, Entity previous) {
        if (newSelected == previous) return false;
        fill(newSelected);
        return true;
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
        ComponentMapper<T> mapper = ecsManager.getWorld().getMapper(componentType);
        Entity entity = ecsManager.getSelectedEntity();
        if (entity == null) return false;
        try {
            Object obj = mapper.get(entity);
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
        Table scrollContainer = new Table();
        scrollContainer.top();
        scrollContainer.defaults().expandX().fillX();
        ScrollPane scrollPane = createScrollPane(scrollContainer);

        // get components and fill bag
        Bag<Component> bag = Pools.obtain(Bag.class);
        bag.clear();
        entity.getComponents(bag);
        for (Component c : bag) {
            Actor componentContainer = createComponentContainer(c);
            scrollContainer.add(componentContainer).row();
        }
        Pools.free(bag);

        // create add button
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


    /**
     * @param inspected can be null
     */
    public void updateInspectorPositionRelativeTo(@Null Entity inspected, Stage ecsStage) {
        if (inspected == null) return;
        Stage uiStage = getStage();
        Viewport uiViewport = uiStage.getViewport();
        Vector2 newCoord = Pools.obtain(Vector2.class);
        Transform transform = ecsManager.getTransformMapper().get(inspected);
        float zoom = ((OrthographicCamera) uiViewport.getCamera()).zoom;
        // above entity
        float newX = transform.x + transform.width / 2f;
        float newY = transform.y + transform.height;


        newCoord.set(newX, newY); // top right corner
        ecsStage.stageToScreenCoordinates(newCoord); // world to screen coord
        uiStage.screenToStageCoordinates(newCoord);
        float gap = 16;
        newCoord.y += gap;

        //clamp inside bounds
        Actor boundsActor = uiStage.getRoot().findActor(UINames.INSPECTOR_CLAMPED_BOUNDS);

        // set position
        float time = 0.25f;
        int alignment = Align.bottom;
        float xDiff = getX(alignment) - getX();
        float yDiff = getY(alignment) - getY();
        //clamp inside bounds
        float toX = MathUtils.clamp(newCoord.x, boundsActor.getX() + xDiff, boundsActor.getX(Align.right) - (getWidth() - xDiff));
        float toY = MathUtils.clamp(newCoord.y, boundsActor.getY() + yDiff, boundsActor.getY(Align.top) - (getHeight() - yDiff));

        /*
        Array<Action> actions = getActions();
        boolean hasAction = false;
        for (Action action : actions) {
            if (action instanceof MoveToAction) {
                MoveToAction a = (MoveToAction) action;
                a.setPosition(toX, toY, alignment);
                hasAction = true;
            }
        }

        if (!hasAction) {
            MoveToAction action = Actions.moveToAligned(toX, toY, alignment, time, Interpolation.smooth2);
            addAction(action);
        }*/

        getActions().clear();
        MoveToAction action = Actions.moveToAligned(toX, toY, alignment, time, Interpolation.smooth2);
        addAction(action);


        Pools.free(newCoord);

    }

}
