package mchorse.bbs_mod.mixin.client.sodium;

import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Colors;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelVertex.class)
public class ModelVertexMixin
{
    @ModifyVariable(method = "write", at = @At("HEAD"), ordinal = 0, remap = false)
    private static int onWrite(int color)
    {
        if (RecolorVertexConsumer.newColor != null)
        {
            Colors.COLOR.set(color);
            Colors.COLOR.mul(RecolorVertexConsumer.newColor);

            return Colors.COLOR.getARGBColor();
        }

        return color;
    }
}