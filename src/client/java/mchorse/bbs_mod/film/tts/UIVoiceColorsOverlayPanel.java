package mchorse.bbs_mod.film.tts;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

public class UIVoiceColorsOverlayPanel extends UIOverlayPanel
{
    public ValueVoiceColors colors;

    public UIVoiceColorsOverlayPanel(ValueVoiceColors colors)
    {
        super(UIKeys.VOICE_COLORS_TITLE);

        this.colors = colors;

        UIScrollView scrollView = UI.scrollView(5, 10);

        for (ElevenLabsVoice voice : ElevenLabsAPI.getVoices().values())
        {
            if (!voice.isAllowed())
            {
                continue;
            }

            UILabel label = new UILabel(IKey.constant(voice.name)).labelAnchor(0F, 0.5F);
            UIColor color = new UIColor((c) -> this.colors.setColor(voice.name, c));

            color.setColor(this.colors.getColor(voice.name));
            label.w(60);

            scrollView.add(UI.row(label, color));
        }

        this.content.add(scrollView.full(this.content));
    }
}
