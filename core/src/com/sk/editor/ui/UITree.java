package com.sk.editor.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Null;
import com.sk.editor.ui.logger.EditorLogger;

import java.util.function.Supplier;

public class UITree<N extends UINode<N,V>,V> extends Tree<N,V> {

    private static final EditorLogger log = new EditorLogger(UITree.class.toString(), Logger.DEBUG);

    private @Null Skin skin;
    private Image line, overNodeCenterDraggingActor;
    private UIBase dragActor;
    private N lastOver;

    private float iconSpacingLeft, iconSpacingRight;


    public UITree(Skin skin) {
        super(new UITreeStyle(skin.get(UITreeStyle.class)));
        setSkin(skin);
        init();
    }

    private void init(){
        UITreeStyle treeStyle = getStyle();
        if(treeStyle.uiStyle == null)throw new NullPointerException("uiStyle must not be null.");
        getSelection().setMultiple(false);
        setIconSpacing(2,2); // to obtain default spacing from Tree.class

        // pixel region
        TextureRegion pixelRegion = treeStyle.uiStyle.pixel;

        // double click
        addListener(createDoubleClickListener());

        // insert drag line
        line = new Image(pixelRegion);
        line.setColor(treeStyle.lineColor);
        line.setTouchable(Touchable.disabled);
        line.setHeight(1);
        line.setVisible(false);
        addActor(line);

        // drag into node actor: over Image
        overNodeCenterDraggingActor = new Image(pixelRegion);
        overNodeCenterDraggingActor.setColor(1,1,1, 0.1f);
        overNodeCenterDraggingActor.setTouchable(Touchable.disabled);
        overNodeCenterDraggingActor.setVisible(false);
        addActor(overNodeCenterDraggingActor);

        // drag indicator icon next to mouse
        dragActor = new UIBase(skin);
        //dragIcon.setColor(1,1,1, 0.5f);
        dragActor.setTouchable(Touchable.disabled);
        dragActor.setVisible(false);
        addActor(dragActor);

        // drag listener
        DragListener dragListener = createDragListener();

        dragListener.setTapSquareSize(4);
        addListener(dragListener);
    }

    private ClickListener createDoubleClickListener(){
        return new ClickListener() { // expand / collapse on double click
            @Override
            public void clicked(InputEvent event, float x, float y) {
                N node = getOverNode(); // can be null when clicking void
                N selected = getSelectedNode();// selected node doesnt turn null when clicking void
                boolean hasNodeChanged = node != lastOver || selected != node;
                if(hasNodeChanged)setTapCount(1);
                treeClicked(getTapCount(), node, hasNodeChanged);
                lastOver = getOverNode();
            }
        };
    }

