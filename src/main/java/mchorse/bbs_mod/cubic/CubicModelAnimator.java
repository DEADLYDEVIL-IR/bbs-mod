package mchorse.bbs_mod.cubic;

import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.keyframes.BezierUtils;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3d;

public class CubicModelAnimator
{
    private static Vector3d p = new Vector3d();
    private static Vector3d s = new Vector3d();
    private static Vector3d r = new Vector3d();

    public static Vector3d interpolateList(Vector3d output, KeyframeChannel<MolangExpression> x, KeyframeChannel<MolangExpression> y, KeyframeChannel<MolangExpression> z, float frame, double defaultValue)
    {
        output.x = interpolateSegment(x.findSegment(frame), defaultValue);
        output.y = interpolateSegment(y.findSegment(frame), defaultValue);
        output.z = interpolateSegment(z.findSegment(frame), defaultValue);

        return output;
    }

    private static double interpolateSegment(KeyframeSegment<MolangExpression> segment, double defaultValue)
    {
        if (segment == null)
        {
            return defaultValue;
        }

        double start = segment.a.getValue().get();
        double destination = segment.b.getValue().get();

        if (segment.b.getInterpolation().getInterp() == Interpolations.BEZIER)
        {
            return BezierUtils.get(start, destination,
                segment.a.getTick(), segment.b.getTick(),
                segment.a.rx, segment.a.ry,
                segment.b.lx, segment.b.ly,
                segment.x
            );
        }

        double pre = segment.preA.getValue().get();
        double post = segment.postB.getValue().get();

        return segment.b.getInterpolation().interpolate(IInterp.context.set(pre, start, destination, post, segment.x));
    }

    public static void animate(Model model, Animation animation, float frame, float blend, boolean skipInitial)
    {
        for (ModelGroup group : model.topGroups)
        {
            animateGroup(group, animation, frame, blend, skipInitial);
        }
    }

    private static void animateGroup(ModelGroup group, Animation animation, float frame, float blend, boolean skipInitial)
    {
        boolean applied = false;

        AnimationPart part = animation.parts.get(group.id);

        if (part != null)
        {
            applyGroupAnimation(group, part, frame, blend);

            applied = true;
        }

        if (!applied && !skipInitial)
        {
            Transform initial = group.initial;
            Transform current = group.current;

            current.translate.lerp(initial.translate, blend);
            current.scale.lerp(initial.scale, blend);
            current.rotate.lerp(initial.rotate, blend);
        }

        for (ModelGroup childGroup : group.children)
        {
            animateGroup(childGroup, animation, frame, blend, skipInitial);
        }
    }

    private static void applyGroupAnimation(ModelGroup group, AnimationPart animation, float frame, float blend)
    {
        Vector3d position = interpolateList(p, animation.x, animation.y, animation.z, frame, 0D);
        Vector3d scale = interpolateList(s, animation.sx, animation.sy, animation.sz, frame, 1D);
        Vector3d rotation = interpolateList(r, animation.rx, animation.ry, animation.rz, frame, 0D);

        scale.sub(1, 1, 1);

        rotation.x *= -1;
        rotation.y *= -1;

        Transform initial = group.initial;
        Transform current = group.current;

        current.translate.x = Lerps.lerp(current.translate.x, (float) position.x + initial.translate.x, blend);
        current.translate.y = Lerps.lerp(current.translate.y, (float) position.y + initial.translate.y, blend);
        current.translate.z = Lerps.lerp(current.translate.z, (float) position.z + initial.translate.z, blend);

        current.scale.x = Lerps.lerp(current.scale.x, (float) scale.x + initial.scale.x, blend);
        current.scale.y = Lerps.lerp(current.scale.y, (float) scale.y + initial.scale.y, blend);
        current.scale.z = Lerps.lerp(current.scale.z, (float) scale.z + initial.scale.z, blend);

        current.rotate.x = Lerps.lerp(current.rotate.x, (float) rotation.x + initial.rotate.x, blend);
        current.rotate.y = Lerps.lerp(current.rotate.y, (float) rotation.y + initial.rotate.y, blend);
        current.rotate.z = Lerps.lerp(current.rotate.z, (float) rotation.z + initial.rotate.z, blend);
    }
}
