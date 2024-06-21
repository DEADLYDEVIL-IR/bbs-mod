package mchorse.bbs_mod.ui.film;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.VoiceLines;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.IFlightSupported;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UICRUDOverlayPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.screenplay.UIScreenplayEditor;
import mchorse.bbs_mod.ui.film.utils.UIFilmUndoHandler;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.Map;
import java.util.function.Supplier;

public class UIFilmPanel extends UIDataDashboardPanel<Film> implements IFlightSupported
{
    private static VoiceLines voiceLines = new VoiceLines(null);

    private RunnerCameraController runner;
    private boolean lastRunning;
    private final Position position = new Position(0, 0, 0, 0, 0);
    private final Position lastPosition = new Position(0, 0, 0, 0, 0);

    public UIElement main;
    public UIElement editArea;
    public UIDraggable draggableMain;
    public UIDraggable draggableEditor;
    public UIFilmRecorder recorder;
    public UIFilmPreview preview;

    public UIIcon duplicateFilm;

    /* Main editors */
    public UIClipsPanel cameraEditor;
    public UIReplaysEditor replayEditor;
    public UIScreenplayEditor screenplayEditor;

    /* Icon bar buttons */
    public UIIcon openReplays;
    public UIIcon toggleHorizontal;
    public UIIcon openCameraEditor;
    public UIIcon openReplayEditor;
    public UIIcon openScreenplay;

    private Camera camera = new Camera();
    private boolean entered;

    /* Toggle viewport mode */
    private boolean horizontal;
    private float mainSizeH = 0.66F;
    private float editorSizeH = 0.5F;
    private float mainSizeV = 0.66F;
    private float editorSizeV = 0.5F;

    /* Entity control */
    private UIFilmController controller = new UIFilmController(this);
    private UIFilmUndoHandler undoHandler;

    public final Matrix4f lastView = new Matrix4f();
    public final Matrix4f lastProjection = new Matrix4f();

    private Timer flightEditTime = new Timer(100);
    private Recorder lastRecorder;

    public static VoiceLines getVoiceLines()
    {
        return voiceLines;
    }

    /**
     * Initialize the camera editor with a camera profile.
     */
    public UIFilmPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.runner = new RunnerCameraController(this);
        this.recorder = new UIFilmRecorder(this);

        this.main = new UIElement();
        this.editArea = new UIElement();
        this.preview = new UIFilmPreview(this);

        this.draggableMain = new UIDraggable((context) ->
        {
            if (this.horizontal)
            {
                this.mainSizeH = 1F - (context.mouseY - this.editor.area.y) / (float) this.editor.area.h;
            }
            else
            {
                this.mainSizeV = (context.mouseX - this.editor.area.x) / (float) this.editor.area.w;
            }

            this.setupEditorFlex(true);
        });

        this.draggableEditor = new UIDraggable((context) ->
        {
            if (this.horizontal)
            {
                this.editorSizeH = 1F - (context.mouseX - this.editor.area.x) / (float) this.editor.area.w;
            }
            else
            {
                this.editorSizeV = (context.mouseY - this.editor.area.y) / (float) this.editor.area.h;
            }

            this.setupEditorFlex(true);
        });

        /* Editors */
        this.cameraEditor = new UIClipsPanel(this, BBSMod.getFactoryCameraClips()).target(this.editArea);
        this.cameraEditor.full(this.main);
        this.replayEditor = new UIReplaysEditor(this);
        this.replayEditor.full(this.main).setVisible(false);
        this.screenplayEditor = new UIScreenplayEditor(this);
        this.screenplayEditor.full(this.main).setVisible(false);

        /* Icon bar buttons */
        this.openReplays = new UIIcon(Icons.EDITOR, (b) -> UIOverlay.addOverlayLeft(this.getContext(), this.replayEditor.replays, 200));
        this.openReplays.tooltip(UIKeys.FILM_REPLAY_TITLE, Direction.LEFT);
        this.toggleHorizontal = new UIIcon(() -> this.horizontal ? Icons.EXCHANGE : Icons.CONVERT, (b) ->
        {
            this.horizontal = !this.horizontal;

            this.setupEditorFlex(true);
        });
        this.toggleHorizontal.tooltip(UIKeys.FILM_TOGGLE_LAYOUT, Direction.LEFT);
        this.openCameraEditor = new UIIcon(Icons.FRUSTUM, (b) -> this.showPanel(this.cameraEditor));
        this.openCameraEditor.tooltip(UIKeys.FILM_OPEN_CAMERA_EDITOR);
        this.openReplayEditor = new UIIcon(Icons.SCENE, (b) -> this.showPanel(this.replayEditor));
        this.openReplayEditor.tooltip(UIKeys.FILM_OPEN_REPLAY_EDITOR);
        this.openScreenplay = new UIIcon(Icons.FILE, (b) -> this.showPanel(this.screenplayEditor));
        this.openScreenplay.tooltip(UIKeys.FILM_OPEN_VOICE_LINE_EDITOR);

