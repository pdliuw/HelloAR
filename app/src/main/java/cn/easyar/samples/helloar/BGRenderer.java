//================================================================================================================================
//
// Copyright (c) 2015-2022 VisionStar Information Technology (Shanghai) Co., Ltd. All Rights Reserved.
// EasyAR is the registered trademark or trademark of VisionStar Information Technology (Shanghai) Co., Ltd in China
// and other countries for the augmented reality technology developed by VisionStar Information Technology (Shanghai) Co., Ltd.
//
//================================================================================================================================

package cn.easyar.samples.helloar;
import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import cn.easyar.Matrix44F;
import cn.easyar.PixelFormat;
import cn.easyar.Vec2I;
import cn.easyar.Vec4F;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;

// all methods of this class can only be called on one thread with the same OpenGLES context
public class BGRenderer {

    private String videobackground_vert="attribute vec4 coord;\n"
                +"attribute vec2 texCoord;\n"
                +"varying vec2 texc;\n"
                +"\n"
                +"void main(void)\n"
                +"{\n"
                +"    gl_Position = coord;\n"
                +"    texc = texCoord;\n"
                +"}\n"
                +"\n"
        ;
    private String videobackground_bgr_frag="#ifdef GL_ES\n"
               +"precision mediump float;\n"
               +"#endif\n"
               +"uniform sampler2D texture;\n"
               +"varying vec2 texc;\n"
               +"\n"
               +"void main(void)\n"
               +"{\n"
               +"    gl_FragColor = texture2D(texture, texc).bgra;\n"
               +"}\n"
               +"\n"
        ;
    private String videobackground_rgb_frag="#ifdef GL_ES\n"
               +"precision mediump float;\n"
               +"#endif\n"
               +"uniform sampler2D texture;\n"
               +"varying vec2 texc;\n"
               +"\n"
               +"void main(void)\n"
               +"{\n"
               +"    gl_FragColor = texture2D(texture, texc);\n"
               +"}\n"
               +"\n"
        ;
    private String videobackground_yuv_i420_yv12_frag="#ifdef GL_ES\n"
                +"precision highp float;\n"
                +"#endif\n"
                +"uniform sampler2D texture;\n"
                +"uniform sampler2D u_texture;\n"
                +"uniform sampler2D v_texture;\n"
                +"varying vec2 texc;\n"
                +"\n"
                +"void main(void)\n"
                +"{\n"
                +"    float cb = texture2D(u_texture, texc).r - 0.5;\n"
                +"    float cr = texture2D(v_texture, texc).r - 0.5;\n"
                +"    vec3 ycbcr = vec3(texture2D(texture, texc).r, cb, cr);\n"
                +"    vec3 rgb = mat3(1, 1, 1,\n"
                +"        0, -0.344, 1.772,\n"
                +"        1.402, -0.714, 0) * ycbcr;\n"
                +"    gl_FragColor = vec4(rgb, 1.0);\n"
                +"}\n"
                +"\n"
        ;
    private String videobackground_yuv_nv12_frag="#ifdef GL_ES\n"
                +"precision highp float;\n"
                +"#endif\n"
                +"uniform sampler2D texture;\n"
                +"uniform sampler2D uv_texture;\n"
                +"varying vec2 texc;\n"
                +"\n"
                +"void main(void)\n"
                +"{\n"
                +"    vec2 cbcr = texture2D(uv_texture, texc).ra - vec2(0.5, 0.5);\n"
                +"    vec3 ycbcr = vec3(texture2D(texture, texc).r, cbcr);\n"
                +"    vec3 rgb = mat3(1.0, 1.0, 1.0,\n"
                +"        0.0, -0.344, 1.772,\n"
                +"        1.402, -0.714, 0.0) * ycbcr;\n"
                +"    gl_FragColor = vec4(rgb, 1.0);\n"
                +"}\n"
                +"\n"
        ;
    private String videobackground_yuv_nv21_frag="#ifdef GL_ES\n"
                +"precision highp float;\n"
                +"#endif\n"
                +"uniform sampler2D texture;\n"
                +"uniform sampler2D uv_texture;\n"
                +"varying vec2 texc;\n"
                +"\n"
                +"void main(void)\n"
                +"{\n"
                +"    vec2 cbcr = texture2D(uv_texture, texc).ar - vec2(0.5, 0.5);\n"
                +"    vec3 ycbcr = vec3(texture2D(texture, texc).r, cbcr);\n"
                +"    vec3 rgb = mat3(1, 1, 1,\n"
                +"        0, -0.344, 1.772,\n"
                +"        1.402, -0.714, 0.0) * ycbcr;\n"
                +"    gl_FragColor = vec4(rgb, 1.0);\n"
                +"}\n"
                +"\n"
        ;

