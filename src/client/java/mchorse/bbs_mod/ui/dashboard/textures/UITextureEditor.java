package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageFolderOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PNGEncoder;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;

import java.io.File;

public class UITextureEditor extends UIPixelsEditor
{
    public UIElement savebar;
    public UIIcon save;
    public UIIcon resize;

    private Link texture;
    private boolean dirty;

    public UITextureEditor()
    {
        super();

        this.savebar = new UIElement();
        this.savebar.relative(this).x(1F).h(30).anchorX(1F).row(0).resize().padding(5);
        this.save = new UIIcon(() -> this.dirty ? Icons.SAVE : Icons.SAVED, (b) -> this.saveTexture());
        this.resize = new UIIcon(Icons.FULLSCREEN, (b) ->
        {
            Pixels pixels = this.getPixels();
            UIResizeTextureOverlayPanel overlayPanel = new UIResizeTextureOverlayPanel(pixels.width, pixels.height, (size) ->
            {
                boolean editing = this.isEditing();
                Pixels newPixels = Pixels.fromSize(
                    MathUtils.clamp(size.x, 1, 4096),
                    MathUtils.clamp(size.y, 1, 4096)
                );

                newPixels.draw(pixels, 0, 0, newPixels.width, newPixels.height);
                pixels.delete();

                this.fillPixels(newPixels);
                this.setDirty(false);
                this.setEditing(editing);
            });

            UIOverlay.addOverlay(this.getContext(), overlayPanel);
        });
        this.resize.tooltip(UIKeys.TEXTURES_RESIZE);

        this.savebar.add(this.save);
        this.toolbar.add(this.resize);

        this.add(this.savebar);
    }

    public Link getTexture()
    {
        return this.texture;
    }

    public boolean isDirty()
    {
        return this.dirty;
    }

    public void dirty()
    {
        this.setDirty(true);
    }

    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
    }

    @Override
    protected void wasChanged()
    {
        this.dirty();
    }

    private void saveTexture()
    {
        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
            UIKeys.GENERAL_EXPORT,
            UIKeys.TEXTURES_SAVE,
            this::saveTexture
        );

        String text = this.texture.toString();
        int index = text.lastIndexOf('.');

        panel.text.setText(text);

        UIOverlay.addOverlay(this.getContext(), panel);

        if (index >= 0)
        {
            int path = text.lastIndexOf('/');

            panel.text.textbox.moveCursorTo(index);
            panel.text.textbox.setSelection(path >= 0 ? path + 1 : 0);
        }
    }

    private void saveTexture(String path)
    {
        Link link = Link.create(path);

        if (!link.source.equals("assets") || !link.path.endsWith(".png"))
        {
            this.getContext().notify(UIKeys.TEXTURES_SAVE_WRONG_PATH, Colors.RED);

            return;
        }

        File file = BBSMod.getAssetsPath(link.path);

        if (path.contains("/"))
        {
            file.getParentFile().mkdirs();
        }

        Pixels pixels = this.getPixels();

        try
        {
            PNGEncoder.writeToFile(pixels, file);
            UIMessageFolderOverlayPanel panel = new UIMessageFolderOverlayPanel(
                UIKeys.TEXTURES_EXPORT_OVERLAY_TITLE,
                UIKeys.TEXTURES_EXPORT_OVERLAY_SUCCESS.format(file.getName()),
                file.getParentFile()
            );

            panel.folder.tooltip(UIKeys.TEXTURES_EXPORT_OVERLAY_OPEN_FOLDER, Direction.LEFT);

            UIOverlay.addOverlay(this.getContext(), panel);

            this.setDirty(false);
        }
        catch (Exception e)
        {
            e.printStackTrace();

            this.getContext().notify(UIKeys.TEXTURES_EXPORT_OVERLAY_ERROR.format(file.getName()), Colors.RED);
        }
    }

    public void fillTexture(Link texture)
    {
        if (this.getPixels() != null)
        {
            this.getPixels().delete();
        }

        this.texture = texture;

        if (texture != null)
        {
            Texture t = BBSModClient.getTextures().getTexture(texture);

            this.fillPixels(Texture.pixelsFromTexture(t));
            this.setDirty(false);
        }
    }

    @Override
    protected Texture getRenderTexture(UIContext context)
    {
        return this.isEditing() ? super.getRenderTexture(context) : context.render.getTextures().getTexture(this.texture);
    }
}