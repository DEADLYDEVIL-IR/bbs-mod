package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.utils.interps.IInterp;
import org.lwjgl.glfw.GLFW;

public class InterpolationUtils
{
    public static void setupKeybind(IInterp interp, ContextAction action, IKey category)
    {
        String key = interp.getKey();

        if (key.endsWith("_in"))
        {
            action.key(category, interp.getKeyCode(), GLFW.GLFW_KEY_LEFT_SHIFT);
        }
        else if (key.endsWith("_out"))
        {
            action.key(category, interp.getKeyCode(), GLFW.GLFW_KEY_LEFT_CONTROL);
        }
        else
        {
            action.key(category, interp.getKeyCode());
        }
    }

    public static IKey getName(IInterp interp)
    {
        return UIKeys.C_INTERPOLATION.get(interp.getKey());
    }
}