package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.DataPath;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionPlayer
{
    public Film film;
    public int tick;
    public boolean playing = true;
    public int exception;
    public boolean syncing;

    private ServerWorld world;
    private int duration;

    private Map<String, ActorEntity> actors = new HashMap<>();

    public ActionPlayer(ServerWorld world, Film film, int tick, int exception)
    {
        this.world = world;
        this.film = film;
        this.tick = tick;
        this.exception = exception;

        this.duration = film.camera.calculateDuration();

        this.updateReplayEntities();
    }

    public void updateReplayEntities()
    {
        for (ActorEntity entity : this.actors.values())
        {
            entity.discard();
        }

        this.actors.clear();

        List<Replay> list = this.film.replays.getList();

        for (int i = 0; i < list.size(); i++)
        {
            Replay replay = list.get(i);

            if (!replay.actor.get() || i == this.exception)
            {
                continue;
            }

            ActorEntity actor = new ActorEntity(BBSMod.ACTOR_ENTITY, this.world);

            actor.setForm(FormUtils.copy(replay.form.get()));

            this.apply(actor, replay, this.tick, false);
            this.actors.put(replay.getId(), actor);
            this.world.spawnEntity(actor);
        }

        for (ServerPlayerEntity player : this.world.getPlayers())
        {
            ServerNetwork.sendActors(player, this.film.getId(), this.actors);
        }
    }

    public ServerWorld getWorld()
    {
        return this.world;
    }

    public void apply(ActorEntity actor, Replay replay, float tick, boolean ticking)
    {
        double x = replay.keyframes.x.interpolate(tick);
        double y = replay.keyframes.y.interpolate(tick);
        double z = replay.keyframes.z.interpolate(tick);
        float yawHead = replay.keyframes.headYaw.interpolate(tick).floatValue();
        float yawBody = replay.keyframes.bodyYaw.interpolate(tick).floatValue();
        float pitch = replay.keyframes.pitch.interpolate(tick).floatValue();

        Vec3d pos = actor.getPos();

        if (ticking)
        {
            actor.move(MovementType.SELF, new Vec3d(x - pos.x, y - pos.y, z - pos.z));
        }

        actor.setPosition(x, y, z);
        actor.setYaw(yawHead);
        actor.setHeadYaw(yawHead);
        actor.setPitch(pitch);
        actor.setBodyYaw(yawBody);
        actor.setSneaking(replay.keyframes.sneaking.interpolate(tick) > 0);
        actor.setOnGround(replay.keyframes.grounded.interpolate(tick) > 0);
        actor.equipStack(EquipmentSlot.MAINHAND, replay.keyframes.mainHand.interpolate(tick));
        actor.equipStack(EquipmentSlot.OFFHAND, replay.keyframes.offHand.interpolate(tick));
        actor.equipStack(EquipmentSlot.HEAD, replay.keyframes.armorHead.interpolate(tick));
        actor.equipStack(EquipmentSlot.CHEST, replay.keyframes.armorChest.interpolate(tick));
        actor.equipStack(EquipmentSlot.LEGS, replay.keyframes.armorLegs.interpolate(tick));
        actor.equipStack(EquipmentSlot.FEET, replay.keyframes.armorFeet.interpolate(tick));

        actor.fallDistance = replay.keyframes.fall.interpolate(tick).floatValue();
    }

    public boolean tick()
    {
        for (Map.Entry<String, ActorEntity> entry : this.actors.entrySet())
        {
            Replay replay = (Replay) this.film.replays.get(entry.getKey());

            if (replay != null)
            {
                this.apply(entry.getValue(), replay, this.tick, true);
            }
        }

        if (!this.playing)
        {
            return false;
        }

        if (this.tick >= 0)
        {
            this.applyAction();
        }

        this.tick += 1;

        return !this.syncing ? this.tick >= this.duration : false;
    }

    private void applyAction()
    {
        SuperFakePlayer fakePlayer = SuperFakePlayer.get(this.world);
        List<Replay> list = this.film.replays.getList();

        for (int i = 0; i < list.size(); i++)
        {
            if (i == this.exception)
            {
                continue;
            }

            Replay replay = list.get(i);
            ActorEntity actor = this.actors.get(replay.getId());

            replay.applyActions(actor, fakePlayer, this.film, this.tick);
        }
    }

    public void syncData(String key, BaseType data)
    {
        BaseValue baseValue = this.film.getRecursively(new DataPath(key));

        if (baseValue != null)
        {
            baseValue.fromData(data);

            if (baseValue.getId().equals("actor") || baseValue.getId().equals("replays"))
            {
                this.updateReplayEntities();
            }
        }
    }

    public void goTo(int tick)
    {
        this.goTo(this.tick, tick);
    }

    public void goTo(int from, int tick)
    {
        for (Map.Entry<String, ActorEntity> entry : this.actors.entrySet())
        {
            Replay replay = (Replay) this.film.replays.get(entry.getKey());

            if (replay != null)
            {
                this.apply(entry.getValue(), replay, this.tick, false);
            }
        }

        if (from != tick)
        {
            this.tick = from;

            while (this.tick != tick)
            {
                this.tick += this.tick > tick ? -1 : 1;

                this.applyAction();
            }
        }
    }

    public void stop()
    {
        for (ActorEntity value : this.actors.values())
        {
            value.discard();
        }
    }
}