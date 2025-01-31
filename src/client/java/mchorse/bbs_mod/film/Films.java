package mchorse.bbs_mod.film;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.AudioRenderer;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.properties.IFormProperty;
import mchorse.bbs_mod.forms.triggers.StateTrigger;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Films
{
    private List<FilmController> controllers = new ArrayList<FilmController>();
    private Recorder recorder;

    public Map<String, Map<String, Integer>> actors = new HashMap<>();

    /* Static helpers */

    public static void playFilm(String filmId, boolean withCamera)
    {
        if (ClientNetwork.isIsBBSModOnServer())
        {
            ClientNetwork.sendToggleFilm(filmId, withCamera);
        }
        else
        {
            if (BBSModClient.getFilms().has(filmId))
            {
                stopFilm(filmId);
            }
            else
            {
                ContentType.FILMS.getRepository().load(filmId, (data) ->
                {
                    MinecraftClient.getInstance().execute(() -> playFilm((Film) data, withCamera));
                });
            }
        }
    }

    public static void playFilm(Film film, boolean withCamera)
    {
        FilmController filmController = new FilmController(film);

        if (withCamera)
        {
            PlayCameraController controller = new PlayCameraController(film.camera);

            controller.getContext().entities.addAll(filmController.getEntities());
            BBSModClient.getCameraController().add(controller);
        }

        BBSModClient.getFilms().add(filmController);
    }

    public static void stopFilm(String filmId)
    {
        Film film = BBSModClient.getFilms().remove(filmId);
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (film != null && current instanceof PlayCameraController play)
        {
            if (play.getContext().clips == film.camera)
            {
                if (BBSModClient.getCameraController().remove(play) instanceof RunnerCameraController controller)
                {
                    controller.getContext().shutdown();
                }
            }
        }
    }

    /* Instance API */

    public Recorder getRecorder()
    {
        return this.recorder;
    }

    public void recordTrigger(Form form, StateTrigger trigger)
    {
        if (this.recorder == null)
        {
            return;
        }

        for (String key : trigger.states.keys())
        {
            boolean existed = this.recorder.properties.properties.containsKey(key);
            KeyframeChannel channel = this.recorder.properties.getOrCreate(form, key);

            if (channel != null)
            {
                if (!existed)
                {
                    IFormProperty property = FormUtils.getProperty(form, key);

                    channel.insert(0, channel.getFactory().fromData(property.toData()));
                }

                channel.insert(this.recorder.tick, channel.getFactory().fromData(trigger.states.get(key)));
            }
        }
    }

    public void startRecording(Film film, int replayId, int tick)
    {
        Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

        this.recorder = new Recorder(film, morph == null ? null : morph.getForm(), replayId, tick);

        if (ClientNetwork.isIsBBSModOnServer())
        {
            ClientNetwork.sendActionRecording(film.getId(), replayId, this.recorder.tick, this.recorder.countdown, true);
        }

        Replay replay = CollectionUtils.getSafe(film.replays.getList(), replayId);

        if (replay != null)
        {
            ClientNetwork.sendPlayerForm(replay.form.get());
        }
    }

    public Recorder stopRecording()
    {
        Recorder recorder = this.recorder;

        this.recorder = null;

        if (recorder != null)
        {
            for (BaseValue value : recorder.keyframes.getAll())
            {
                if (value instanceof KeyframeChannel channel)
                {
                    channel.simplify();
                }
            }

            if (ClientNetwork.isIsBBSModOnServer())
            {
                ClientNetwork.sendActionRecording(recorder.film.getId(), recorder.exception, recorder.tick, 0, false);
            }

            Vector3d pos = recorder.lastPosition;

            if (pos != null)
            {
                Vector4f rot = recorder.lastRotation;

                PlayerUtils.teleport(pos.x, pos.y, pos.z, rot.z, rot.y);
                ClientNetwork.sendPlayerForm(recorder.lastForm);
            }
        }

        return recorder;
    }

    public void add(FilmController controller)
    {
        this.controllers.add(controller);
    }

    public boolean has(String filmId)
    {
        for (FilmController controller : this.controllers)
        {
            if (controller.film.getId().equals(filmId))
            {
                return true;
            }
        }

        return false;
    }

    public Film remove(String id)
    {
        Iterator<FilmController> it = this.controllers.iterator();

        while (it.hasNext())
        {
            FilmController next = it.next();

            if (next.film.getId().equals(id))
            {
                next.shutdown();
                it.remove();

                return next.film;
            }
        }

        return null;
    }

    public void updateActors(String filmId, Map<String, Integer> actors)
    {
        this.actors.put(filmId, actors);
    }

    public void startRenderFrame(float transition)
    {
        for (FilmController controller : this.controllers)
        {
            controller.startRenderFrame(transition);
        }
    }

    public void update()
    {
        this.controllers.removeIf((film) ->
        {
            film.update();

            boolean finished = film.hasFinished();

            if (finished)
            {
                film.shutdown();
            }

            return finished;
        });

        if (this.recorder != null)
        {
            this.recorder.update();
        }
    }

    public void render(WorldRenderContext context)
    {
        RenderSystem.enableDepthTest();

        for (FilmController controller : this.controllers)
        {
            controller.render(context);
        }

        if (this.recorder != null)
        {
            this.recorder.render(context);
        }

        RenderSystem.disableDepthTest();
    }

    public void renderHud(DrawContext drawContext, float tickDelta)
    {
        Batcher2D batcher2D = new Batcher2D(drawContext);
        Recorder recorder = BBSModClient.getFilms().getRecorder();

        if (recorder != null)
        {
            String label = recorder.hasNotStarted() ?
                String.valueOf(TimeUtils.toSeconds(recorder.countdown)) :
                UIKeys.FILM_RECORDING.format(recorder.tick).get();
            int x = 5;
            int y = 5;
            int w = batcher2D.getFont().getWidth(label);

            batcher2D.box(x, y, x + 18 + w + 3, y + 16, Colors.A50);
            batcher2D.icon(Icons.SPHERE, Colors.RED | Colors.A100, x, y);
            batcher2D.textShadow(label, x + 18, y + 4);

            /* Render audio waveform */
            List<AudioClip> audioClips = new ArrayList<>();

            for (Clip clip : recorder.film.camera.get())
            {
                if (clip instanceof AudioClip)
                {
                    audioClips.add((AudioClip) clip);
                }
            }

            int sw = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int sh = MinecraftClient.getInstance().getWindow().getScaledHeight();
            w = (int) (sw * BBSSettings.audioWaveformWidth.get());
            x = sw / 2 - w / 2;
            y = sh / 2 + 100;

            AudioRenderer.renderAll(batcher2D, audioClips, recorder.tick + tickDelta, x, y, w, BBSSettings.audioWaveformHeight.get(), sw, sh);
        }
    }

    public void reset()
    {
        controllers.clear();

        recorder = null;
    }
}