    private byte yuv_back[] = {
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            127, 127,
            127, 127,
            127, 127,
            127, 127};

    enum FrameShader{
        RGB,
        YUV,
    };

    private FrameShader background_shader_ = FrameShader.RGB;

    private boolean initialized_ = false;
    private EGLContext current_context_ = null;
    private int background_program_ = 0;
    private IntBuffer background_texture_id_ = IntBuffer.allocate(1);
    private IntBuffer background_texture_uv_id_ = IntBuffer.allocate(1);
    private IntBuffer background_texture_u_id_ = IntBuffer.allocate(1);
    private IntBuffer background_texture_v_id_ = IntBuffer.allocate(1);
    private int background_coord_location_ = -1;
    private int background_texture_location_ = -1;
    private IntBuffer background_coord_vbo_ = IntBuffer.allocate(1);
    private IntBuffer background_texture_vbo_ = IntBuffer.allocate(1);
    private IntBuffer background_texture_fbo_ = IntBuffer.allocate(1);

    private int current_format_ = PixelFormat.Unknown;
    private Vec2I current_image_size_;

    private Vec4F mul(Matrix44F mat, Vec4F vec)
    {
        Vec4F val = new Vec4F(0,0,0,0);
        for (int i = 0; i < 4; ++i)
        {
            for (int k = 0; k < 4; ++k)
            {
                val.data[i] += mat.data[i*4 + k] * vec.data[k];
            }
        }
        return val;
    }

    public void dispose()
    {
        finalize(current_format_);
    }

