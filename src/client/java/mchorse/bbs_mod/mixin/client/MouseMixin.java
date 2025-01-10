package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin
{
    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    public void mouseScroll(long window, double horizontal, double vertical, CallbackInfo ci)
    {
        if (window == Window.getWindow())
        {
            Window.setVerticalScroll((int) vertical);
        }

        if (MinecraftClient.getInstance().currentScreen instanceof UIScreen screen)
        {
            screen.setHorizontal(horizontal);
        }
    }
}