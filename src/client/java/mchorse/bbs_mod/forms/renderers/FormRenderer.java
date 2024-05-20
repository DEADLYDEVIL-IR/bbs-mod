package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class FormRenderer <T extends Form>
{
    protected T form;

    public static void renderBodyPart(BodyPart part, FormRenderingContext context)
    {
        IEntity oldEntity = context.entity;

        context.entity = part.useTarget ? oldEntity : part.getEntity();

        if (part.getForm() != null)
        {
            context.stack.push();
            MatrixStackUtils.applyTransform(context.stack, part.getTransform());

            FormUtilsClient.render(part.getForm(), context);

            context.stack.pop();
        }

        context.entity = oldEntity;
    }

    public FormRenderer(T form)
    {
        this.form = form;
    }

    public T getForm()
    {
        return this.form;
    }

    public List<String> getBones()
    {
        return Collections.emptyList();
    }

    public final void renderUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.renderInUI(context, x1, y1, x2, y2);

        String name = this.form.name.get();

        if (!name.isEmpty())
        {
            FontRenderer font = context.batcher.getFont();

            name = font.limitToWidth(name, x2 - x1 - 3);

            int w = font.getWidth(name);

            context.batcher.textCard(name, (x2 + x1 - w) / 2, y1 + 6, Colors.WHITE, Colors.ACTIVE | Colors.A50);
        }
    }

    protected abstract void renderInUI(UIContext context, int x1, int y1, int x2, int y2);

    public final void render(FormRenderingContext context)
    {
        int light = context.light;
        Boolean visible = this.form.visible.get(context.getTransition());

        if (!visible)
        {
            return;
        }

        boolean isPicking = context.stencilMap != null;

        context.stack.push();
        MatrixStackUtils.applyTransform(context.stack, this.form.transform.get(context.getTransition()));

        if (!this.form.lighting.get(context.getTransition()))
        {
            context.light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }

        this.render3D(context);

        if (isPicking)
        {
            this.updateStencilMap(context);
        }

        this.renderBodyParts(context);

        context.stack.pop();

        context.light = light;
    }

    protected Supplier<ShaderProgram> getShader(FormRenderingContext context, Supplier<ShaderProgram> normal, Supplier<ShaderProgram> picking)
    {
        if (context.isPicking())
        {
            this.setupTarget(context, picking.get());

            return picking;
        }

        return normal;
    }

    protected ShaderProgram getShader(FormRenderingContext context, ShaderProgram normal, ShaderProgram picking)
    {
        if (context.isPicking())
        {
            this.setupTarget(context, picking);

            return picking;
        }

        return normal;
    }

    protected void setupTarget(FormRenderingContext context, ShaderProgram program)
    {
        GlUniform target = program.getUniform("Target");

        if (target != null)
        {
            target.set(context.getPickingIndex());
        }
    }

    protected void updateStencilMap(FormRenderingContext context)
    {
        context.stencilMap.addPicking(this.form);
    }

    protected void render3D(FormRenderingContext context)
    {}

    public void renderBodyParts(FormRenderingContext context)
    {
        for (BodyPart part : this.form.parts.getAll())
        {
            renderBodyPart(part, context);
        }
    }

    public void collectMatrices(IEntity entity, MatrixStack stack, Map<String, Matrix4f> matrices, String prefix, float transition)
    {
        stack.push();
        MatrixStackUtils.applyTransform(stack, this.form.transform.get(transition));

        matrices.put(prefix, new Matrix4f(stack.peek().getPositionMatrix()));

        int i = 0;

        for (BodyPart part : this.form.parts.getAll())
        {
            Form form = part.getForm();

            if (form != null)
            {
                stack.push();
                MatrixStackUtils.applyTransform(stack, part.getTransform());

                FormUtilsClient.getRenderer(form).collectMatrices(entity, stack, matrices, StringUtils.combinePaths(prefix, String.valueOf(i)), transition);

                stack.pop();
            }

            i += 1;
        }

        stack.pop();
    }
}