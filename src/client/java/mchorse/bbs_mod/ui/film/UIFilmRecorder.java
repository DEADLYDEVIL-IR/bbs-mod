package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.VideoRecorder;
import org.lwjgl.glfw.GLFW;

public class UIFilmRecorder extends UIElement
{
    public UIFilmPanel editor;

    private UIExit exit = new UIExit(this);
    private int end;

    public UIFilmRecorder(UIFilmPanel editor)
    {
        super();

        this.editor = editor;

        this.noCulling();
    }

    public boolean isRecording()
    {
        return getRecorder().isRecording();
    }

    private UIContext getUIContext()
    {
        return this.editor.getContext();
    }

    private VideoRecorder getRecorder()
    {
        return BBSModClient.getVideoRecorder();
    }

    private boolean isRunning()
    {
        return this.editor.isRunning();
    }

    public void openMovies()
    {
        UIUtils.openFolder(BBSRendering.getVideoFolder());
    }

    public void startRecording(int duration, Texture texture)
    {
        VideoRecorder recorder = this.getRecorder();
        UIContext context = this.getUIContext();

        if (this.isRunning() || recorder.isRecording() || duration <= 0)
        {
            return;
        }

        this.end = duration;

        try
        {
            recorder.startRecording(texture.id, texture.width, texture.height);
        }
        catch (Exception e)
        {
            UIOverlay.addOverlay(context, new UIMessageOverlayPanel(UIKeys.GENERAL_ERROR, IKey.raw(e.getMessage())));

            return;
        }

        this.editor.notifyServer(ActionState.RESTART);
        this.editor.getController().createEntities();
        this.editor.setCursor(0);
        this.editor.togglePlayback();
        context.menu.main.setEnabled(false);
        context.menu.overlay.add(this);
        context.menu.getRoot().add(this.exit);
    }

    public void stop()
    {
        UIContext context = this.getUIContext();

        context.render.postRunnable(this.exit::removeFromParent);

        if (this.getRecorder().isRecording())
        {
            try
            {
                this.getRecorder().stopRecording();
            }
            catch (Exception e) {}

            if (this.isRunning())
            {
                this.editor.togglePlayback();
            }

            context.menu.main.setEnabled(true);
            context.render.postRunnable(this::removeFromParent);
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        int ticks = this.editor.getCursor();

        if (!this.isRecording())
        {
            return;
        }

        if (!this.isRunning() || ticks >= this.end)
        {
            this.stop();
        }
    }

    public static class UIExit extends UIElement
    {
        private UIFilmRecorder recorder;

        public UIExit(UIFilmRecorder recorder)
        {
            this.recorder = recorder;
        }

        @Override
        protected boolean subKeyPressed(UIContext context)
        {
            if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
            {
                this.recorder.stop();

                return true;
            }

            return super.subKeyPressed(context);
        }
    }
}