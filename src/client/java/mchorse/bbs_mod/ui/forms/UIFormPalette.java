package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class UIFormPalette extends UIElement implements IUIFormList
{
    public UIFormList list;
    public UIFormEditor editor;

    public Consumer<Form> callback;

    private UIFormCategory lastSelected;
    private boolean cantExit;
    private boolean immersive;

    public static UIFormPalette open(UIElement parent, boolean editing, Form form, Consumer<Form> callback)
    {
        return open(parent, editing, form, false, callback);
    }

    public static UIFormPalette open(UIElement parent, boolean editing, Form form, boolean ignore, Consumer<Form> callback)
    {
        UIContext context = parent.getContext();

        if (!ignore)
        {
            if (!parent.getRoot().getChildren(UIFormPalette.class).isEmpty() || context == null)
            {
                return null;
            }
        }

        context.unfocus();

        UIFormPalette palette = new UIFormPalette(callback);

        palette.resetFlex().full(parent);
        palette.resize();

        parent.add(palette);

        palette.setSelected(form);
        palette.edit(editing);

        return palette;
    }

    public UIFormPalette(Consumer<Form> callback)
    {
        this.callback = callback;

        this.list = new UIFormList(this);
        this.list.full(this);

        this.editor = new UIFormEditor(this);
        this.editor.full(this);
        this.editor.setVisible(false);

        this.add(this.list, this.editor);

        this.eventPropagataion(EventPropagation.BLOCK_INSIDE).markContainer();
    }

    public void cantExit()
    {
        this.cantExit = true;

        this.list.close.removeFromParent();
        this.eventPropagataion(EventPropagation.PASS);
    }

    public boolean isImmersive()
    {
        return this.immersive;
    }

    public void immersive()
    {
        this.immersive = true;
    }

    public UIFormPalette updatable()
    {
        this.editor.renderer.updatable();

        return this;
    }

    public void edit(boolean editing)
    {
        if (editing != this.editor.isEditing())
        {
            this.toggleEditor();
        }
    }

    @Override
    public void exit()
    {
        if (!this.editor.isEditing())
        {
            if (!this.cantExit)
            {
                this.removeFromParent();
            }
        }
        else
        {
            this.toggleEditor();
        }
    }

    @Override
    public void toggleEditor()
    {
        this.events.emit(new UIToggleEditorEvent(this, !this.editor.isEditing()));

        if (!this.editor.isEditing())
        {
            Form form = this.list.getSelected();

            if (this.editor.edit(form))
            {
                this.lastSelected = this.list.getSelectedCategory();
            }
        }
        else
        {
            Form form = this.editor.finish();

            if (this.lastSelected.category.canModify(form))
            {
                int index = this.lastSelected.category.getForms().indexOf(this.lastSelected.selected);

                if (index >= 0)
                {
                    this.lastSelected.category.replaceForm(index, form);
                }
            }

            this.list.setSelected(form);
            this.accept(form);

            this.lastSelected = null;
        }

        this.list.setVisible(!this.editor.isEditing());
        this.editor.setVisible(this.editor.isEditing());
    }

    @Override
    public void accept(Form form)
    {
        if (this.callback != null)
        {
            this.callback.accept(form);
        }
    }

    public void setSelected(Form form)
    {
        this.list.setSelected(form);
    }

    @Override
    public boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.exit();

            return !this.cantExit;
        }

        return false;
    }

    @Override
    public void render(UIContext context)
    {
        if (!this.immersive || this.list.isVisible())
        {
            this.area.render(context.batcher, Colors.A75);
        }

        super.render(context);
    }
}