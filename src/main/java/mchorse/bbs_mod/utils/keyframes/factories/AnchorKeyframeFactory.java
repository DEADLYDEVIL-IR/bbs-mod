package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.properties.AnchorProperty;
import mchorse.bbs_mod.utils.interps.IInterp;

public class AnchorKeyframeFactory implements IKeyframeFactory<AnchorProperty.Anchor>
{
    private AnchorProperty.Anchor i = new AnchorProperty.Anchor();

    @Override
    public AnchorProperty.Anchor fromData(BaseType data)
    {
        AnchorProperty.Anchor anchor = new AnchorProperty.Anchor();

        anchor.fromData(data.asMap());

        return anchor;
    }

    @Override
    public BaseType toData(AnchorProperty.Anchor value)
    {
        return value.toData();
    }

    @Override
    public AnchorProperty.Anchor createEmpty()
    {
        return new AnchorProperty.Anchor();
    }

    @Override
    public AnchorProperty.Anchor copy(AnchorProperty.Anchor value)
    {
        AnchorProperty.Anchor anchor = new AnchorProperty.Anchor();

        anchor.actor = value.actor;
        anchor.attachment = value.attachment;

        return anchor;
    }

    @Override
    public AnchorProperty.Anchor interpolate(AnchorProperty.Anchor preA, AnchorProperty.Anchor a, AnchorProperty.Anchor b, AnchorProperty.Anchor postB, IInterp interpolation, float x)
    {
        this.i.actor = a.actor;
        this.i.attachment = a.attachment;

        this.i.previousActor = b.actor;
        this.i.previousAttachment = b.attachment;
        this.i.x = interpolation.interpolate(0F, 1F, x);

        return this.i;
    }
}