        /* Setup elements */
        this.iconBar.add(this.openReplays.marginTop(9), this.toggleHorizontal, this.openCameraEditor.marginTop(9), this.openReplayEditor, this.openScreenplay);

        this.editor.add(this.main, new UIRenderable(this::renderIcons));
        this.main.add(this.cameraEditor, this.replayEditor, this.screenplayEditor, this.editArea, this.preview, this.draggableMain, this.draggableEditor);
        this.add(this.controller, new UIRenderable(this::renderDividers));
        this.overlay.namesList.setFileIcon(Icons.FILM);

        /* Register keybinds */
        IKey modes = UIKeys.CAMERA_EDITOR_KEYS_MODES_TITLE;
        IKey editor = UIKeys.CAMERA_EDITOR_KEYS_EDITOR_TITLE;
        IKey looping = UIKeys.CAMERA_EDITOR_KEYS_LOOPING_TITLE;
        Supplier<Boolean> active = () -> !this.isFlying();

        this.keys().register(Keys.PLAUSE, () -> this.preview.plause.clickItself()).active(active).category(editor);
        this.keys().register(Keys.NEXT_CLIP, () -> this.setCursor(this.data.camera.findNextTick(this.getCursor()))).active(active).category(editor);
        this.keys().register(Keys.PREV_CLIP, () -> this.setCursor(this.data.camera.findPreviousTick(this.getCursor()))).active(active).category(editor);
        this.keys().register(Keys.NEXT, () -> this.setCursor(this.getCursor() + 1)).active(active).category(editor);
        this.keys().register(Keys.PREV, () -> this.setCursor(this.getCursor() - 1)).active(active).category(editor);
        this.keys().register(Keys.UNDO, this::undo).category(editor);
        this.keys().register(Keys.REDO, this::redo).category(editor);
        this.keys().register(Keys.FLIGHT, this::toggleFlight).active(() -> this.data != null).category(modes);
        this.keys().register(Keys.LOOPING, () -> BBSSettings.editorLoop.set(!BBSSettings.editorLoop.get())).active(active).category(looping);
        this.keys().register(Keys.LOOPING_SET_MIN, () -> this.cameraEditor.clips.setLoopMin()).active(active).category(looping);
        this.keys().register(Keys.LOOPING_SET_MAX, () -> this.cameraEditor.clips.setLoopMax()).active(active).category(looping);
        this.keys().register(Keys.JUMP_FORWARD, () -> this.setCursor(this.getCursor() + BBSSettings.editorJump.get())).active(active).category(editor);
        this.keys().register(Keys.JUMP_BACKWARD, () -> this.setCursor(this.getCursor() - BBSSettings.editorJump.get())).active(active).category(editor);

        this.keys().register(Keys.FILM_CONTROLLER_START_RECORDING_OUTSIDE, () -> this.controller.recordOutside());

        this.fill(null);

