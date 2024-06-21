package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;

public class ColorKeyframeFactory implements IKeyframeFactory<Color>
{
    private Color i = new Color();

    @Override
    public Color fromData(BaseType data)
    {
        if (!data.isNumeric())
        {
            return new Color();
        }

        return Color.rgba(data.asNumeric().intValue());
    }

    @Override
    public BaseType toData(Color value)
    {
        return new IntType(value.getARGBColor());
    }

    @Override
    public Color createEmpty()
    {
        return new Color().set(Colors.WHITE);
    }

    @Override
    public Color copy(Color value)
    {
        return value.copy();
    }

    @Override
    public Color interpolate(Color preA, Color a, Color b, Color postB, IInterp interpolation, float x)
    {
        this.i.r = (float) interpolation.interpolate(IInterp.context.set(preA.r, a.r, b.r, postB.r, x));
        this.i.g = (float) interpolation.interpolate(IInterp.context.set(preA.g, a.g, b.g, postB.g, x));
        this.i.b = (float) interpolation.interpolate(IInterp.context.set(preA.b, a.b, b.b, postB.b, x));
        this.i.a = (float) interpolation.interpolate(IInterp.context.set(preA.a, a.a, b.a, postB.a, x));

        return this.i;
    }
}