package com.aiolos.vivek.filmstripvr;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

public class FilmStripRenderer implements GvrView.StereoRenderer {

    protected float[] modelSquare;
    protected float[] modelSquarePosition;

    private static final String TAG = "FilmStripActivity";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 0.01f;

    private static final int COORDS_PER_VERTEX = 3;

    private static final float MAX_MODEL_DISTANCE = 7.0f;

    private FloatBuffer squareVertices;
    private int squareProgram;

    private int squarePositionParam;
    private int squareModelViewProjectionParam;

    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;

    private final FilmStripActivity filmStripActivity;

    public FilmStripRenderer(FilmStripActivity filmStripActivity) {
        this.filmStripActivity = filmStripActivity;
        modelSquare = new float[16];
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        // Model first appears directly in front of user.
        modelSquarePosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
        headView = new float[16];
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        //A matrix representing the transform from the camera to the head.
        headTransform.getHeadView(headView, 0);
        FilmStripActivity.checkGLError("onReadyToDraw");
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        FilmStripActivity.checkGLError("colorParam");

        // Apply the eye transformation to the camera.
        //getEyeView: Returns a matrix that transforms from the camera to the current eye.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        //getPerspective: Convenience method that returns the perspective projection matrix for this eye.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        Matrix.multiplyMM(modelView, 0, view, 0, modelSquare, 0);

        //Create the modelView projection by applying multiple eye transformations to modelSquare.
        //Remember model square was already translated deeper into Z territory.
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawSquare();
    }

    public void drawSquare() {
        GLES20.glUseProgram(squareProgram);

        // Set the position of the square vertices in the shader.
        GLES20.glVertexAttribPointer(
                squarePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, squareVertices);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(squareModelViewProjectionParam, 1, false, modelViewProjection, 0);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(squarePositionParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(squarePositionParam);

        FilmStripActivity.checkGLError("Drawing square");
    }

    @Override
    public void onFinishFrame(Viewport viewport) {}

    @Override
    public void onSurfaceChanged(int i, int i1) {}

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        loadSquare();
    }

    private void loadSquare() {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(WorldLayoutData.SquareCoordinates.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        squareVertices = bbVertices.asFloatBuffer();
        squareVertices.put(WorldLayoutData.SquareCoordinates);
        squareVertices.position(0);

        int vertexShader = filmStripActivity.loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = filmStripActivity.loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);

        squareProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(squareProgram, vertexShader);
        GLES20.glAttachShader(squareProgram, gridShader);
        GLES20.glLinkProgram(squareProgram);
        GLES20.glUseProgram(squareProgram);

        FilmStripActivity.checkGLError("Square program");

        // return location of an attribute.
        // Vertex attributes are used to communicate from "outside" to the vertex shader.
        // Unlike uniform variables, values are provided per vertex (and not globally for all vertices)
        // Attributes can't be defined in the fragment shader.
        squarePositionParam = GLES20.glGetAttribLocation(squareProgram, "a_Position");

        //return location of an uniform variable.
        squareModelViewProjectionParam = GLES20.glGetUniformLocation(squareProgram, "u_MVP");

        FilmStripActivity.checkGLError("Square program params");

        updateModelSquarePosition();

        FilmStripActivity.checkGLError("onSurfaceCreated");
    }

    protected void updateModelSquarePosition() {
        //Create an identity matrix
        Matrix.setIdentityM(modelSquare, 0);
        //Move identity matrix away into the screen.
        Matrix.translateM(modelSquare, 0, modelSquarePosition[0], modelSquarePosition[1], modelSquarePosition[2]);
        FilmStripActivity.checkGLError("updateModelSquarePosition");
    }

    @Override
    public void onRendererShutdown() {}
}