        this.setupEditorFlex(false);
        this.flightEditTime.mark();
    }

    private void setupEditorFlex(boolean resize)
    {
        this.mainSizeH = MathUtils.clamp(this.mainSizeH, 0.05F, 0.95F);
        this.editorSizeH = MathUtils.clamp(this.editorSizeH, 0.05F, 0.95F);
        this.mainSizeV = MathUtils.clamp(this.mainSizeV, 0.05F, 0.95F);
        this.editorSizeV = MathUtils.clamp(this.editorSizeV, 0.05F, 0.95F);

        this.main.resetFlex();
        this.editArea.resetFlex();
        this.preview.resetFlex();
        this.draggableMain.resetFlex();
        this.draggableEditor.resetFlex();

        if (this.horizontal)
        {
            this.main.relative(this.editor).y(1F - this.mainSizeH).w(1F).hTo(this.editor.area, 1F);
            this.editArea.relative(this.editor).x(1F - this.editorSizeH).wTo(this.editor.area, 1F).hTo(this.main.area, 0F);
            this.preview.relative(this.editor).w(1F - this.editorSizeH).hTo(this.main.area, 0F);
            this.draggableMain.hoverOnly().relative(this.main).x(0.5F, -40).y(-3).wh(80, 6);
            this.draggableEditor.hoverOnly().relative(this.preview).x(1F, -3).y(0.5F, -20).wh(6, 40);
        }
        else
        {
            this.main.relative(this.editor).w(this.mainSizeV).h(1F);
            this.editArea.relative(this.main).x(1F).y(this.editorSizeV).wTo(this.editor.area, 1F).hTo(this.editor.area, 1F);
            this.preview.relative(this.main).x(1F).wTo(this.editor.area, 1F).hTo(this.editArea.area, 0F);
            this.draggableMain.hoverOnly().relative(this.main).x(1F).y(0.5F, -40).wh(6, 80);
            this.draggableEditor.hoverOnly().relative(this.editArea).x(0.5F, -20).wh(40, 6);
        }

        if (resize)
        {
            this.resize();
            this.resize();
        }
    }

    public void showPanel(UIElement element)
    {
        this.cameraEditor.setVisible(false);
        this.replayEditor.setVisible(false);
        this.screenplayEditor.setVisible(false);

        element.setVisible(true);
    }

    public UIFilmController getController()
    {
        return this.controller;
    }

    public UIFilmUndoHandler getUndoHandler()
    {
        return this.undoHandler;
    }

    public RunnerCameraController getRunner()
    {
        return this.runner;
    }

    @Override
    protected UICRUDOverlayPanel createOverlayPanel()
    {
        UICRUDOverlayPanel crudPanel = super.createOverlayPanel();

        this.duplicateFilm = new UIIcon(Icons.SCENE, (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.GENERAL_DUPE,
                UIKeys.PANELS_MODALS_DUPE,
                (str) -> this.dupeData(crudPanel.namesList.getPath(str).toString())
            );

            panel.text.setText(crudPanel.namesList.getCurrentFirst().getLast());
            panel.text.filename();

            UIOverlay.addOverlay(this.getContext(), panel);
        });

        crudPanel.icons.add(this.duplicateFilm);

        return crudPanel;
    }

    private void dupeData(String name)
    {
        if (this.getData() != null && !this.overlay.namesList.getList().contains(name))
        {
            this.save();
            this.overlay.namesList.addFile(name);

            Film data = new Film();
            Position position = new Position();
            IdleClip idle = new IdleClip();
            int tick = this.runner.ticks;

            position.set(this.getCamera());
            idle.duration.set(BBSSettings.getDefaultDuration());
            idle.position.set(position);
            data.camera.addClip(idle);
            data.setId(name);

            for (Replay replay : this.data.replays.getList())
            {
                Replay copy = new Replay(replay.getId());

                copy.form.set(FormUtils.copy(replay.form.get()));

                for (BaseValue value : replay.keyframes.getAll())
                {
                    if (!(value instanceof KeyframeChannel<?>))
                    {
                        continue;
                    }

                    KeyframeChannel<Double> channel = (KeyframeChannel<Double>) value;

                    if (!channel.isEmpty())
                    {
                        KeyframeChannel<Double> newChannel = (KeyframeChannel<Double>) copy.keyframes.get(channel.getId());

                        newChannel.insert(0, channel.interpolate(tick));
                    }
                }

                for (Map.Entry<String, KeyframeChannel> entry : replay.properties.properties.entrySet())
                {
                    KeyframeChannel channel = entry.getValue();

                    if (channel.isEmpty())
                    {
                        continue;
                    }

                    KeyframeChannel newChannel = new KeyframeChannel(channel.getId(), channel.getFactory());
                    KeyframeSegment segment = channel.find(tick);

                    if (segment != null)
                    {
                        newChannel.insert(0, segment.createInterpolated());
                    }

                    if (!newChannel.isEmpty())
                    {
                        copy.properties.properties.put(newChannel.getId(), newChannel);
                        copy.properties.add(newChannel);
                    }
                }

                data.replays.add(copy);
            }

            this.fill(data);
            this.save();
        }
    }

    @Override
    public void open()
    {
        super.open();

        this.cameraEditor.open();

        Recorder recorder = BBSModClient.getFilms().stopRecording();

        if (recorder != null && recorder.tick >= 0)
        {
            this.lastRecorder = recorder;
        }
    }

    @Override
    public void appear()
    {
        super.appear();

        BBSRendering.setCustomSize(true);
        MorphRenderer.hidePlayer = true;

        CameraController cameraController = this.getCameraController();

        this.fillData();
        this.setFlight(false);
        cameraController.add(this.runner);
    }

    @Override
    public void close()
    {
        super.close();

        BBSRendering.setCustomSize(false);
        MorphRenderer.hidePlayer = false;

        CameraController cameraController = this.getCameraController();

        this.cameraEditor.embedView(null);
        this.setFlight(false);
        cameraController.remove(this.runner);

        this.disableContext();
        this.replayEditor.close();
    }

    @Override
    public void disappear()
    {
        super.disappear();

        BBSRendering.setCustomSize(false);
        MorphRenderer.hidePlayer = false;

        this.setFlight(false);
        this.getCameraController().remove(this.runner);

        this.disableContext();
    }

    private void disableContext()
    {
        this.runner.getContext().shutdown();
    }

    @Override
    public boolean needsBackground()
    {
        return false;
    }

    @Override
    public boolean canToggleVisibility()
    {
        return false;
    }

    @Override
    public boolean canPause()
    {
        return false;
    }

    @Override
    public boolean canRefresh()
    {
        return false;
    }

    @Override
    public ContentType getType()
    {
        return ContentType.FILMS;
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.FILM_TITLE;
    }

    @Override
    public void fill(Film data)
    {
        if (this.data != null)
        {
            this.disableContext();
        }

        if (data != null)
        {
            voiceLines.delete();
            voiceLines = new VoiceLines(BBSMod.getAssetsPath("audio/elevenlabs/" + data.getId()));

            this.undoHandler = new UIFilmUndoHandler(this);

            data.preCallback(this.undoHandler::handlePreValues);
        }
        else
        {
            this.undoHandler = null;
        }

        super.fill(data);

        this.openReplays.setEnabled(data != null);
        this.toggleHorizontal.setEnabled(data != null);
        this.openCameraEditor.setEnabled(data != null);
        this.openReplayEditor.setEnabled(data != null);
        this.openScreenplay.setEnabled(data != null);
        this.duplicateFilm.setEnabled(data != null);

        this.runner.setWork(data == null ? null : data.camera);
        this.cameraEditor.clips.setClips(data == null ? null : data.camera);
        this.replayEditor.setFilm(data);
        this.cameraEditor.pickClip(null);

        this.fillData();
        this.controller.createEntities();

        this.entered = data != null;

        if (this.lastRecorder != null)
        {
            /* Apply recorded data */
            if (data != null && this.lastRecorder.film.getId().equals(data.getId()))
            {
                int replayId = this.lastRecorder.exception;

                if (CollectionUtils.inRange(data.replays.getList(), replayId))
                {
                    BaseValue.edit(data.replays.getList().get(replayId), (replay) ->
                    {
                        replay.keyframes.copy(this.lastRecorder.keyframes);
                    });

                    this.dashboard.context.notify(UIKeys.FILMS_SAVED_NOTIFICATION.format(data.getId()), Colors.BLUE | Colors.A100);
                }
            }

            this.lastRecorder = null;
        }
    }

    public void undo()
    {
        if (this.data != null && this.undoHandler.getUndoManager().undo(this.data)) UIUtils.playClick();
    }

    public void redo()
    {
        if (this.data != null && this.undoHandler.getUndoManager().redo(this.data)) UIUtils.playClick();
    }

    public boolean isFlying()
    {
        return this.dashboard.orbitUI.canControl();
    }

    public void toggleFlight()
    {
        this.setFlight(!this.isFlying());
    }

    /**
     * Set flight mode
     */
    public void setFlight(boolean flight)
    {
        this.runner.setManual(flight ? this.position : null);

        if (!this.isRunning() || !flight)
        {
            this.dashboard.orbitUI.setControl(flight);

            /* Marking the latest undo as unmergeable */
            if (this.undoHandler != null && !flight)
            {
                this.undoHandler.markLastUndoNoMerging();
            }
            else
            {
                this.lastPosition.set(Position.ZERO);
            }
        }
    }

    @Override
    public void update()
    {
        this.controller.update();

        super.update();
    }

    /* Rendering code */

    /**
     * Draw everything on the screen
     */
    @Override
    public void render(UIContext context)
    {
        if (this.undoHandler != null)
        {
            this.undoHandler.submitUndo();
        }

        this.updateLogic(context);

        int color = BBSSettings.primaryColor.get();

        this.area.render(context.batcher, Colors.mulRGB(color | Colors.A100, 0.2F));

        if (this.editor.isVisible())
        {
            this.preview.area.render(context.batcher, Colors.A75);
        }

        super.render(context);

        if (this.entered)
        {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            Vec3d pos = player.getPos();
            Vector3d cameraPos = this.camera.position;
            double distance = cameraPos.distance(pos.x, pos.y, pos.z);
            int value = MinecraftClient.getInstance().options.getViewDistance().getValue();

            if (distance > value * 12)
            {
                this.getContext().notify(UIKeys.FILM_TELEPORT_DESCRIPTION, Colors.RED | Colors.A100);
            }

            this.entered = false;
        }
    }

    /**
     * Update logic for such components as repeat fixture, minema recording,
     * sync mode, flight mode, etc.
     */
    private void updateLogic(UIContext context)
    {
        Clip clip = this.cameraEditor.getClip();

        /* Loop fixture */
        if (BBSSettings.editorLoop.get() && this.isRunning())
        {
            long min = -1;
            long max = -1;

            if (clip != null)
            {
                min = clip.tick.get();
                max = min + clip.duration.get();
            }

            UIClips clips = this.cameraEditor.clips;

            if (clips.loopMin != clips.loopMax && clips.loopMin >= 0 && clips.loopMin < clips.loopMax)
            {
                min = clips.loopMin;
                max = clips.loopMax;
            }

            max = Math.min(max, this.data.camera.calculateDuration());

            if (min >= 0 && max >= 0 && min < max && (this.runner.ticks >= max - 1 || this.runner.ticks < min))
            {
                this.setCursor((int) min);
            }
        }

        /* Animate flight mode */
        if (this.dashboard.orbitUI.canControl())
        {
            this.dashboard.orbit.apply(this.position);

            Position current = new Position(this.getCamera());
            boolean check = this.flightEditTime.check();

            if (this.cameraEditor.getClip() != null && this.cameraEditor.isVisible())
            {
                if (!this.lastPosition.equals(current) && check)
                {
                    this.cameraEditor.editClip(current);
                }
            }

            if (check)
            {
                this.lastPosition.set(current);
            }
        }
        else
        {
            this.dashboard.orbit.setup(this.getCamera());
        }

        /* Rewind playback back to 0 */
        if (this.lastRunning && !this.isRunning())
        {
            this.lastRunning = this.runner.isRunning();
            this.setCursor(0);
        }
    }

    /**
     * Draw icons for indicating different active states (like syncing
     * or flight mode)
     */
    private void renderIcons(UIContext context)
    {
        int x = this.iconBar.area.ex() - 18;
        int y = this.iconBar.area.ey() - 18;

        if (BBSSettings.editorLoop.get())
        {
            context.batcher.icon(Icons.REFRESH, x, y);
        }
    }

    private void renderDividers(UIContext context)
    {
        Area a1 = this.saveIcon.area;
        Area a2 = this.toggleHorizontal.area;

        context.batcher.box(a1.x + 3, a1.ey() + 4, a1.ex() - 3, a1.ey() + 5, 0x22ffffff);
        context.batcher.box(a2.x + 3, a2.ey() + 4, a2.ex() - 3, a2.ey() + 5, 0x22ffffff);
    }

    @Override
    public void renderInWorld(WorldRenderContext context)
    {
        super.renderInWorld(context);

        if (!BBSRendering.isIrisShadowPass())
        {
            this.lastProjection.set(RenderSystem.getProjectionMatrix());
            this.lastView.set(context.matrixStack().peek().getPositionMatrix());
        }

        this.controller.renderFrame(context);
    }

    /* IUICameraWorkDelegate implementation */

    public Film getFilm()
    {
        return this.data;
    }

    public Camera getCamera()
    {
        return this.camera;
    }

    public Camera getWorldCamera()
    {
        return BBSModClient.getCameraController().camera;
    }

    public CameraController getCameraController()
    {
        return BBSModClient.getCameraController();
    }

    public int getCursor()
    {
        return this.runner.ticks;
    }

    public void setCursor(int value)
    {
        this.flightEditTime.mark();
        this.lastPosition.set(Position.ZERO);

        this.runner.ticks = Math.max(0, value);

        this.screenplayEditor.setCursor(this.runner.ticks);
    }

    public boolean isRunning()
    {
        return this.runner.isRunning();
    }

    public void togglePlayback()
    {
        this.setFlight(false);

        this.runner.toggle(this.getCursor());
        this.lastRunning = this.runner.isRunning();

        if (this.runner.isRunning())
        {
            this.cameraEditor.clips.scale.shiftIntoMiddle(this.runner.ticks);

            if (this.replayEditor.keyframeEditor != null)
            {
                this.replayEditor.keyframeEditor.view.getXAxis().shiftIntoMiddle(this.runner.ticks);
            }

            this.screenplayEditor.editor.clips.scale.shiftIntoMiddle(this.runner.ticks);
        }
    }

    public boolean canUseKeybinds()
    {
        return !this.isFlying();
    }

    public void fillData()
    {
        this.cameraEditor.fillData();

        if (this.data != null)
        {
            this.screenplayEditor.setFilm(this.data);
        }
    }
}