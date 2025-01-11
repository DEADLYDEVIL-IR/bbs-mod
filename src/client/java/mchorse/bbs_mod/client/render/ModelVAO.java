package mchorse.bbs_mod.client.render;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.render.gl.Attributes;
import org.lwjgl.opengl.GL30;

public class ModelVAO
{
    private int vao;
    private int count;

    public ModelVAO(ModelVAOData data)
    {
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        this.upload(data);

        GL30.glBindVertexArray(currentVAO);
    }

    public void delete()
    {
        GL30.glDeleteVertexArrays(this.vao);
    }

    public void upload(ModelVAOData data)
    {
        this.vao = GL30.glGenVertexArrays();

        GL30.glBindVertexArray(this.vao);

        int vertexBuffer = GL30.glGenBuffers();
        int normalBuffer = GL30.glGenBuffers();
        int tangentsBuffer = GL30.glGenBuffers();
        int texCoordBuffer = GL30.glGenBuffers();
        int midTexCoordBuffer = GL30.glGenBuffers();

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vertexBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.vertices(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.POSITION, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, normalBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.normals(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.NORMAL, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, tangentsBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.tangents(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TANGENTS, 4, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, texCoordBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.texCoords(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TEXTURE_UV, 2, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, midTexCoordBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, data.texCoords(), GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.MID_TEXTURE_UV, 2, GL30.GL_FLOAT, false, 0, 0);

        GL30.glVertexAttribPointer(1, 4, GL30.GL_FLOAT, false, 0, 0);

        this.count = data.vertices().length / 3;
    }

    public void render()
    {
        boolean hasShaders = isShadersEnabled();

        GL30.glBindVertexArray(vao);

        enableAttributes(hasShaders);
        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, this.count);
        disableAttributes(hasShaders);
    }

    private static void disableAttributes(boolean hasShaders)
    {
        GL30.glDisableVertexAttribArray(Attributes.POSITION);
        GL30.glDisableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glDisableVertexAttribArray(Attributes.NORMAL);

        if (hasShaders) GL30.glDisableVertexAttribArray(Attributes.TANGENTS);
        if (hasShaders) GL30.glDisableVertexAttribArray(Attributes.MID_TEXTURE_UV);
    }

    private static void enableAttributes(boolean hasShaders)
    {
        GL30.glEnableVertexAttribArray(Attributes.POSITION);
        GL30.glEnableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glEnableVertexAttribArray(Attributes.NORMAL);

        if (hasShaders) GL30.glEnableVertexAttribArray(Attributes.TANGENTS);
        if (hasShaders) GL30.glEnableVertexAttribArray(Attributes.MID_TEXTURE_UV);
    }

    public static boolean isShadersEnabled()
    {
        return BBSRendering.isIrisShadersEnabled();
    }
}
