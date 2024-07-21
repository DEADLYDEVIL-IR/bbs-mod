package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.converters.IClipConverter;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.clips.renderer.IUIClipRenderer;
import mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderers;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.factory.IFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class UIClips extends UIElement
{
    /* Constants */
    public static final IKey KEYS_CATEGORY = UIKeys.CAMERA_EDITOR_KEYS_CLIPS_TITLE;

    private static final int MARGIN = 10;
    private static final int LAYER_HEIGHT = 20;
    private static final int LAYERS = 20;

    private static final Area CLIP_AREA = new Area();

    /* Main objects */
    private IUIClipsDelegate delegate;
    private Clips clips;
    private IFactory<Clip, ClipFactoryData> factory;

    /* Navigation */
    public Scale scale = new Scale(this.area, ScrollDirection.HORIZONTAL);
    public Scroll vertical = new Scroll(new Area());

    private boolean grabbing;
    private boolean scrubbing;
    private boolean scrolling;
    private int lastX;
    private int lastY;
    private int grabMode;

    /* Looping */
    public int loopMin = 0;
    public int loopMax = 0;
    private int selectingLoop = -1;

    /* Selection */
    private boolean selecting;
    private List<Integer> selection = new ArrayList<>();

    /* Embedded view */
    private UIIcon embeddedClose;
    private UIElement embedded;

    private Vector3i addPreview;

    private UIClipRenderers renderers = new UIClipRenderers();

    /**
     * Render cursor that displays the full duration of the camera work,
     * and also current tick within the camera work.
     */
    public static void renderCursor(UIContext context, String label, Area area, int x)
    {
        /* Draw the marker */
        FontRenderer font = context.batcher.getFont();
        int width = font.getWidth(label) + 3;

        context.batcher.box(x, area.y, x + 2, area.ey(), Colors.CURSOR);

        /* Move the tick line left, so it won't overflow the timeline */
        if (x + 2 + width > area.ex())
        {
            x -= width + 1;
        }

        /* Draw the tick label */
        context.batcher.textCard(label, x + 4, area.ey() - 2 - font.getHeight(), Colors.WHITE, Colors.setA(Colors.CURSOR, 0.75F), 2);
    }

    public UIClips(IUIClipsDelegate delegate, IFactory<Clip, ClipFactoryData> factory)
    {
        super();

        this.delegate = delegate;
        this.factory = factory;

        this.embeddedClose = new UIIcon(Icons.CLOSE, (b) -> this.embedView(null));
        this.embeddedClose.relative(this);

        this.context((menu) ->
        {
            UIContext context = this.getContext();
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            boolean hasSelected = this.delegate.getClip() != null;

            if (this.fromLayerY(mouseY) < 0)
            {
                return;
            }

            menu.action(Icons.ADD, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD, () -> this.showAdds(mouseX, mouseY));

            if (hasSelected)
            {
                menu.action(Icons.COPY, UIKeys.CAMERA_TIMELINE_CONTEXT_COPY, this::copyClips);
            }

            this.addPaste(menu, this.fromGraphX(mouseX));

            if (hasSelected)
            {
                this.addConverters(menu, context);
                menu.action(Icons.CUT, UIKeys.CAMERA_TIMELINE_CONTEXT_CUT, this::cut);
                menu.action(Icons.MOVE_TO, UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT, this::shiftToCursor);
                menu.action(Icons.SHIFT_TO, UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT_DURATION, this::shiftDurationToCursor);
            }

            menu.action(Icons.EXCHANGE, UIKeys.CAMERA_TIMELINE_CONTEXT_REORGANIZE, () -> this.clips.sortLayers());

            if (hasSelected)
            {
                menu.action(Icons.REMOVE, UIKeys.CAMERA_TIMELINE_CONTEXT_REMOVE_CLIPS, Colors.NEGATIVE, this::removeSelected);
            }
        });

        Supplier<Boolean> canUseKeybinds = () -> this.delegate.canUseKeybinds() && !this.hasEmbeddedView();
        Supplier<Boolean> canUseKeybindsSelected = () -> this.delegate.getClip() != null && canUseKeybinds.get();

        this.keys().register(Keys.DESELECT, () -> this.pickClip(null)).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
        this.keys().register(Keys.ADD_ON_TOP, this::showAddsOnTop).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
        this.keys().register(Keys.ADD_AT_CURSOR, this::showAddsAtCursor).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.ADD_AT_TICK, this::showAddsAtTick).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.COPY, this::copyClips).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
        this.keys().register(Keys.PASTE, () ->
        {
            MapType data = Window.getClipboardMap("_CopyClips");

            if (data != null)
            {
                this.paste(data, this.fromGraphX(this.getContext().mouseX));
            }
        }).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_CUT, this::cut).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_SHIFT, this::shiftToCursor).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_DURATION, this::shiftDurationToCursor).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.DELETE, this::removeSelected).label(UIKeys.CAMERA_TIMELINE_CONTEXT_REMOVE_CLIPS).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_ENABLE, this::toggleEnabled).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_SELECT_AFTER, this::selectAfter).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_SELECT_BEFORE, this::selectBefore).category(KEYS_CATEGORY).active(canUseKeybinds);
    }

    public IFactory<Clip, ClipFactoryData> getFactory()
    {
        return this.factory;
    }

    /* Tools */

    private void showAdds(int mouseX, int mouseY)
    {
        UIContext context = this.getContext();

        context.replaceContextMenu((add) ->
        {
            add.action(Icons.CURSOR, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_CURSOR, () -> this.showAddsAtCursor(context, mouseX, mouseY));
            add.action(Icons.SHIFT_TO, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_TICK, () -> this.showAddsAtTick(context, mouseX, mouseY));

            if (this.delegate.getClip() != null)
            {
                add.action(Icons.UPLOAD, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_ON_TOP, this::showAddsOnTop);
            }

            if (this.factory.getKeys().contains(Link.bbs("keyframe")))
            {
                add.action(Icons.EDITOR, UIKeys.CAMERA_TIMELINE_CONTEXT_FROM_PLAYER_RECORDING, () -> this.fromReplay(mouseX, mouseY));
            }
        });
    }

    private void showAddsAtCursor()
    {
        UIContext context = this.getContext();

        this.showAddsAtCursor(context, context.mouseX, context.mouseY);
    }

    private void showAddsAtCursor(UIContext context, int mouseX, int mouseY)
    {
        this.showAddClips(context, this.fromGraphX(mouseX), this.fromLayerY(mouseY), BBSSettings.getDefaultDuration());
    }

    private void showAddsAtTick()
    {
        UIContext context = this.getContext();

        this.showAddsAtTick(context, context.mouseX, context.mouseY);
    }

    private void showAddsAtTick(UIContext context, int mouseX, int mouseY)
    {
        this.showAddClips(context, this.delegate.getCursor(), this.fromLayerY(mouseY), BBSSettings.getDefaultDuration());
    }

    private void showAddsOnTop()
    {
        Clip clip = this.delegate.getClip();
        UIContext context = this.getContext();

        this.showAddClips(context, clip.tick.get(), clip.layer.get() + 1, clip.duration.get());
    }

    private void showAddClips(UIContext context, int tick, int layer, int duration)
    {
        context.replaceContextMenu((add) ->
        {
            IKey addCategory = UIKeys.CAMERA_TIMELINE_KEYS_CLIPS;
            int i = 0;

            for (Link type : this.factory.getKeys())
            {
                IKey typeKey = UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_CLIP_TYPE.format(UIKeys.C_CLIP.get(type));
                ClipFactoryData data = this.factory.getData(type);
                ContextAction action = add.action(data.icon, typeKey, data.color, () -> this.addClip(type, tick, layer, duration));

                if (i < 30)
                {
                    int mod = i % 10;
                    int key = i == 9 ? GLFW.GLFW_KEY_0 : GLFW.GLFW_KEY_1 + mod;

                    if (i >= 20)
                    {
                        action.key(addCategory, key, GLFW.GLFW_KEY_LEFT_CONTROL);
                    }
                    else if (i >= 10)
                    {
                        action.key(addCategory, key, GLFW.GLFW_KEY_LEFT_SHIFT);
                    }
                    else
                    {
                        action.key(addCategory, key);
                    }
                }

                i += 1;
            }

            add.onClose((m) -> this.addPreview = null);
        });

        this.addPreview = new Vector3i(tick, layer, duration);
    }

    private void addClip(Link type, int tick, int layer, int duration)
    {
        Clip clip = this.factory.create(type);

        if (clip instanceof CameraClip)
        {
            ((CameraClip) clip).fromCamera(this.delegate.getCamera());
        }

        this.addClip(clip, tick, layer, duration);
    }

    /**
     * Add a new clip of given type at mouse coordinates.
     */
    private void addClip(Clip clip, int tick, int layer, int duration)
    {
        clip.layer.set(layer);
        clip.tick.set(tick);
        clip.duration.set(duration);

        this.clips.addClip(clip);
        this.pickClip(clip);
    }

    private void copyClips()
    {
        MapType data = new MapType();
        ListType clips = new ListType();

        data.put("clips", clips);

        for (Clip clip : this.getClipsFromSelection())
        {
            clips.add(this.factory.toData(clip));
        }

        Window.setClipboard(data, "_CopyClips");
    }

    private void addPaste(ContextMenuManager menu, int tick)
    {
        MapType data = Window.getClipboardMap("_CopyClips");

        if (data != null)
        {
            menu.action(Icons.PASTE, UIKeys.CAMERA_TIMELINE_CONTEXT_PASTE, () -> this.paste(data, tick));
        }
    }

    /**
     * Paste given clip data to timeline.
     */
    private void paste(MapType data, int tick)
    {
        this.clearSelection();

        ListType clipsList = data.getList("clips");
        List<Clip> newClips = new ArrayList<>();
        int min = Integer.MAX_VALUE;

        for (BaseType type : clipsList)
        {
            MapType typeMap = type.asMap();
            Clip clip = this.factory.fromData(typeMap);

            min = Math.min(min, clip.tick.get());

            newClips.add(clip);
        }

        for (Clip clip : newClips)
        {
            clip.tick.set(tick + (clip.tick.get() - min));
            this.clips.addClip(clip);
            this.addSelected(clip);
        }

        this.pickLastSelectedClip();
    }

    /**
     * Breakdown currently selected clip into two.
     */
    private void cut()
    {
        List<Clip> selectedClips = this.isSelecting() ? this.getClipsFromSelection() : new ArrayList<>(this.clips.get());
        Clip original = this.delegate.getClip();
        int offset = this.delegate.getCursor();

        this.clips.preNotifyParent();

        for (Clip clip : selectedClips)
        {
            if (!clip.isInside(offset))
            {
                continue;
            }

            Clip copy = clip.breakDown(offset - clip.tick.get());

            if (copy != null)
            {
                clip.duration.set(clip.duration.get() - copy.duration.get());
                copy.tick.set(copy.tick.get() + clip.duration.get());
                this.clips.addClip(copy);
                this.addSelected(copy);
            }
        }

        this.clips.postNotifyParent();

        this.addSelected(original);
    }

    /**
     * Add available converters to context menu.
     */
    private void addConverters(ContextMenuManager menu, UIContext context)
    {
        ClipFactoryData data = this.factory.getData(this.delegate.getClip());
        Collection<Link> converters = data.converters.keySet();

        if (converters.isEmpty())
        {
            return;
        }

        menu.action(Icons.REFRESH, UIKeys.CAMERA_TIMELINE_CONTEXT_CONVERT, () ->
        {
            context.replaceContextMenu((add) ->
            {
                for (Link type : converters)
                {
                    IKey label = UIKeys.CAMERA_TIMELINE_CONTEXT_CONVERT_TO.format(UIKeys.C_CLIP.get(type));

                    add.action(Icons.REFRESH, label, this.factory.getData(type).color, () -> this.convertTo(type));
                }
            });
        });
    }

    /**
     * Convert currently editing camera clip into given type.
     */
    private void convertTo(Link type)
    {
        Clip original = this.delegate.getClip();
        ClipFactoryData data = this.factory.getData(original);
        IClipConverter converter = data.converters.get(type);
        Clip converted = converter.convert(original);

        if (converted == null)
        {
            return;
        }

        this.clips.remove(original);
        this.clips.addClip(converted);
        this.pickClip(converted);
    }

    private void fromReplay(int mouseX, int mouseY)
    {
        Film film = this.delegate.getFilm();

        this.getContext().replaceContextMenu((menu) ->
        {
            for (Replay replay : film.replays.getList())
            {
                Form form = replay.form.get();

                menu.action(Icons.EDITOR, IKey.raw(form == null ? "-" : form.getIdOrName()), () ->
                {
                    KeyframeClip clip = new KeyframeClip();

                    clip.fov.insert(0, 50D);

                    clip.x.copyKeyframes(replay.keyframes.x);
                    clip.y.copyKeyframes(replay.keyframes.y);
                    clip.z.copyKeyframes(replay.keyframes.z);

                    clip.yaw.copyKeyframes(replay.keyframes.yaw);
                    clip.pitch.copyKeyframes(replay.keyframes.pitch);

                    for (Keyframe<Double> keyframe : clip.yaw.getKeyframes())
                    {
                        keyframe.setValue(180D + keyframe.getValue());
                        // keyframe.setLy(180F + keyframe.getLy());
                        // keyframe.setRy(180F + keyframe.getRy());
                    }

                    int size = Math.max(
                        clip.x.getLength(),
                        Math.max(
                            clip.y.getLength(),
                            Math.max(
                                clip.z.getLength(),
                                Math.max(clip.yaw.getLength(), clip.pitch.getLength())
                            )
                        )
                    );

                    this.addClip(clip, this.fromGraphX(mouseX), this.fromLayerY(mouseY), size);
                });
            }
        });
    }

    /**
     * Move clips to cursor.
     */
    private void shiftToCursor()
    {
        List<Clip> clips = this.getClipsFromSelection();

        if (clips.isEmpty())
        {
            return;
        }

        int min = Integer.MAX_VALUE;

        for (Clip clip : clips)
        {
            min = Math.min(min, clip.tick.get());
        }

        int diff = this.delegate.getCursor() - min;

        for (Clip clip : clips)
        {
            clip.tick.set(clip.tick.get() + diff);
        }

        this.delegate.fillData();
    }

    /**
     * Move duration of currently selected clip(s) to cursor.
     */
    private void shiftDurationToCursor()
    {
        List<Clip> clips = this.getClipsFromSelection();

        if (clips.isEmpty())
        {
            return;
        }

        for (Clip clip : clips)
        {
            int offset = clip.tick.get();

            if (this.delegate.getCursor() > offset)
            {
                clip.duration.set(this.delegate.getCursor() - offset);
            }
            else if (this.delegate.getCursor() < offset + clip.duration.get())
            {
                clip.tick.set(this.delegate.getCursor());
                clip.duration.set(clip.duration.get() + offset - this.delegate.getCursor());
            }
        }

        this.delegate.fillData();
    }

    /**
     * Remove currently selected camera clip(s) from the camera work.
     */
    private void removeSelected()
    {
        List<Clip> selectedClips = this.getClipsFromSelection();

        if (selectedClips.isEmpty())
        {
            return;
        }

        for (Clip clip : selectedClips)
        {
            this.clips.remove(clip);
        }

        this.pickClip(null);
    }

    /**
     * Toggle enabled option of all selected clips
     */
    private void toggleEnabled()
    {
        List<Clip> clips = this.getClipsFromSelection();

        if (clips.isEmpty())
        {
            return;
        }

        for (Clip clip : clips)
        {
            clip.enabled.set(!clip.enabled.get());
        }

        this.delegate.fillData();
    }

    private void selectBefore()
    {
        int i = 0;

        this.clearSelection();

        for (Clip clip : this.clips.get())
        {
            if (clip.tick.get() < this.delegate.getCursor())
            {
                this.selection.add(i);
            }

            i += 1;
        }

        this.delegate.pickClip(this.selection.isEmpty() ? null : this.clips.get(this.selection.get(0)));
    }

    private void selectAfter()
    {
        int i = 0;

        this.clearSelection();

        for (Clip clip : this.clips.get())
        {
            if (clip.tick.get() + clip.duration.get() > this.delegate.getCursor())
            {
                this.selection.add(i);
            }

            i += 1;
        }

        this.delegate.pickClip(this.selection.isEmpty() ? null : this.clips.get(this.selection.get(0)));
    }

    /* Selection */

    private boolean isSelecting()
    {
        return !this.selection.isEmpty();
    }

    public List<Integer> getSelection()
    {
        return Collections.unmodifiableList(this.selection);
    }

    public List<Clip> getClipsFromSelection()
    {
        List<Clip> clips = new ArrayList<>();

        for (int index : this.selection)
        {
            Clip clip = this.clips.get(index);

            if (clip != null)
            {
                clips.add(clip);
            }
        }

        return clips;
    }

    public Clip getLastSelectedClip()
    {
        if (!this.isSelecting())
        {
            return null;
        }

        return this.clips.get(this.selection.get(this.selection.size() - 1));
    }

    public void setSelection(List<Integer> selection)
    {
        this.clearSelection();
        this.selection.addAll(selection);
    }

    public void clearSelection()
    {
        this.selection.clear();
    }

    private void pickClip(Clip clip)
    {
        this.setSelected(clip);
        this.delegate.pickClip(clip);
    }

    private void pickLastSelectedClip()
    {
        this.delegate.pickClip(this.getLastSelectedClip());
    }

    public void setSelected(Clip clip)
    {
        this.clearSelection();
        this.addSelected(clip);
    }

    public void addSelected(Clip clip)
    {
        int index = this.clips.getIndex(clip);

        if (index >= 0)
        {
            this.selection.remove((Integer) index);
            this.selection.add(index);
        }
    }

    public boolean hasSelected(int clip)
    {
        return this.selection.contains(clip);
    }

    /* Getters and setters */

    public void setClips(Clips clips)
    {
        this.clips = clips;
        this.addPreview = null;

        this.scale.anchor(0F);

        this.updateScrollSize();
        this.vertical.scrollToEnd();
        this.clearSelection();
        this.embedView(null);

        if (clips != null)
        {
            int duration = clips.calculateDuration();

            if (duration > 0)
            {
                this.scale.view(0, duration);
            }
            else
            {
                this.scale.set(0, 1);
            }
        }
    }

    public int fromLayerY(int mouseY)
    {
        int bottom = this.area.ey() - MARGIN;

        if (mouseY > bottom)
        {
            return -1;
        }

        mouseY -= this.getScroll();

        return (bottom - mouseY) / LAYER_HEIGHT;
    }

    public int toLayerY(int layer)
    {
        int h = LAYER_HEIGHT;

        return this.area.ey() - MARGIN - (layer + 1) * h + this.getScroll();
    }

    private int getScroll()
    {
        if (this.vertical.scrollSize < this.vertical.area.h)
        {
            return 0;
        }

        return this.vertical.scrollSize - this.vertical.area.h - (int) this.vertical.scroll;
    }

    public int fromGraphX(int mouseX)
    {
        return (int) this.scale.from(mouseX);
    }

    public int toGraphX(int value)
    {
        return (int) (this.scale.to(value));
    }

    public void setLoopMin()
    {
        this.loopMin = this.delegate.getCursor();
    }

    public void setLoopMax()
    {
        this.loopMax = this.delegate.getCursor();
    }

    private void verifyLoopMinMax()
    {
        int min = this.loopMin;
        int max = this.loopMax;

        this.loopMin = Math.min(min, max);
        this.loopMax = Math.max(min, max);
    }

    /* Embedded view */

    public boolean hasEmbeddedView()
    {
        return this.embedded != null;
    }

    public void embedView(UIElement element)
    {
        this.embeddedClose.removeFromParent();

        if (this.embedded != null)
        {
            this.embedded.removeFromParent();
        }

        this.embedded = element;

        if (this.embedded != null)
        {
            this.embedded.resetFlex().full(this);

            this.prepend(this.embedded);
            this.add(this.embeddedClose);
            this.embedded.resize();
            this.embeddedClose.resize();
        }
    }

    /* Handling user input */

    @Override
    protected void afterResizeApplied()
    {
        super.afterResizeApplied();

        this.vertical.area.copy(this.area);
        this.vertical.area.h -= MARGIN;

        this.updateScrollSize();
    }

    private void updateScrollSize()
    {
        this.vertical.scrollSize = this.clips == null ? 0 : LAYERS * LAYER_HEIGHT;
        this.vertical.clamp();
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.vertical.mouseClicked(context))
        {
            return true;
        }

        if (this.area.isInside(context) && !this.hasEmbeddedView())
        {
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            boolean ctrl = Window.isCtrlPressed();
            boolean shift = Window.isShiftPressed();
            boolean alt = Window.isAltPressed();

            if (context.mouseButton == 0 && this.handleLeftClick(context, mouseX, mouseY, ctrl, shift, alt))
            {
                return true;
            }
            else if (context.mouseButton == 1 && this.handleRightClick(mouseX, mouseY, ctrl, shift, alt))
            {
                return true;
            }
            else if (context.mouseButton == 2 && this.handleMiddleClick(mouseX, mouseY, ctrl, shift, alt))
            {
                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    private boolean handleLeftClick(UIContext context, int mouseX, int mouseY, boolean ctrl, boolean shift, boolean alt)
    {
        if (!this.hasEmbeddedView())
        {
            int tick = this.fromGraphX(mouseX);
            int layerIndex = this.fromLayerY(mouseY);
            Clip original = this.delegate.getClip();
            Clip clip = this.clips.getClipAt(tick, layerIndex);

            if (clip != null)
            {
                if (clip != original)
                {
                    if (shift || this.selection.contains(this.clips.getIndex(clip)))
                    {
                        this.addSelected(clip);

                        Clip last = this.getLastSelectedClip();

                        if (last != original)
                        {
                            this.delegate.pickClip(last);
                        }
                    }
                    else
                    {
                        this.delegate.pickClip(clip);
                        this.setSelected(clip);
                    }
                }

                this.grabMode = this.getClipHandle(clip, context, LAYER_HEIGHT);
                this.grabbing = true;
                this.lastX = mouseX;
                this.lastY = mouseY;

                return true;
            }
        }

        if (shift && !this.hasEmbeddedView())
        {
            this.selecting = true;
            this.lastX = mouseX;
            this.lastY = mouseY;

            return true;
        }
        else if (alt)
        {
            this.selectingLoop = 0;
            this.loopMin = this.fromGraphX(mouseX);
            this.verifyLoopMinMax();
        }
        else
        {
            this.scrubbing = true;
            this.delegate.setCursor(this.fromGraphX(mouseX));

            return true;
        }

        return false;
    }

    private boolean handleRightClick(int mouseX, int mouseY, boolean ctrl, boolean shift, boolean alt)
    {
        if (alt)
        {
            boolean same = this.loopMin == this.loopMax;

            this.selectingLoop = 1;
            this.loopMax = this.fromGraphX(mouseX);

            if (same)
            {
                this.loopMin = this.loopMax;
            }
            else
            {
                this.verifyLoopMinMax();
            }

            return true;
        }

        return false;
    }

    private boolean handleMiddleClick(int mouseX, int mouseY, boolean ctrl, boolean shift, boolean alt)
    {
        if (alt)
        {
            this.loopMin = this.loopMax = 0;
        }
        else
        {
            this.scrolling = true;
            this.lastX = mouseX;
            this.lastY = mouseY;

            return true;
        }

        return false;
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        if (this.area.isInside(context) && !this.scrolling && !this.hasEmbeddedView())
        {
            if (Window.isShiftPressed())
            {
                this.vertical.mouseScroll(context);
            }
            else
            {
                this.scale.zoomAnchor(Scale.getAnchorX(context, this.area), Math.copySign(this.scale.getZoomFactor(), context.mouseWheel));
            }

            return true;
        }

        return super.subMouseScrolled(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.hasEmbeddedView())
        {
            return super.subMouseReleased(context);
        }

        this.vertical.mouseReleased(context);

        if (this.selecting)
        {
            this.pickLastSelectedClip();
        }

        if (this.grabbing)
        {
            this.delegate.markLastUndoNoMerging();
        }

        this.grabMode = 0;
        this.grabbing = false;
        this.selecting = false;
        this.scrubbing = false;
        this.scrolling = false;
        this.selectingLoop = -1;

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.clips != null && !this.hasEmbeddedView())
        {
            this.vertical.drag(context);
            this.handleInput(context.mouseX, context.mouseY);
            this.handleScrolling(context.mouseX, context.mouseY);
            this.renderCameraWork(context);
        }

        super.render(context);
    }

    private void handleInput(int mouseX, int mouseY)
    {
        if (this.scrubbing)
        {
            this.delegate.setCursor(this.fromGraphX(mouseX));
        }
        else if (this.selectingLoop == 0)
        {
            this.loopMin = MathUtils.clamp(this.fromGraphX(mouseX), 0, this.loopMax);
        }
        else if (this.selectingLoop == 1)
        {
            this.loopMax = MathUtils.clamp(this.fromGraphX(mouseX), this.loopMin, Integer.MAX_VALUE);
        }
        else if (this.selecting)
        {
            Area selection = new Area();

            selection.setPoints(this.lastX, this.lastY, mouseX, mouseY);
            this.captureSelection(selection);
        }
        else if (this.grabbing)
        {
            List<Clip> clips = this.getClipsFromSelection();
            int relativeX = this.fromGraphX(mouseX) - this.fromGraphX(this.lastX);
            int relativeY = this.fromLayerY(mouseY) - this.fromLayerY(this.lastY);

            /* Collect the rest of clips for collision */
            if (this.grabMode == 0)
            {
                List<Clip> others = Window.isAltPressed() ? new ArrayList<>() : new ArrayList<>(this.clips.get());
                Iterator<Clip> it = others.iterator();

                while (it.hasNext())
                {
                    if (clips.contains(it.next()))
                    {
                        it.remove();
                    }
                }

                /* Checking whether it's possible to move clips */
                for (Clip clip : clips)
                {
                    int newTick = clip.tick.get() + relativeX;
                    int newLayer = clip.layer.get() + relativeY;

                    /* Detect clips collisions */
                    for (Clip other : others)
                    {
                        int otherTick = other.tick.get();
                        int otherRight = otherTick + other.duration.get();

                        int newRight = newTick + clip.duration.get();
                        boolean intersect = MathUtils.isInside(newTick, newRight, otherTick, otherRight);

                        if (intersect && other.layer.get() == newLayer) relativeX = relativeY = 0;
                    }

                    if (newTick < 0) relativeX = 0;
                    if (newLayer < 0 || newLayer >= LAYERS) relativeY = 0;
                }
            }

            for (Clip clip : clips)
            {
                /* Move clips */
                if (this.grabMode == 0)
                {
                    int newTick = clip.tick.get() + relativeX;
                    int newLayer = clip.layer.get() + relativeY;

                    clip.tick.set(newTick);
                    clip.layer.set(newLayer);
                }
                else if (this.grabMode == 1 && clip.duration.get() - relativeX > 0)
                {
                    if (clip.tick.get() + relativeX < 0)
                    {
                        relativeX -= clip.tick.get() + relativeX;
                    }

                    clip.tick.set(clip.tick.get() + relativeX);
                    clip.duration.set(clip.duration.get() - relativeX);
                }
                else if (this.grabMode == 2)
                {
                    clip.duration.set(Math.max(clip.duration.get() + relativeX, 1));
                }
            }

            this.delegate.fillData();

            this.lastX = mouseX;
            this.lastY = mouseY;
        }
    }

    private void captureSelection(Area area)
    {
        this.clearSelection();

        for (Clip clip : this.clips.get())
        {
            Area clipArea = new Area();

            int x = this.toGraphX(clip.tick.get());
            int y = this.toLayerY(clip.layer.get());

            clipArea.set(x, y, this.toGraphX(clip.tick.get() + clip.duration.get()) - x, LAYER_HEIGHT);

            if (area.intersects(clipArea))
            {
                this.addSelected(clip);
            }
        }
    }

    private void handleScrolling(int mouseX, int mouseY)
    {
        if (this.scrolling)
        {
            this.scale.setShift(this.scale.getShift() - (mouseX - this.lastX) / this.scale.getZoom());
            this.vertical.scrollBy(this.lastY - mouseY);
            this.vertical.clamp();

            this.lastX = mouseX;
            this.lastY = mouseY;

            this.scale.setShift(this.scale.getShift());
            this.scale.calculateMultiplier();
        }
    }

    /**
     * Render camera work (layers, clips, envelope previews, looping region, cursor, etc.)
     */
    private void renderCameraWork(UIContext context)
    {
        Batcher2D batcher = context.batcher;
        Area area = this.area;
        int h = LAYER_HEIGHT;
        int leftEdge = this.toGraphX(0);

        if (leftEdge > this.area.x)
        {
            batcher.box(this.area.x, this.area.y, Math.min(leftEdge, this.area.ex()), this.area.ey(), Colors.A75);
        }

        area.render(batcher, Colors.A50);
        batcher.clip(this.vertical.area, context);

        for (int i = 0; i < LAYERS; i++)
        {
            int ly = this.toLayerY(i);

            if (i % 2 != 0)
            {
                batcher.box(leftEdge, ly, this.area.ex(), ly + h, Colors.A50);
            }
        }

        batcher.unclip(context);
        batcher.clip(this.area, context);

        this.renderTickMarkers(context, area.y, area.h);

        batcher.unclip(context);
        batcher.clip(this.vertical.area, context);

        List<Clip> clips = this.clips.get();

        for (int i = 0, c = clips.size(); i < c; i++)
        {
            Clip clip = clips.get(i);
            IUIClipRenderer renderer = this.renderers.get(clip);

            Area clipArea = this.getClipArea(clip, CLIP_AREA, h);
            boolean selected = this.hasSelected(i);

            if (!this.hasEmbeddedView())
            {
                clipArea.y += 1;
                clipArea.h -= 2;
            }

            renderer.renderClip(context, this, clip, clipArea, selected, this.delegate.getClip() == clip);

            int clipHandle = this.getClipHandle(clip, context, h);
            int color = this.grabMode != 0 ? Colors.WHITE : Colors.A50;

            if (clipHandle == 1 || (selected && this.grabMode == 1))
            {
                context.batcher.icon(Icons.CLIP_HANLDE_LEFT, color, clipArea.x, clipArea.y + 10, 0F, 0.5F);
            }
            else if (clipHandle == 2 || (selected && this.grabMode == 2))
            {
                context.batcher.icon(Icons.CLIP_HANLDE_RIGHT, color, clipArea.ex(), clipArea.y + 10, 1F, 0.5F);
            }
        }

        this.renderAddPreview(context, h);
        this.renderLoopingRegion(context, area.y);

        batcher.unclip(context);
        batcher.clip(this.area, context);

        String label = TimeUtils.formatTime(this.delegate.getCursor()) + "/" + TimeUtils.formatTime(this.clips.calculateDuration());

        renderCursor(context, label, area, this.toGraphX(this.delegate.getCursor()));
        this.renderSelection(context);

        batcher.unclip(context);
        batcher.clip(this.vertical.area, context);

        this.vertical.renderScrollbar(batcher);

        batcher.unclip(context);
    }

    private Area getClipArea(Clip clip, Area area, int h)
    {
        int tick = clip.tick.get();
        int x = this.toGraphX(tick);
        int y = this.toLayerY(clip.layer.get());
        int w = this.toGraphX(tick + clip.duration.get()) - x;

        area.set(x, y, w, h);

        return area;
    }

    private int getClipHandle(Clip clip, UIContext context, int h)
    {
        Area clipArea = this.getClipArea(clip, CLIP_AREA, h);
        int separation = Math.min(clipArea.w / 2, 10);

        if (clipArea.isInside(context))
        {
            if (Window.isCtrlPressed())
            {
                return 0;
            }

            if (context.mouseX - clipArea.x < separation)
            {
                return 1;
            }
            else if (context.mouseX - clipArea.ex() >= -separation)
            {
                return 2;
            }

            return 0;
        }

        return -1;
    }

    private void renderAddPreview(UIContext context, int h)
    {
        if (this.addPreview == null)
        {
            return;
        }

        int x = this.toGraphX(this.addPreview.x);
        int y = this.toLayerY(this.addPreview.y);
        int d = this.toGraphX(this.addPreview.x + this.addPreview.z);

        context.batcher.outline(x, y, d, y + h, Colors.WHITE);
    }

    /**
     * Render tick markers that help orient within camera work.
     */
    private void renderTickMarkers(UIContext context, int y, int h)
    {
        int mult = this.scale.getMult() * 2;
        int start = (int) this.scale.getMinValue();
        int end = (int) this.scale.getMaxValue();
        int max = Integer.MAX_VALUE;

        start -= start % mult;
        end -= end % mult;

        start = MathUtils.clamp(start, 0, max);
        end = MathUtils.clamp(end, mult, max);

        for (int j = start; j <= end; j += mult)
        {
            int xx = this.toGraphX(j);
            String value = TimeUtils.formatTime(j);

            context.batcher.box(xx, y, xx + 1, y + h, Colors.setA(Colors.WHITE, 0.2F));
            context.batcher.textShadow(value, xx + 3, this.area.y + 4, Colors.WHITE);
        }
    }

    /**
     * Render selection box.
     */
    private void renderSelection(UIContext context)
    {
        if (this.selecting)
        {
            context.batcher.normalizedBox(this.lastX, this.lastY, context.mouseX, context.mouseY, Colors.setA(Colors.ACTIVE, 0.25F));
        }
    }

    /**
     * Render looping region
     */
    private void renderLoopingRegion(UIContext context, int y)
    {
        if (this.loopMin == this.loopMax)
        {
            return;
        }

        int min = Math.min(this.loopMin, this.loopMax);
        int max = Math.max(this.loopMin, this.loopMax);

        int minX = this.toGraphX(min);
        int maxX = this.toGraphX(max);

        if (maxX >= this.area.x + 1 && minX < this.area.ex() - 1)
        {
            minX = MathUtils.clamp(minX, this.area.x + 1, this.area.ex() - 1);
            maxX = MathUtils.clamp(maxX, this.area.x + 1, this.area.ex() - 1);

            float alpha = BBSSettings.editorLoop.get() ? 1 : 0.4F;
            int color = Colors.mulRGB(0xff88ffff, alpha);

            context.batcher.gradientVBox(minX, y, maxX, this.area.ey(), Colors.mulRGB(0x0000ffff, alpha), Colors.mulRGB(0xaa0088ff, alpha));
            context.batcher.box(minX, y, minX + 1, this.area.ey(), color);
            context.batcher.box(maxX - 1, y, maxX, this.area.ey(), color);
        }
    }
}