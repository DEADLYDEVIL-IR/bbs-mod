package mchorse.bbs_mod.utils.clips;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.ValueBoolean;
import mchorse.bbs_mod.settings.values.ValueFloat;
import mchorse.bbs_mod.settings.values.ValueGroup;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class Envelope extends ValueGroup
{
    public final ValueBoolean enabled = new ValueBoolean("enabled");

    public final ValueFloat fadeIn = new ValueFloat("fadeIn", 10F);
    public final ValueFloat fadeOut = new ValueFloat("fadeOut", 10F);

    public final Interpolation pre = new Interpolation("pre", Interpolations.MAP);
    public final Interpolation post = new Interpolation("post", Interpolations.MAP);

    public final ValueBoolean keyframes = new ValueBoolean("keyframes");
    public final KeyframeChannel<Double> channel = new KeyframeChannel<>("channel", KeyframeFactories.DOUBLE);

    public Envelope(String id)
    {
        super(id);

        this.add(this.enabled);
        this.add(this.fadeIn);
        this.add(this.fadeOut);
        this.add(this.pre);
        this.add(this.post);
        this.add(this.keyframes);
        this.add(this.channel);

        this.channel.insert(0, 0D);
        this.channel.insert(BBSSettings.getDefaultDuration(), 1D);
    }

    public float getStartX(int duration)
    {
        return 0;
    }

    public float getStartDuration(int duration)
    {
        return this.fadeIn.get();
    }

    public float getEndX(int duration)
    {
        return duration;
    }

    public float getEndDuration(int duration)
    {
        return duration - this.fadeOut.get();
    }

    public float factorEnabled(int duration, float tick)
    {
        if (!this.enabled.get())
        {
            return 1;
        }

        return this.factor(duration, tick);
    }

    public float factor(int duration, float tick)
    {
        float envelope = 0;

        if (this.keyframes.get())
        {
            if (!this.channel.isEmpty())
            {
                envelope = MathUtils.clamp(this.channel.interpolate(tick).floatValue(), 0, 1);
            }
        }
        else
        {
            float lowOut = this.fadeIn.get();

            envelope = Lerps.envelope(tick, 0, lowOut, this.getEndDuration(duration), this.getEndX(duration));
            envelope = (float) (tick <= lowOut ? this.pre : this.post).interpolate(IInterp.context.set(0, 1, envelope));
        }

        return envelope;
    }

    public void breakDown(Clip original, int offset)
    {
        this.fadeIn.set(0F);
        original.envelope.fadeOut.set(0F);

        this.channel.moveX(-offset);
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (data.isMap())
        {
            MapType map = data.asMap();

            if (map.has("interpolation"))
            {
                BaseType interpolation = map.get("interpolation");

                this.pre.fromData(interpolation);
                this.post.fromData(interpolation);
            }
        }
    }
}