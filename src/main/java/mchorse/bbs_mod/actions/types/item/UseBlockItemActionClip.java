package mchorse.bbs_mod.actions.types.item;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.values.ValueBlockHitResult;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;

public class UseBlockItemActionClip extends ItemActionClip
{
    public final ValueBlockHitResult hit = new ValueBlockHitResult("hit");

    public UseBlockItemActionClip()
    {
        this.add(this.hit);
    }

    @Override
    public void apply(SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        Hand hand = this.hand.get() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        ItemStack copy = this.itemStack.get().copy();

        this.applyPositionRotation(player, replay, tick);
        player.setStackInHand(hand, copy);
        this.itemStack.get().useOnBlock(new ItemUsageContext(player.getWorld(), player, hand, copy, this.hit.getHitResult()));
        player.setStackInHand(hand, ItemStack.EMPTY);
    }

    @Override
    protected Clip create()
    {
        return new UseBlockItemActionClip();
    }
}