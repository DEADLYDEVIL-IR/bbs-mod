package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseManager;
import mchorse.bbs_mod.utils.pose.PoseTransform;

import java.util.Collection;

public class UIPoseEditor extends UIElement
{
    public UIStringList groups;
    public UITrackpad fix;
    public UIColor color;
    public UIToggle lighting;
    public UIPropTransform transform;

    private String group = "";
    private Pose pose;

    public UIPoseEditor()
    {
        this.groups = new UIStringList((l) -> this.pickBone(l.get(0)));
        this.groups.background().h(UIStringList.DEFAULT_HEIGHT * 8 - 8);
        this.groups.scroll.cancelScrolling();
        this.groups.context(() ->
        {
            UIDataContextMenu menu = new UIDataContextMenu(PoseManager.INSTANCE, this.group, () -> this.pose.toData(), (data) ->
            {
                String current = this.groups.getCurrentFirst();

                this.changedPose(() -> this.pose.fromData(data));
                this.pickBone(current);
            });

            UIIcon flip = new UIIcon(Icons.CONVERT, (b) ->
            {
                String current = this.groups.getCurrentFirst();

                this.changedPose(() -> this.pose.flip());
                this.pickBone(current);
            });

            flip.tooltip(UIKeys.POSE_CONTEXT_FLIP_POSE);
            menu.row.addBefore(menu.save, flip);

            return menu;
        });
        this.fix = new UITrackpad((v) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setFix(poseTransform, v.floatValue());
            }
        });
        this.fix.limit(0D, 1D).increment(1D).values(0.1, 0.05D, 0.2D);
        this.fix.tooltip(UIKeys.POSE_CONTEXT_FIX_TOOLTIP);
        this.color = new UIColor((c) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setColor(poseTransform, c);
            }
        });
        this.color.withAlpha();
        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setLighting(poseTransform, b.getValue());
            }
        });
        this.lighting.h(20);
        this.transform = this.createTransformEditor();
        this.transform.setModel();

        this.column().vertical().stretch();
        this.add(this.groups, UI.label(UIKeys.POSE_CONTEXT_FIX), this.fix, UI.row(this.color, this.lighting), this.transform);
    }

    public String getGroup()
    {
        return this.groups.getCurrentFirst();
    }

    public void setPose(Pose pose, String group)
    {
        this.pose = pose;
        this.group = group;
    }

    public void fillGroups(Collection<String> groups)
    {
        this.groups.clear();
        this.groups.add(groups);
        this.groups.sort();

        this.fix.setVisible(!groups.isEmpty());
        this.color.setVisible(!groups.isEmpty());
        this.transform.setVisible(!groups.isEmpty());

        this.groups.setIndex(0);
        this.pickBone(this.groups.getCurrentFirst());
    }

    public void selectBone(String bone)
    {
        this.groups.setCurrentScroll(bone);
        this.pickBone(bone);
    }

    /* Subclass overridable methods */

    protected UIPropTransform createTransformEditor()
    {
        return new UIPropTransform().enableHotkeys();
    }

    protected void changedPose(Runnable runnable)
    {
        runnable.run();
    }

    private void pickBone(String bone)
    {
        PoseTransform poseTransform = this.pose.get(bone);

        if (poseTransform != null)
        {
            this.fix.setValue(poseTransform.fix);
            this.color.setColor(poseTransform.color.getARGBColor());
            this.lighting.setValue(poseTransform.lighting == 0F);
            this.transform.setTransform(poseTransform);
        }
        else
        {
            this.fix.setValue(0F);
            this.color.setColor(Colors.WHITE);
            this.lighting.setValue(false);
            this.transform.setTransform(null);
        }
    }

    protected void setFix(PoseTransform transform, float value)
    {
        transform.fix = value;
    }

    protected void setColor(PoseTransform transform, int value)
    {
        transform.color.set(value);
    }

    protected void setLighting(PoseTransform poseTransform, boolean value)
    {
        poseTransform.lighting = value ? 0F : 1F;
    }
}