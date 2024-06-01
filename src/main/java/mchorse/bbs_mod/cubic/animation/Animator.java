package mchorse.bbs_mod.cubic.animation;

import mchorse.bbs_mod.cubic.CubicModel;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Animator class
 * 
 * This class is responsible for applying currently running actions onto 
 * form (more specifically onto an armature).
 */
public class Animator implements IAnimator
{
    /* Actions */
    public ActionPlayback idle;
    public ActionPlayback running;
    public ActionPlayback sprinting;
    public ActionPlayback crouching;
    public ActionPlayback crouchingIdle;
    public ActionPlayback dying;
    public ActionPlayback falling;

    public ActionPlayback jump;
    public ActionPlayback swipe;
    public ActionPlayback hurt;
    public ActionPlayback land;
    public ActionPlayback shoot;
    public ActionPlayback consume;

    /* Action pipeline properties */
    public ActionPlayback active;
    public ActionPlayback lastActive;
    public List<ActionPlayback> actions = new ArrayList<>();

    public double prevX = Float.MAX_VALUE;
    public double prevZ = Float.MAX_VALUE;
    public double prevMY;

    /* States */
    public boolean wasOnGround = true;

    private CubicModel model;

    @Override
    public List<String> getActions()
    {
        return Arrays.asList(
            "idle", "running", "sprinting", "crouching", "crouching_idle", "dying", "falling",
            "swipe", "jump", "hurt", "land", "shoot", "consume"
        );
    }

    @Override
    public void setup(CubicModel model, ActionsConfig actions, boolean fade)
    {
        this.model = model;

        this.idle = this.createAction(this.idle, actions.getConfig("idle"), true);
        this.running = this.createAction(this.running, actions.getConfig("running"), true);
        this.sprinting = this.createAction(this.sprinting, actions.getConfig("sprinting"), true);
        this.crouching = this.createAction(this.crouching, actions.getConfig("crouching"), true);
        this.crouchingIdle = this.createAction(this.crouchingIdle, actions.getConfig("crouching_idle"), true);
        this.dying = this.createAction(this.dying, actions.getConfig("dying"), false);
        this.falling = this.createAction(this.falling, actions.getConfig("falling"), true);

        this.swipe = this.createAction(this.swipe, actions.getConfig("swipe"), false);
        this.jump = this.createAction(this.jump, actions.getConfig("jump"), false, 2);
        this.hurt = this.createAction(this.hurt, actions.getConfig("hurt"), false, 3);
        this.land = this.createAction(this.land, actions.getConfig("land"), false);
        this.shoot = this.createAction(this.shoot, actions.getConfig("shoot"), true);
        this.consume = this.createAction(this.consume, actions.getConfig("consume"), true);

        if (!fade)
        {
            this.setActiveAction(this.idle);

            if (this.idle != null)
            {
                this.idle.resetFade();
            }
        }
    }

    /**
     * Create an action with default priority
     */
    public ActionPlayback createAction(ActionPlayback old, ActionConfig config, boolean looping)
    {
        return this.createAction(old, config, looping, 1);
    }

    /**
     * Create an action playback based on given arguments. This method
     * is used for creating actions so it was easier to tell which
     * actions are missing. Beside that, you can pass an old action so
     * in form merging situation it wouldn't interrupt animation.
     */
    public ActionPlayback createAction(ActionPlayback old, ActionConfig config, boolean looping, int priority)
    {
        CubicModel model = this.model;
        Animations animations = model == null ? null : model.animations;

        if (animations == null)
        {
            return null;
        }

        Animation action = animations.get(config.name);

        /* If given action is missing, then omit creation of ActionPlayback */
        if (action == null)
        {
            return null;
        }

        /* If old is the same, then there is no point creating a new one */
        if (old != null && old.action == action)
        {
            old.config = config;
            old.setSpeed(1);

            return old;
        }

        return new ActionPlayback(action, config, looping, priority);
    }

    /**
     * Update animator. This method is responsible for updating action 
     * pipeline and also change current actions based on entity's state.
     */
    @Override
    public void update(IEntity target)
    {
        /* Fix issue with forms sudden running action */
        if (this.prevX == Float.MAX_VALUE)
        {
            this.prevX = target.getX();
            this.prevZ = target.getZ();
        }

        this.controlActions(target);

        /* Update primary actions */
        if (this.active != null)
        {
            this.active.update();
        }

        if (this.lastActive != null)
        {
            this.lastActive.update();
        }

        /* Update secondary actions */
        Iterator<ActionPlayback> it = this.actions.iterator();

        while (it.hasNext())
        {
            ActionPlayback action = it.next();

            action.update();

            if (action.finishedFading() && action.isFadingModeOut())
            {
                action.stopFade();
                it.remove();
            }
        }
    }

