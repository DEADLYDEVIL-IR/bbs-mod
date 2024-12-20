package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIParticleSettings;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

public class UIVanillaParticleFormPanel extends UIFormPanel<VanillaParticleForm>
{
    public UIParticleSettings settings;
    public UIToggle paused;
    public UITrackpad velocity;
    public UITrackpad count;
    public UITrackpad frequency;
    public UITrackpad scatteringYaw;
    public UITrackpad scatteringPitch;

    public UIVanillaParticleFormPanel(UIForm editor)
    {
        super(editor);

        this.settings = new UIParticleSettings();
        this.paused = new UIToggle(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_PAUSED, (b) -> this.form.paused.set(b.getValue()));
        this.velocity = new UITrackpad((v) -> this.form.velocity.set(v.floatValue()));
        this.count = new UITrackpad((v) -> this.form.count.set(v.intValue())).integer();
        this.count.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_COUNT);
        this.frequency = new UITrackpad((v) -> this.form.frequency.set(v.intValue())).integer();
        this.frequency.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_FREQUENCY);
        this.scatteringYaw = new UITrackpad((v) -> this.form.scatteringYaw.set(v.floatValue()));
        this.scatteringYaw.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_HORIZONTAL);
        this.scatteringPitch = new UITrackpad((v) -> this.form.scatteringPitch.set(v.floatValue()));
        this.scatteringPitch.tooltip(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_VERTICAL);

        this.options.add(this.settings, this.paused.marginTop(6), UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_VELOCITY).marginTop(6), this.velocity);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_EMISSION).marginTop(6), this.count, this.frequency);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VANILLA_PARTICLE_SCATTER).marginTop(6), this.scatteringYaw, this.scatteringPitch);
    }

    @Override
    public void startEdit(VanillaParticleForm form)
    {
        super.startEdit(form);

        this.settings.setSettings(form.settings.get());
        this.paused.setValue(form.paused.get());
        this.velocity.setValue(form.velocity.get());
        this.count.setValue(form.count.get());
        this.frequency.setValue(form.frequency.get());
        this.scatteringYaw.setValue(form.scatteringYaw.get());
        this.scatteringPitch.setValue(form.scatteringPitch.get());
    }
}