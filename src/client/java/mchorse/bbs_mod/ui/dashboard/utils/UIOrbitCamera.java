package mchorse.bbs_mod.ui.dashboard.utils;

import mchorse.bbs_mod.camera.OrbitCamera;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.utils.Area;

public class UIOrbitCamera implements IUIElement
{
    public OrbitCamera orbit = new OrbitCamera();
    private boolean control;
    private boolean enabled = true;

    public boolean canControl()
    {
        return this.control;
    }

    public void setControl(boolean control)
    {
        this.control = control;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean animate(UIContext context)
    {
        if (!this.control)
        {
            this.orbit.cache(context.mouseX, context.mouseY);

            return false;
        }

        boolean dragged = this.orbit.drag(context.mouseX, context.mouseY);
        boolean moved = this.orbit.update(context);

        return dragged || moved;
    }

    @Override
    public IUIElement mouseClicked(UIContext context)
    {
        int i = this.orbit.canStart(context);

        if (i >= 0)
        {
            this.orbit.start(i, context.mouseX, context.mouseY);

            return this;
        }

        return null;
    }

    @Override
    public IUIElement mouseScrolled(UIContext context)
    {
        if (!this.control)
        {
            return null;
        }

        return this.orbit.scroll((int) context.mouseWheel) ? this : null;
    }

    @Override
    public IUIElement mouseReleased(UIContext context)
    {
        this.orbit.release();

        return null;
    }

    @Override
    public void render(UIContext context)
    {
        this.animate(context);
    }

    /* Unimplemented GUI element methods */

    @Override
    public void resize()
    {}

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public boolean isVisible()
    {
        return true;
    }

    @Override
    public IUIElement keyPressed(UIContext context)
    {
        return this.control && this.orbit.keyPressed(context) ? this : null;
    }

    @Override
    public IUIElement textInput(UIContext context)
    {
        return null;
    }

    @Override
    public boolean canBeRendered(Area area)
    {
        return true;
    }
}