    private DragListener createDragListener(){
        return new DragListener() {
            Rectangle tmpCenterRect = new Rectangle();
            Drawable overDrawable;
            N dragNode, insertParent;
            int insertIndex;
            Color tmpColor = new Color();


            @Override
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                N overNode = getOverNode();
                if(overNode != null ) {
                    dragNode = overNode;
                    tmpColor.set(dragNode.getActor().getColor());

                    float ySpacing = getYSpacing();
                    float bottomYSpacing = ySpacing / 2;// in tree ySpacing / (int)2 is used (rounds down)
                    float nodeY = overNode.getActor().getY() - bottomYSpacing;
                    float nodeHeight = overNode.getHeight();

                    // dragged node table consists of icon and label
                    dragActor.clearChildren();
                    dragActor.add(new Image(dragNode.getIcon())).spaceLeft(iconSpacingLeft).spaceRight(iconSpacingRight);
                    dragActor.add(dragNode.getText());
                    dragActor.pack();
                    dragActor.getColor().a = 0.5f; // decrease drag actor alpha
                    dragNode.getActor().getColor().a = 0.5f; //  decrease alpha of node that is being dragged

                    // don not display over drawable while dragging
                    overDrawable = getStyle().over;
                    getStyle().over = null;

                    // avoid being able to drag parent into children
                    overNode.collapseAll();

                    //keyboard focus to detect escape press
                    event.getStage().setKeyboardFocus(UITree.this);
                }
            }


            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                line.setVisible(false);
                overNodeCenterDraggingActor.setVisible(false);
                insertIndex = -1;
                insertParent = null;
                if(dragNode == null)return;

                //position and show drag actor
                dragActor.setVisible(true);
                dragActor.setPosition(x , y, Align.topLeft);


                // note: while dragging no new overNode is Set and therefore no over drawable
                N overNode = getNodeAt(y); //tree.getOverNode();

                if(overNode == null){ // if over node is null it must be below last node or above first node
                    N lastVisibleNode = getLastVisibleNode();

                    // if tree is childless or above last node it must have index 0
                    int index = lastVisibleNode == null ||  y >  lastVisibleNode.getActor().getY()? 0 : getRootNodes().size;
                    //onInsertion = () -> insertNode(dragNode, null,index);
                    insertParent = null;
                    insertIndex = index;
                    return;
                }
                Table overActor = overNode.getActor();
                GlyphLayout layout = overNode.getGlyphLayout();
                Label.LabelStyle style = overNode.getLabelStyle();
                BitmapFont font = style.font;

                // rect in center of node that is smaller than node for
                // inserting between nodes and adding as child when over center
                float capHeight = font.getCapHeight();
                float centerWidth = getWidth();
                float centerHeight = layout.height;//using font height
                float centerX = 0;
                float centerY = overActor.getY() + (overActor.getHeight() - centerHeight) / 2f;
                float centerYTop = centerY + centerHeight;

                // node coordinates
                float ySpacing = getYSpacing();
                float bottomYSpacing = ySpacing / 2;// in tree ySpacing / 2 is used (rounds down)
                float nodeHeight = overNode.getHeight() + ySpacing;
                float nodeY = overActor.getY() - bottomYSpacing;
                float nodeYTop = nodeY + nodeHeight;

                // set line width
                line.setWidth(getWidth());

                // is in center of node - insert to overNode as parent by appending its children
                tmpCenterRect.set(centerX, centerY, centerWidth, centerHeight);
                boolean isInCenter = tmpCenterRect.contains(x,y);

                if(isInCenter) {
                    if(overNode == dragNode)return;

                    // lighten node that is being dragged over by showing the actor
                    overNodeCenterDraggingActor.setVisible(true);
                    overNodeCenterDraggingActor.setBounds(0, nodeY, getWidth(), nodeHeight);

                    // insert node
                    insertParent = overNode;
                    insertIndex = overNode.getChildren().size;

                } else { // always insert to overNode siblings except if nextNode is child then insert to overNode children
                    boolean isBelowCenter = y <= centerY; // can only be below or above center here

                    // check next node
                    N nextNode = getNodeAt(y - nodeHeight);
                    boolean isNextNodeChild = nextNode != null && overNode.isAscendantOf(nextNode);

                    // line coord
                    float lineX; // by default y of over node
                    float lineY = isBelowCenter ? nodeY : nodeYTop;

                    if(isBelowCenter && isNextNodeChild){ // node before overNode is parent
                        lineX = calcLineBottomXOfNode(nextNode);
                        insertParent = overNode;
                    } else {
                        lineX = calcLineBottomXOfNode(overNode);
                        insertParent = overNode.getParent();
                    }

                    line.setPosition(lineX, lineY);
                    line.setVisible(true);

                    if(overNode == dragNode)return; // do it here to still draw lines
                    if(overNode == insertParent)insertIndex = 0; // parent is overNode
                    else if(insertParent == null) insertIndex = getRootNodes().indexOf(overNode, true) + (isBelowCenter ? 1 : 0); // parent is root
                    else insertIndex = insertParent.getChildren().indexOf(overNode, true) + (isBelowCenter ? 1 : 0); // parent is over node parent ( not null)
                }
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer) {
                if(dragNode == null)return;
                // successful inserted
                if(insertNode(dragNode, insertParent, insertIndex)){
                    chooseNode(dragNode);
                } else {
                    //...
                }
                reset();
            }


            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if(keycode == Input.Keys.ESCAPE) {
                    reset();
                    return true;
                }
                return false;
            }

            private void reset() {
                if(dragNode != null)dragNode.getActor().getColor().set(tmpColor);
                dragNode = null;
                insertParent = null;
                insertIndex = -1;
                getStyle().over = overDrawable;
                dragActor.clearChildren();
                dragActor.setVisible(false);
                line.setVisible(false);
                overNodeCenterDraggingActor.setVisible(false);
            }


        };
    }

    /**called whenever a node is clicked.
     * Collapses or expands the double clicked node by defaul.t*/
    private void treeClicked(int tapCount, N node, boolean hasNodeChanged) {
        if(node == null)return;
        if (tapCount % 2 == 0)node.setExpanded(!node.isExpanded());
        else if(tapCount == 1 && hasNodeChanged) nodeSelected(node);
    }


    private float calcLineBottomXOfNode(N overNode){
        Drawable icon = overNode.getIcon();
        return overNode.getActor().getX() - iconSpacingLeft - ( icon != null ? icon.getMinWidth() + iconSpacingRight  : 0);
    }

    /**
     *
     * @param node the node to insert
     * @param parent the parent to insert the node to. If null the node will be inserted as root node
     * @param index must be >= 0. the index to insert the node to
     * @return
     */
    protected boolean insertNode(N node, @Null N parent, int index){
        if(node == null || node == parent || index < 0 || (parent != null && index == parent.getChildren().indexOf(node, true))) {
            log.error("not inserting node");
            return false;
        }

        Array<N> children = parent == null ? getRootNodes() : parent.getChildren();
        N prevNode = index > 0 ? children.get(index-1) : null; // prev node index used to calc index correctly
        if(prevNode == node){
            log.error("not inserting node");
            return false;// inserting after itself
        }
        node.remove(); // remove from old parent
        index = prevNode == null ? 0 : children.indexOf(prevNode, true) + 1; // calc index anew
        if(parent == null)insert(index, node);// insert
        else parent.insert(index, node);
        node.expandTo();
        //updateRootNodes();
        //parent.updateChildren();

        /*
        if(parent == null){
            if(index > 0)prevNode = getRootNodes().get(index -1); // used to calc index correctly
            if(prevNode == node)return false; // inserting after itself
            node.remove(); // remove from old parent
            index = getRootNodes().indexOf(prevNode, true) + 1; // calc index anew
            insert(index, node);// insert as root

        }else {
            if(index > 0)prevNode = parent.getChildren().get(index -1); // used to calc index correctly
            node.remove(); // remove from old parent
            index = parent.getChildren().indexOf(prevNode, true) + 1; // calc index anew
            parent.insert(index, node);// insert as child of parent

        }
        node.expandTo();*/
        log.debug("inserting node");
        return true;
    }

    /**
     * @return Maybe null.
     */
    public @Null N getLastVisibleNode(){
        N lastRootNode = getRootNodes().peek(); // root nodes either empty but always present
        N lastVisible = lastRootNode;
        while(lastVisible != null && lastVisible.isExpanded() && lastVisible.hasChildren()){
            lastVisible = lastVisible.getChildren().peek();
        }
        return lastVisible;
    }

    /**
     * @param node
     * @param newParent if null inserts node as root node
     * @param prevNode if null just appends the parents' children with this node
     * @param add if true ignores the prevNode parameter and appends the parents' children with this node
     * @return true if node could get inserted
     */
    protected boolean insertDraggedNodeAt(N node, @Null N newParent, @Null N prevNode, boolean add) {
        if(node == null || node == newParent || node == prevNode ) {
            log.error("not inserting node");
            return false;
        }

        log.debug("inserting node");

        // add to parent node
        if(newParent != null) {
            node.remove();
            int index = add ? -1 : (prevNode == null ?
                    0 /*Math.max(0, newParent.getChildren().size - 1)*/: newParent.getChildren().indexOf(prevNode, true) + 1);
            if(index >= 0)newParent.insert(index, node);
            else newParent.add(node);
            node.expandTo();
        }else { // add to tree
            remove(node);
            int index = add ? -1 : (prevNode == null ?
                    0 /*Math.max(0,explorerTree.getRootNodes().size -1)*/ : getRootNodes().indexOf(prevNode, true) + 1);
            if(index >= 0)insert(index, node);
            else add(node);
        }
        return true;

    }

    // -- public --


    @Override
    public UITreeStyle getStyle() {
        return (UITreeStyle) super.getStyle();
    }

    /**
     * selects the node and expands the tree to it
     * */
    public void chooseNode(@Null N node) {
        if(node != null) {
            boolean nodeChanged = node != getSelectedNode();
            getSelection().choose(node);
            node.expandTo();

            if(nodeChanged)nodeSelected(node);

        }else getSelection().clear();
        lastOver = getOverNode();
    }

    @Override
    public void setIconSpacing(float left, float right) {
        this.iconSpacingLeft = left;
        this.iconSpacingRight = right;
        super.setIconSpacing(left, right);
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }


    // -- ui tree style --

    /**
     * is called when a node gets newly selected
     * @param selectedNode Maybe null
     */
    public void nodeSelected(@Null N selectedNode) {}

    public static class UITreeStyle extends TreeStyle{
        public @Null UIBase.UIStyle uiStyle;
        public final Color lineColor = new Color(1, 1, 1, 1);

        public UITreeStyle(){}

        public UITreeStyle(TreeStyle style){
            super(style);
            if(style instanceof UITreeStyle){
                uiStyle = ((UITreeStyle)style).uiStyle;
            }
        }

    }
}