    public void upload(int format, int width, int height, int pixelWidth, int pixelHeight, ByteBuffer buffer)
    {
        IntBuffer bak_tex = IntBuffer.allocate(1);
        IntBuffer bak_program = IntBuffer.allocate(1);
        IntBuffer bak_active_tex = IntBuffer.allocate(1);
        IntBuffer bak_tex_1 = IntBuffer.allocate(1);
        IntBuffer bak_tex_2 = IntBuffer.allocate(1);

        GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, bak_program);
        GLES30.glGetIntegerv(GLES30.GL_ACTIVE_TEXTURE, bak_active_tex);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, bak_tex);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, bak_tex_1);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
        GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, bak_tex_2);

        try {
            if (current_format_ != format) {
                finalize(current_format_);
                if (!initialize(format)) { return; }
                current_format_ = format;
            }
            current_image_size_ = new Vec2I(width, height);

            switch (background_shader_) {
                case RGB:
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_id_.get(0));
                    retrieveFrame(format, width, height, pixelWidth, pixelHeight, buffer, 0);
                case YUV:
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_id_.get(0));
                    retrieveFrame(format, width, height, pixelWidth, pixelHeight, buffer, 0);
                    if (format == PixelFormat.YUV_NV21 || format == PixelFormat.YUV_NV12) {
                        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_uv_id_.get(0));
                        retrieveFrame(format, width, height, pixelWidth, pixelHeight, buffer, 1);
                    } else {
                        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_u_id_.get(0));
                        retrieveFrame(format, width, height, pixelWidth, pixelHeight, buffer, 1);

                        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
                        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_v_id_.get(0));
                        retrieveFrame(format, width, height, pixelWidth, pixelHeight, buffer, 2);
                    }
            }
        } finally {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bak_tex.get(0));
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bak_tex_1.get(0));
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bak_tex_2.get(0));
            GLES30.glActiveTexture(bak_active_tex.get(0));
            GLES30.glUseProgram(bak_program.get(0));
        }
    }

    public void render(Matrix44F imageProjection)
    {
        IntBuffer bak_blend = IntBuffer.allocate(1),
                bak_depth = IntBuffer.allocate(1),
                bak_fbo = IntBuffer.allocate(1),
                bak_tex = IntBuffer.allocate(1),
                bak_arr_buf = IntBuffer.allocate(1),
                bak_ele_arr_buf = IntBuffer.allocate(1),
                bak_cull = IntBuffer.allocate(1),
                bak_program = IntBuffer.allocate(1),
                bak_active_tex = IntBuffer.allocate(1),
                bak_tex_1 = IntBuffer.allocate(1),
                bak_tex_2 = IntBuffer.allocate(1);
        IntBuffer bak_viewport = IntBuffer.allocate(4);

        GLES30.glGetIntegerv(GLES30.GL_BLEND, bak_blend);
        GLES30.glGetIntegerv(GLES30.GL_DEPTH_TEST, bak_depth);
        GLES30.glGetIntegerv(GLES30.GL_CULL_FACE, bak_cull);
        GLES30.glGetIntegerv(GLES30.GL_ARRAY_BUFFER_BINDING, bak_arr_buf);
        GLES30.glGetIntegerv(GLES30.GL_ELEMENT_ARRAY_BUFFER_BINDING, bak_ele_arr_buf);
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, bak_fbo);
        bak_viewport.position(0);
        GLES30.glGetIntegerv(GLES30.GL_VIEWPORT, bak_viewport);
        GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, bak_program);
        GLES30.glGetIntegerv(GLES30.GL_ACTIVE_TEXTURE, bak_active_tex);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, bak_tex);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, bak_tex_1);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
        GLES30.glGetIntegerv(GLES30.GL_TEXTURE_BINDING_2D, bak_tex_2);

        int[] va = {-1, -1};
        IntBuffer bak_va_binding[] = {IntBuffer.allocate(1),IntBuffer.allocate(1)};

        IntBuffer bak_va_enable[] = {IntBuffer.allocate(1),IntBuffer.allocate(1)},
                bak_va_size[]= {IntBuffer.allocate(1),IntBuffer.allocate(1)},
                bak_va_stride[] = {IntBuffer.allocate(1),IntBuffer.allocate(1)},
                bak_va_type[] = {IntBuffer.allocate(1),IntBuffer.allocate(1)},
                bak_va_norm[]= {IntBuffer.allocate(1),IntBuffer.allocate(1)};
        IntBuffer[] bak_va_pointer = {IntBuffer.allocate(1),IntBuffer.allocate(1)};

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);

        va[0] = background_coord_location_;
        va[1] = background_texture_location_;
        for (int i = 0; i < 2; ++i) {
            if (va[i] == -1)
                continue;
            GLES30.glGetVertexAttribiv(va[i], GLES30.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, bak_va_binding[i]);
            GLES30.glGetVertexAttribiv(va[i], GLES30.GL_VERTEX_ATTRIB_ARRAY_ENABLED, bak_va_enable[i]);
            GLES30.glGetVertexAttribiv(va[i], GLES30.GL_VERTEX_ATTRIB_ARRAY_SIZE, bak_va_size[i]);
            GLES30.glGetVertexAttribiv(va[i], GLES30.GL_VERTEX_ATTRIB_ARRAY_STRIDE, bak_va_stride[i]);
            GLES30.glGetVertexAttribiv(va[i], GLES30.GL_VERTEX_ATTRIB_ARRAY_TYPE, bak_va_type[i]);
            GLES30.glGetVertexAttribiv(va[i], GLES30.GL_VERTEX_ATTRIB_ARRAY_NORMALIZED, bak_va_norm[i]);
            //GLES30.glGetVertexAttribPointerv(va[i], GLES30.GL_VERTEX_ATTRIB_ARRAY_POINTER,bak_va_pointer[i]);
        }

        try {
            GLES30.glUseProgram(background_program_);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, background_coord_vbo_.get(0));

            GLES30.glEnableVertexAttribArray(background_coord_location_);
            GLES30.glVertexAttribPointer(background_coord_location_, 3, GLES30.GL_FLOAT, false, 0, 0);

            float vertices[] = {
                -1.0f,  -1.0f,  0.f,
                1.0f,  -1.0f,  0.f,
                1.0f,   1.0f,  0.f,
                -1.0f, 1.0f,  0.f,
            };

            Vec4F v0_v4f = new Vec4F(vertices[0], vertices[1], vertices[2], 1.0f);
            Vec4F v1_v4f = new Vec4F(vertices[3], vertices[4], vertices[5], 1.0f);
            Vec4F v2_v4f = new Vec4F(vertices[6], vertices[7], vertices[8], 1.0f);
            Vec4F v3_v4f = new Vec4F(vertices[9], vertices[10], vertices[11], 1.0f);
            v0_v4f = mul(imageProjection, v0_v4f);
            v1_v4f = mul(imageProjection, v1_v4f);
            v2_v4f = mul(imageProjection, v2_v4f);
            v3_v4f = mul(imageProjection, v3_v4f);

            Vec4F v4f_array[] = {v0_v4f, v1_v4f, v2_v4f, v3_v4f};

            for (int i = 0; i < 4; i += 1) {
                for (int k = 0; k < 3; k += 1) {
                    vertices[i * 3 + k] = v4f_array[i].data[k];
                }
            }
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 4 * 12, FloatBuffer.wrap(vertices), GLES30.GL_DYNAMIC_DRAW);

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, background_texture_vbo_.get(0));
            GLES30.glEnableVertexAttribArray(background_texture_location_);
            GLES30.glVertexAttribPointer(background_texture_location_, 2, GLES30.GL_FLOAT, false, 0, 0);

            switch (background_shader_) {
                case RGB:
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_id_.get(0));
                case YUV:
                    GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_id_.get(0));
                    if (current_format_ == PixelFormat.YUV_NV21 || current_format_ == PixelFormat.YUV_NV12) {
                        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_uv_id_.get(0));
                    } else {
                        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
                        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_u_id_.get(0));

                        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
                        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_v_id_.get(0));
                    }
            }

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);
        } finally {
            if (bak_blend.get(0) != 0) GLES30.glEnable(GLES30.GL_BLEND);
            if (bak_depth.get(0) != 0) GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            if (bak_cull.get(0) != 0) GLES30.glEnable(GLES30.GL_CULL_FACE);

            for (int i = 0; i < 2; ++i) {
                if (bak_va_binding[i].get(0) == 0)
                    continue;
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bak_va_binding[i].get(0));
                if (bak_va_enable[i].get(0) != 0)
                    GLES30.glEnableVertexAttribArray(va[i]);
                else
                    GLES30.glDisableVertexAttribArray(va[i]);
                //bak_va_pointer[i].position(0);
                //GLES30.glVertexAttribPointer(va[i], bak_va_size[i].get(0),bak_va_type[i].get(0), bak_va_norm[i].get(0)==0? false:true, bak_va_stride[i].get(0), bak_va_pointer[i].get(0));
            }

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, bak_arr_buf.get(0));
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, bak_ele_arr_buf.get(0));
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bak_fbo.get(0));
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bak_tex.get(0));
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bak_tex_1.get(0));
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bak_tex_2.get(0));
            GLES30.glActiveTexture(bak_active_tex.get(0));
            GLES30.glViewport(bak_viewport.get(0), bak_viewport.get(1), bak_viewport.get(2), bak_viewport.get(3));
            GLES30.glUseProgram(bak_program.get(0));
        }
    }

    private byte[] uv_buffer;
    private void retrieveFrame(int format, int width, int height, int pixelWidth, int pixelHeight, ByteBuffer buffer, int retrieve_count)
    {
        if (retrieve_count == 0) {
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, pixelWidth);
            switch (background_shader_) {
                case RGB:
                    switch (format) {
                        case PixelFormat.Unknown:
                            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
                            break;
                        case PixelFormat.Gray:
                            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, width, height, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, buffer);
                            break;
                        case PixelFormat.BGR888:
                            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB, width, height, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buffer);
                            break;
                        case PixelFormat.RGB888:
                            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB, width, height, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buffer);
                            break;
                        case PixelFormat.RGBA8888:
                            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);
                            break;
                        case PixelFormat.BGRA8888:
                            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);
                            break;
                        default:
                            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
                    }
                    break;
                case YUV:
                    if (format == PixelFormat.YUV_NV21 || format == PixelFormat.YUV_NV12 ||
                            format == PixelFormat.YUV_I420 || format == PixelFormat.YUV_YV12) {
                        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, width, height, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, buffer);
                    } else {
                        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);
                        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, 4, 4, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, ByteBuffer.wrap(yuv_back));
                    }
                    break;
                default:
                    break;
            }
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);
        } else if (retrieve_count == 1 || retrieve_count == 2) {
            if (background_shader_ != FrameShader.YUV) { return; }
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, pixelWidth / 2);
            int type;
            int offset;
            int size;
            if (format == PixelFormat.YUV_NV21 || format == PixelFormat.YUV_NV12) {
                type = GLES30.GL_LUMINANCE_ALPHA;
                offset = pixelWidth * pixelHeight;
                size = pixelWidth * pixelHeight / 2;
            } else {
                type = GLES30.GL_LUMINANCE;
                if (format == PixelFormat.YUV_I420) {
                    if (retrieve_count == 1) { //U
                        offset = pixelWidth * pixelHeight;
                    } else if (retrieve_count == 2) { //V
                        offset = pixelWidth * pixelHeight * 5 / 4;
                    } else {
                        throw new IllegalStateException();
                    }
                    size = pixelWidth * pixelHeight / 4;
                } else if (format == PixelFormat.YUV_YV12) {
                    if (retrieve_count == 1) { //U
                        offset = pixelWidth * pixelHeight * 5 / 4;
                    } else if (retrieve_count == 2) { //V
                        offset = pixelWidth * pixelHeight;
                    } else {
                        throw new IllegalStateException();
                    }
                    size = pixelWidth * pixelHeight / 4;
                } else {
                    throw new IllegalStateException();
                }
            }
            if ((uv_buffer == null) || (uv_buffer.length != size)) {
                uv_buffer = new byte[size];
            }
            buffer.position(offset);
            buffer.get(uv_buffer);
            buffer.position(0);
            //Android bug: GLES30.glTexImage2D crash with buffer with offset, https://issuetracker.google.com/issues/136535675
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, type, width / 2, height / 2, 0, type, GLES30.GL_UNSIGNED_BYTE, ByteBuffer.wrap(uv_buffer));
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);
        }
    }

    private boolean initialize(int format)
    {
        if (format == PixelFormat.Unknown) {
            return false;
        }
        current_context_ = ((EGL10)EGLContext.getEGL()).eglGetCurrentContext();
        background_program_ = GLES30.glCreateProgram();
        int vertShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vertShader, videobackground_vert);
        GLES30.glCompileShader(vertShader);
        int fragShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        switch (format) {
            case PixelFormat.Gray:
            case PixelFormat.RGB888:
            case PixelFormat.RGBA8888:
                background_shader_ = FrameShader.RGB;
                GLES30.glShaderSource(fragShader, videobackground_rgb_frag);
                break;
            case PixelFormat.BGR888:
            case PixelFormat.BGRA8888:
                background_shader_ = FrameShader.RGB;
                GLES30.glShaderSource(fragShader, videobackground_bgr_frag);
                break;
            case PixelFormat.YUV_NV21:
                background_shader_ = FrameShader.YUV;
                GLES30.glShaderSource(fragShader, videobackground_yuv_nv21_frag);
                break;
            case PixelFormat.YUV_NV12:
                background_shader_ = FrameShader.YUV;
                GLES30.glShaderSource(fragShader,videobackground_yuv_nv12_frag);
                break;
            case PixelFormat.YUV_I420:
            case PixelFormat.YUV_YV12:
                background_shader_ = FrameShader.YUV;
                GLES30.glShaderSource(fragShader,videobackground_yuv_i420_yv12_frag);
                break;
            default:
                break;
        }
        GLES30.glCompileShader(fragShader);
        GLES30.glAttachShader(background_program_, vertShader);
        GLES30.glAttachShader(background_program_, fragShader);

        IntBuffer compileSuccess_0 = IntBuffer.allocate(1);

        GLES30.glGetShaderiv(vertShader, GLES30.GL_COMPILE_STATUS, compileSuccess_0);
        if (compileSuccess_0.get(0) == GLES30.GL_FALSE) {
            String messages = GLES30.glGetShaderInfoLog(vertShader);
            Log.v("[easyar]", "vertshader error " + messages);
        }
        IntBuffer compileSuccess_1 = IntBuffer.allocate(1);
        GLES30.glGetShaderiv(fragShader, GLES30.GL_COMPILE_STATUS, compileSuccess_1);
        if (compileSuccess_1.get(0) == GLES30.GL_FALSE) {
            String messages = GLES30.glGetShaderInfoLog(fragShader);
            Log.v("[easyar]", "vertshader error " + messages);
        }
        GLES30.glLinkProgram(background_program_);
        GLES30.glDeleteShader(vertShader);
        GLES30.glDeleteShader(fragShader);

        IntBuffer linkstatus = IntBuffer.allocate(1);
        GLES30.glGetProgramiv(background_program_, GLES30.GL_LINK_STATUS, linkstatus);
        GLES30.glUseProgram(background_program_);
        background_coord_location_ = GLES30.glGetAttribLocation(background_program_, "coord");
        background_texture_location_ = GLES30.glGetAttribLocation(background_program_, "texCoord");

        GLES30.glDeleteShader(vertShader);
        GLES30.glDeleteShader(fragShader);
        GLES30.glGenBuffers(1, background_coord_vbo_);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, background_coord_vbo_.get(0));
        float coord[] = {-1.0f, -1.0f, 0.f, 1.0f, -1.0f, 0.f, 1.0f, 1.0f, 0.f, -1.0f, 1.0f, 0.f};
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 12*4, FloatBuffer.wrap(coord), GLES30.GL_DYNAMIC_DRAW);
        GLES30.glGenBuffers(1, background_texture_vbo_);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, background_texture_vbo_.get(0));
        float texcoord[] = {0.f, 1.f, 1.f, 1.f, 1.f, 0.f, 0.f, 0.f}; //input texture data is Y-inverted
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 8 * 4, FloatBuffer.wrap(texcoord), GLES30.GL_STATIC_DRAW);

        GLES30.glUniform1i(GLES30.glGetUniformLocation(background_program_, "texture"), 0);
        GLES30.glGenTextures(1, background_texture_id_);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_id_.get(0));
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        switch (background_shader_) {
            case RGB:
                break;
            case YUV:
                if (format == PixelFormat.YUV_NV21 || format == PixelFormat.YUV_NV12) {
                    GLES30.glUniform1i(GLES30.glGetUniformLocation(background_program_, "uv_texture"), 1);
                    GLES30.glGenTextures(1, background_texture_uv_id_);
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_uv_id_.get(0));
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
                } else {
                    GLES30.glUniform1i(GLES30.glGetUniformLocation(background_program_, "u_texture"), 1);
                    GLES30.glGenTextures(1, background_texture_u_id_);
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_u_id_.get(0));
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

                    GLES30.glUniform1i(GLES30.glGetUniformLocation(background_program_, "v_texture"), 2);
                    GLES30.glGenTextures(1, background_texture_v_id_);
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, background_texture_v_id_.get(0));
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
                }
                break;
            default:
                break;

        }
        GLES30.glGenFramebuffers(1, background_texture_fbo_);
        initialized_ = true;
        return true;
    }

    private void finalize(int format)
    {
        if (!initialized_) { return; }

        if (((EGL10)EGLContext.getEGL()).eglGetCurrentContext().equals(current_context_)) { //destroy resources unless the context has lost
            GLES30.glDeleteProgram(background_program_);
            GLES30.glDeleteBuffers(1, background_coord_vbo_);
            GLES30.glDeleteBuffers(1, background_texture_vbo_);
            GLES30.glDeleteFramebuffers(1, background_texture_fbo_);
            GLES30.glDeleteTextures(1, background_texture_id_);
            switch (background_shader_)
            {
                case RGB:
                    break;
                case YUV:
                    if (format == PixelFormat.YUV_NV21 || format == PixelFormat.YUV_NV12)
                    {
                        GLES30.glDeleteTextures(1, background_texture_uv_id_);
                    }
                    else
                    {
                        GLES30.glDeleteTextures(1, background_texture_u_id_);
                        GLES30.glDeleteTextures(1, background_texture_v_id_);
                    }
                    break;
                default:
                    break;
            }
        }
        initialized_ = false;
    }
}
