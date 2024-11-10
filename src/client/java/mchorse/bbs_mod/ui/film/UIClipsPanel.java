package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.ValueGroup;
import mchorse.bbs_mod.settings.values.ValueInt;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.factory.IFactory;
import mchorse.bbs_mod.utils.undo.IUndo;

import java.util.List;
import java.util.function.Consumer;

public class UIClipsPanel extends UIElement implements IUIClipsDelegate
{
    public UIClips clips;
    public UIFilmPanel filmPanel;

    private IFactory<Clip, ClipFactoryData> factory;
    private UIClip panel;

    private UIElement target;

    public UIClipsPanel(UIFilmPanel panel, IFactory<Clip, ClipFactoryData> factory)
    {
        this.filmPanel = panel;
        this.factory = factory;
        this.clips = new UIClips(this, factory);

        this.add(this.clips.full(this));
    }

    public UIClipsPanel target(UIElement target)
    {
        this.target = target;

        return this;
    }

    public void setClips(Clips clips)
    {
        this.clips.setClips(clips);
        this.clips.setVisible(clips != null);
    }

    public void handleUndo(IUndo<ValueGroup> undo, boolean redo)
    {
        if (this.panel != null)
        {
            this.panel.handleUndo(undo, redo);
        }
    }

    public void editClip(Position position)
    {
        if (this.panel != null)
        {
            this.panel.editClip(position);
        }
    }

    @Override
    public Film getFilm()
    {
        return this.filmPanel.getData();
    }

    @Override
    public Camera getCamera()
    {
        return this.filmPanel.getCamera();
    }

    @Override
    public Clip getClip()
    {
        return this.panel == null ? null : this.panel.clip;
    }

    @Override
    public void pickClip(Clip clip)
    {
        UIClip.saveScroll(this.panel);

        if (this.panel != null)
        {
            if (this.panel.clip == clip)
            {
                this.panel.fillData();

                return;
            }
            else
            {
                this.panel.removeFromParent();
            }
        }

        if (clip == null)
        {
            this.panel = null;

            this.clips.w(1F, 0);
            this.clips.clearSelection();
            this.resize();

            return;
        }

        try
        {
            this.clips.embedView(null);

            this.panel = UIClip.createPanel(clip, this);

            if (this.target == null)
            {
                this.panel.relative(this).x(1F, -160).w(160).h(1F);
            }
            else
            {
                this.panel.full(this.target);
            }

            this.add(this.panel);
            this.resize();
            this.panel.fillData();

            if (this.filmPanel.isFlying())
            {
                this.setCursor(clip.tick.get());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.clips.w(1F, this.target == null ? -160 : 0);
        this.resize();
    }

    @Override
    public int getCursor()
    {
        return this.filmPanel.getCursor();
    }

    @Override
    public void setCursor(int tick)
    {
        this.filmPanel.setCursor(tick);
    }

    @Override
    public boolean isRunning()
    {
        return this.filmPanel.isRunning();
    }

    @Override
    public void togglePlayback()
    {
        this.filmPanel.togglePlayback();
    }

    @Override
    public boolean canUseKeybinds()
    {
        return this.filmPanel.canUseKeybinds();
    }

    @Override
    public void fillData()
    {
        if (this.panel != null)
        {
            this.panel.fillData();
        }
    }

    @Override
    public void embedView(UIElement element)
    {
        this.clips.embedView(element);
    }

    @Override
    public void markLastUndoNoMerging()
    {
        this.filmPanel.getUndoHandler().markLastUndoNoMerging();
    }

    @Override
    public <T extends BaseValue> void editMultiple(T property, Consumer<T> consumer)
    {
        DataPath path = property.getRelativePath(this.getClip());

        for (Clip clip : this.clips.getClipsFromSelection())
        {
            BaseValue value = clip.getRecursively(path);

            if (value != null && value.getClass() == property.getClass())
            {
                consumer.accept((T) value);
            }
        }
    }

    @Override
    public void editMultiple(ValueInt property, int value)
    {
        int difference = value - property.get();
        List<Clip> clips = this.clips.getClipsFromSelection();

        for (Clip clip : clips)
        {
            ValueInt clipValue = (ValueInt) clip.get(property.getId());
            int newValue = clipValue.get() + difference;

            if (newValue < clipValue.getMin() || newValue > clipValue.getMax())
            {
                return;
            }
        }

        for (Clip clip : clips)
        {
            ValueInt clipValue = (ValueInt) clip.get(property.getId());

            clipValue.set(clipValue.get() + difference);
        }
    }
}