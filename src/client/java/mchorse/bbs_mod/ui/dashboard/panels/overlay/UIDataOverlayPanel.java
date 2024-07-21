package mchorse.bbs_mod.ui.dashboard.panels.overlay;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.ValueGroup;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.io.File;
import java.util.function.Consumer;

public class UIDataOverlayPanel <T extends ValueGroup> extends UICRUDOverlayPanel
{
    protected UIDataDashboardPanel<T> panel;
    protected T transientCopy;

    public UIDataOverlayPanel(IKey title, UIDataDashboardPanel<T> panel, Consumer<String> callback)
    {
        super(title, callback);

        this.panel = panel;

        this.namesList.context((menu) ->
        {
            menu.action(Icons.FOLDER, UIKeys.PANELS_MODALS_ADD_FOLDER_TITLE, this::addNewFolder);

            if (this.panel.getData() != null)
            {
                menu.action(Icons.COPY, UIKeys.PANELS_CONTEXT_COPY, this::copy);
            }

            try
            {
                MapType data = Window.getClipboardMap("_ContentType_" + this.panel.getType().getId());

                if (data != null)
                {
                    menu.action(Icons.PASTE, UIKeys.PANELS_CONTEXT_PASTE, () -> this.paste(data));
                }
            }
            catch (Exception e)
            {}

            File folder = this.panel.getType().getRepository().getFolder();

            if (folder != null)
            {
                menu.action(Icons.FOLDER, UIKeys.PANELS_CONTEXT_OPEN, () ->
                {
                    UIUtils.openFolder(new File(folder, this.namesList.getPath().toString()));
                });
            }
        });
    }

    private void copy()
    {
        Window.setClipboard(this.panel.getData().toData().asMap(), "_ContentType_" + this.panel.getType().getId());
    }

    private void paste(MapType data)
    {
        this.transientCopy = (T) this.panel.getType().getRepository().create("", data);

        this.addNewData(this.add);
    }

    /* CRUD */

    @Override
    protected void addNewData(String name)
    {
        if (!this.namesList.hasInHierarchy(name))
        {
            this.panel.save();

            this.namesList.addFile(name);

            if (this.transientCopy == null)
            {
                this.transientCopy = (T) this.panel.getType().getRepository().create(name);

                this.fillDefaultData(this.transientCopy);
            }
            else
            {
                this.transientCopy.setId(name);
            }

            this.panel.fill(this.transientCopy);
        }

        this.transientCopy = null;
    }

    @Override
    protected void addNewFolder(String path)
    {
        this.panel.getType().getRepository().addFolder(path, (bool) ->
        {
            if (bool)
            {
                this.panel.requestNames();
            }
        });
    }

    protected void fillDefaultData(T data)
    {
        this.panel.fillDefaultData(data);
    }

    @Override
    protected void dupeData(String name)
    {
        if (this.panel.getData() != null && !this.namesList.hasInHierarchy(name))
        {
            this.panel.save();
            this.panel.getType().getRepository().save(name, this.panel.getData().toData().asMap());
            this.namesList.addFile(name);

            T data = (T) this.panel.getType().getRepository().create(name, this.panel.getData().toData().asMap());

            this.panel.fill(data);
        }
    }

    @Override
    protected void renameData(String name)
    {
        if (this.panel.getData() != null && !this.namesList.hasInHierarchy(name))
        {
            this.panel.getType().getRepository().rename(this.panel.getData().getId(), name);

            this.namesList.removeFile(this.panel.getData().getId());
            this.namesList.addFile(name);

            this.panel.getData().setId(name);
        }
    }

    @Override
    protected void renameFolder(String name)
    {
        String path = this.namesList.getCurrentFirst().toString();

        this.panel.getType().getRepository().renameFolder(path, name, (bool) ->
        {
            if (bool)
            {
                if (this.panel.getData() != null)
                {
                    String id = this.panel.getData().getId();

                    this.panel.getData().setId(name + "/" + id.substring(path.length()));
                }

                this.panel.requestNames();
            }
        });
    }

    @Override
    protected void removeData()
    {
        if (this.panel.getData() != null)
        {
            this.panel.getType().getRepository().delete(this.panel.getData().getId());

            this.namesList.removeFile(this.panel.getData().getId());
            this.panel.fill(null);
        }
    }

    @Override
    protected void removeFolder()
    {
        String path = this.namesList.getCurrentFirst().toString();

        this.panel.getType().getRepository().deleteFolder(path, (bool) ->
        {
            if (bool)
            {
                this.panel.requestNames();
            }
        });
    }
}