    /**
     * This method is designed specifically to isolate any controlling 
     * code (i.e. the ones that is responsible for switching between 
     * actions).
     */
    protected void controlActions(IEntity target)
    {
        Vec3d velocity = target.getVelocity();
        double dx = velocity.x;
        double dz = velocity.z;
        final float threshold = 0.05F;
        boolean moves = Math.abs(dx) > threshold || Math.abs(dz) > threshold;

        /* if (target.getHealth() <= 0)
        {
            this.setActiveAction(this.dying);
        }
        else if (target.isPlayerSleeping())
        {
            this.setActiveAction(this.sleeping);
        }
        else if (wet)
        {
            this.setActiveAction(!moves ? this.swimmingIdle : this.swimming);
        }
        else if (target.isRiding())
        {
            Entity riding = target.getRidingEntity();
            moves = Math.abs(riding.posX - this.prevX) > threshold || Math.abs(riding.posZ - this.prevZ) > threshold;

            this.prevX = riding.posX;
            this.prevZ = riding.posZ;
            this.setActiveAction(!moves ? this.ridingIdle : this.riding);
        }
        else if (creativeFlying || target.isElytraFlying())
        {
            this.setActiveAction(!moves ? this.flyingIdle : this.flying);
        } */
        if (false)
        {
            // TODO: implement more actions?
        }
        else
        {
            if (target.isSneaking())
            {
                this.setActiveAction(!moves ? this.crouchingIdle : this.crouching);
            }
            else if (!target.isOnGround() && velocity.y < 0 && target.getFallDistance() > 1.25)
            {
                this.setActiveAction(this.falling);
            }
            else if (target.isSprinting() && this.sprinting != null)
            {
                this.setActiveAction(this.sprinting);
            }
            else
            {
                this.setActiveAction(!moves ? this.idle : this.running);
            }

            if (target.isOnGround() && !this.wasOnGround && /* !target.isSprinting() && */ this.prevMY < -0.5)
            {
                this.addAction(this.land);
            }
        }

        if (!target.isOnGround() && this.wasOnGround && Math.abs(velocity.y) > 0.2F)
        {
            this.addAction(this.jump);
            this.wasOnGround = false;
        }

        if (target.isPunching())
        {
            this.addAction(this.swipe);
        }

        this.prevX = target.getX();
        this.prevZ = target.getZ();
        this.prevMY = velocity.y;

        this.wasOnGround = target.isOnGround();
    }

    /**
     * Set current active (primary) action 
     */
    public void setActiveAction(ActionPlayback action)
    {
        if (this.active == action || action == null)
        {
            return;
        }

        if (this.active != null && action.priority < this.active.priority)
        {
            return;
        }

        if (this.active != null)
        {
            this.lastActive = this.active;
        }

        this.active = action;
        this.active.rewind();
        this.active.fadeIn();
    }

    public void addAction(ActionPlayback action)
    {
        this.addAction(action, true);
    }

    /**
     * Add an additional secondary action to the playback 
     */
    public void addAction(ActionPlayback action, boolean rewind)
    {
        if (action == null)
        {
            return;
        }

        if (this.actions.contains(action))
        {
            if (rewind)
            {
                action.rewind();
            }

            return;
        }

        action.rewind();
        action.fadeIn();
        this.actions.add(action);
    }

    /**
     * Apply currently running action pipeline onto given armature
     */
    @Override
    public void applyActions(IEntity target, Model armature, float transition)
    {
        if (this.lastActive != null && this.active.isFading())
        {
            this.lastActive.apply(target, armature, transition, 1F, false);
        }

        if (this.active != null)
        {
            float fade = this.active.isFading() ? this.active.getFadeFactor(transition) : 1F;

            this.active.apply(target, armature, transition, fade, false);
        }

        for (ActionPlayback action : this.actions)
        {
            if (action.isFading())
            {
                action.apply(target, armature, transition, action.getFadeFactor(transition), true);
            }
            else
            {
                action.apply(target, armature, transition, 1F, true);
            }
        }
    }
}