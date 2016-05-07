package com.medicaltrust;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Hashtable;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.opengl.GLUtils;

public class GraphicUtils {
  final static private String TAG = "GraphicUtils";

  // for speeding up
  private static Hashtable<Integer, float[]> verticesPool =
    new Hashtable<Integer, float[]>();
  private static Hashtable<Integer, float[]> colorsPool = 
    new Hashtable<Integer, float[]>();
  private static Hashtable<Integer, float[]> coordsPool =
    new Hashtable<Integer, float[]>();
  private static Hashtable<Integer, FloatBuffer> polygonVerticesPool = 
    new Hashtable<Integer, FloatBuffer>();
  private static Hashtable<Integer, FloatBuffer> polygonColorsPool =
    new Hashtable<Integer, FloatBuffer>();
  private static Hashtable<Integer, FloatBuffer> texCoordsPool = 
    new Hashtable<Integer, FloatBuffer>();
  
  public static final FloatBuffer makeFloatBuffer(float[] arr) {
    // Line up system memory
    ByteBuffer bb = ByteBuffer.allocateDirect(arr.length*4);
    bb.order(ByteOrder.nativeOrder());
    // Transfer arr to system memory
    FloatBuffer fb = bb.asFloatBuffer();
    fb.put(arr);
    fb.position(0);
    return fb;
  }

  // Recycle arra
  public static float[] getVertices(int n) {
    if (verticesPool.containsKey(n)) {
      return verticesPool.get(n); // Reuse 
    }
    float[] vertices = new float[n]; // Create
    verticesPool.put(n, vertices);
    return vertices;
  }
  public static float[] getColors(int n) {
    if (colorsPool.containsKey(n)) {
      return colorsPool.get(n);
    }
    float[] colors = new float[n];
    colorsPool.put(n, colors);
    return colors;
  }
  public static float[] getCoords(int n) {
    if (coordsPool.containsKey(n)) {
      return coordsPool.get(n);
    }
    float[] coords = new float[n];
    coordsPool.put(n, coords);
    return coords;
  }
  // Reuse FloatBuffer
  public static final FloatBuffer makeVerticesBuffer(float[] arr) {
    FloatBuffer fb = null;
    if (polygonVerticesPool.containsKey(arr.length)) {
      fb = polygonVerticesPool.get(arr.length);
      fb.clear();
      fb.put(arr);
      fb.position(0);
      return fb;
    }
    fb = makeFloatBuffer(arr);
    polygonVerticesPool.put(arr.length, fb);
    return fb;
  }
  public static final FloatBuffer makeColorsBuffer(float[] arr) {
    FloatBuffer fb = null;
    if (polygonColorsPool.containsKey(arr.length)) {
      fb = polygonColorsPool.get(arr.length);
      fb.clear();
      fb.put(arr);
      fb.position(0);
      return fb;
    }
    fb = makeFloatBuffer(arr);
    polygonColorsPool.put(arr.length, fb);
    return fb;
  }
  public static final FloatBuffer makeTexCoordsBuffer(float[] arr) {
    FloatBuffer fb = null;
    if (texCoordsPool.containsKey(arr.length)) {
      fb = texCoordsPool.get(arr.length);
      fb.clear();
      fb.put(arr);
      fb.position(0);
      return fb;
    }
    fb = makeFloatBuffer(arr);
    texCoordsPool.put(arr.length, fb);
    return fb;
  }
  
  // Load texture
  public static final int loadTexture
    (GL10 gl, Resources rsrc, int resId) {
    int[] textures = new int[1];
		
    // Make bitmap
    Bitmap bmp = BitmapFactory.decodeResource(rsrc, resId, options);
    if (bmp == null) return 0;
		
    // Make texture
    gl.glGenTextures(1, textures, 0);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
    GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                       GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                       GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
		
    // Destroy bitmap
    bmp.recycle();
		
    return textures[0];
  }
  
  private static final BitmapFactory.Options options =
    new BitmapFactory.Options();
  static {
    // Disable resources auto resizing
    options.inScaled = false;
    // Read as 32bit
    options.inPreferredConfig = Config.ARGB_8888;
  }
  /*==================================================
     Let's draw differrent shapes!!
   ==================================================*/

  public static final void drawLine
    (GL10 gl,
     float x1, float y1, float x2, float y2,
     float r, float g, float b)
  {
    
    float[] ver = getVertices(4);
    ver[0] = (float)x1; ver[1] = (float)y1;
    ver[2] = (float)x2; ver[3] = (float)y2;

    float[] col = getColors(8);
    col[0] = r; col[1] = g; col[2] = b; col[3] = 1.0f;
    col[4] = r; col[5] = g; col[6] = b; col[7] = 1.0f;

    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    gl.glDrawArrays(GL10.GL_LINES, 0, 2);
  }

  public static final void drawRect
    (GL10 gl,
     float x1, float y1, float x2, float y2,
     float r, float g, float b)
  {
    float[] ver = getVertices(8);
    ver[0] = (float)x1; ver[1] = (float)y1;
    ver[2] = (float)x1; ver[3] = (float)y2;
    ver[4] = (float)x2; ver[5] = (float)y2;
    ver[6] = (float)x2; ver[7] = (float)y1;

    float[] col = getColors(16);
    col[0] = r; col[1] = g; col[2] = b; col[3] = 1.0f;
    col[4] = r; col[5] = g; col[6] = b; col[7] = 1.0f;
    col[8]  = r; col[9] = g; col[10] = b; col[11] = 1.0f;
    col[12] = r; col[13] = g; col[14] = b; col[15] = 1.0f;

    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);

  }

  public static final void drawFilledRect
    (GL10 gl,
     float x1, float y1, float x2, float y2,
     float r, float g, float b)
  {
    float[] ver = getVertices(8);
    ver[0] = x1; ver[1] = y1;
    ver[2] = x2; ver[3] = y1;
    ver[4] = x1; ver[5] = y2;
    ver[6] = x2; ver[7] = y2;

    float[] col = getColors(16);
    col[0] = r; col[1] = g; col[2] = b; col[3] = 1.0f;
    col[4] = r; col[5] = g; col[6] = b; col[7] = 1.0f;
    col[8] = r; col[9] = g; col[10]= b; col[11]= 1.0f;
    col[12]= r; col[13]= g; col[14]= b; col[15]= 1.0f;

    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
    // vertex, rule, offset, count of vertexes
    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
  }

  public static final void drawRoundRect
    (GL10 gl,
     float x1, float y1, float x2, float y2,
     float arcWidth, float arcHeight,
     float r, float g, float b)
  {
    // draw round rectangle
  }

  public static final void drawFilledRoundRect
    (GL10 gl,
     float x1, float y1, float x2, float y2,
     float arcWidth, float arcHeight,
     float r, float g, float b)
  {
    // draw filled round rectangle
  }
  
  public static final void drawOval
    (GL10 gl,
     float x1, float y1, float x2, float y2,
     float r, float g, float b)
  {
    int divides = 36;

    float x = (x1 + x2) * 0.5f;
    float y = (y1 + y2) * 0.5f;
    float rx = Math.abs(x1 - x);
    float ry = Math.abs(y1 - y);
		
    float[] ver = getVertices(divides * 2);
    float[] col = getColors(divides * 4);

    int vid = 0;
    int cid = 0;
		
    for (int i = 1; i < divides-1; i++) {
      double theta1 = 2.0d / divides * i * Math.PI;
      ver[vid++] = (float)Math.cos(theta1) * rx + x;
      ver[vid++] = (float)Math.sin(theta1) * ry + y;
      col[cid++] = r;
      col[cid++] = g;
      col[cid++] = b;
      col[cid++] = 1.0f;
    }
    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
    gl.glDrawArrays(GL10.GL_TRIANGLES, 0, divides);
  }

  public static final void drawFilledOval
    (GL10 gl,
     float x1, float y1, float x2, float y2,
     float r, float g, float b)
  {
    int divides = 36;

    float x = (x1 + x2) * 0.5f;
    float y = (y1 + y2) * 0.5f;
    float rx = Math.abs(x1 - x);
    float ry = Math.abs(y1 - y);
		
    float[] ver = getVertices(divides * 3 * 2);

    int vid = 0;
		
    // 頂点角計算
    for (int i = 1; i < divides-1; i++) {
      double theta1 = 2.0d / divides * i * Math.PI;
      double theta2 = 2.0d / divides * (i+1) * Math.PI;
		
      // 頂点座標セット
      ver[vid++] = x + rx;
      ver[vid++] = y;
      ver[vid++] = (float)Math.cos(theta1) * rx + x;
      ver[vid++] = (float)Math.sin(theta1) * ry + y;
      ver[vid++] = (float)Math.cos(theta2) * rx + x;
      ver[vid++] = (float)Math.sin(theta2) * ry + y;
    } // 色
    gl.glColor4f(r, g, b, 1.0f);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		
    FloatBuffer vfb = makeVerticesBuffer(ver);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
    gl.glDrawArrays(GL10.GL_TRIANGLES, 0, (divides-2) * 3);
  }

  public static final void drawLines
    (GL10 gl,
     float[] x, float[] y, int points,
     float r, float g, float b)
  {
    float[] ver = getVertices(points * 2);
    float[] col = getColors(points * 4);

    int vid = 0;
    int cid = 0;
		
    for (int i = 0; i < points; i++) {
      ver[vid++] = x[i];
      ver[vid++] = y[i];
      col[cid++] = r;
      col[cid++] = g;
      col[cid++] = b;
      col[cid++] = 1.0f;
    }
    
    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
    gl.glDrawArrays(GL10.GL_LINES, 0, points);
  }
  
  public static final void drawPath
    (GL10 gl,
     float[] x, float[] y, int points,
     float r, float g, float b)
  {
    float[] ver = getVertices(points * 2);
    float[] col = getColors(points * 4);

    int vid = 0;
    int cid = 0;
		
    for (int i = 0; i < points; i++) {
      ver[vid++] = x[i];
      ver[vid++] = y[i];
      col[cid++] = r;
      col[cid++] = g;
      col[cid++] = b;
      col[cid++] = 1.0f;
    }
    
    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		
    gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, points);
  }

  public static final void drawTexture
    (GL10 gl, int tex,
     float x1, float y1, float x2, float y2,
     float u1, float v1, float u2, float v2,
     float r, float g, float b)
  {
    float[] ver = getVertices(8);
    float[] col = getColors(16);
    float[] coo = getCoords(8);
    
    ver[0] = x1; ver[1] = y1;
    ver[2] = x2; ver[3] = y1;
    ver[4] = x1; ver[5] = y2;
    ver[6] = x2; ver[7] = y2;

    col[0] = r; col[1] = g; col[2] = b; col[3] = 1.0f;
    col[4] = r; col[5] = g; col[6] = b; col[7] = 1.0f;
    col[8] = r; col[9] = g; col[10]= b; col[11]= 1.0f;
    col[12]= r; col[13]= g; col[14]= b; col[15]= 1.0f;

    coo[0] = u1; coo[1] = v1;
    coo[2] = u2; coo[3] = v1;
    coo[4] = u1; coo[5] = v2;
    coo[6] = u2; coo[7] = v2;
		
    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
    FloatBuffer tfb = makeTexCoordsBuffer(coo);

    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    
    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, tfb);
    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glDisable(GL10.GL_TEXTURE_2D);
  }

  public static final void drawChar
    (GL10 gl, int tex,
     char c,
     float x1, float y1, float x2, float y2,
     float r, float g, float b)
  {
    float u = CHAR_UNIT * (c - ' ');
    
    drawTexture(gl, tex, x1, y1, x2, y2,
                u, 0.0f, u+CHAR_UNIT, 1.0f,
                r, g, b);
  }

  final static private float CHAR_UNIT = 1.0f / 128.2f;
                
  public static final void drawText
    (GL10 gl, int tex,
     String str,
     float x, float y, float w, float h,
     float r, float g, float b)
  {
    int len = str.length();

    float[] ver = getVertices(12*len);
    float[] col = getColors(24*len);
    float[] coo = getCoords(12*len);

    int vid = 0;
    int cid = 0;
    int tid = 0;

    float y2 = y + h;

    for (int i=0; i<len; i++) {
      
      float x1 = x + w*i;
      float x2 = x + w*(i+1);

      ver[vid++] = x1; ver[vid++] = y;
      ver[vid++] = x1; ver[vid++] = y2;
      ver[vid++] = x2; ver[vid++] = y;
      
      ver[vid++] = x2; ver[vid++] = y;
      ver[vid++] = x1; ver[vid++] = y2;
      ver[vid++] = x2; ver[vid++] = y2;
    
      col[cid++] = r; col[cid++] = g; col[cid++] = b; col[cid++] = 1.0f;
      col[cid++] = r; col[cid++] = g; col[cid++] = b; col[cid++] = 1.0f;
      col[cid++] = r; col[cid++] = g; col[cid++] = b; col[cid++] = 1.0f;
      col[cid++] = r; col[cid++] = g; col[cid++] = b; col[cid++] = 1.0f;
      col[cid++] = r; col[cid++] = g; col[cid++] = b; col[cid++] = 1.0f;
      col[cid++] = r; col[cid++] = g; col[cid++] = b; col[cid++] = 1.0f;

      float u1 = CHAR_UNIT * (str.charAt(i) - ' ');
      float u2 = u1 + CHAR_UNIT;
      float v1 = 0.0f;
      float v2 = 1.0f;

      coo[tid++] = u1; coo[tid++] = v1;
      coo[tid++] = u1; coo[tid++] = v2;
      coo[tid++] = u2; coo[tid++] = v1;

      coo[tid++] = u2; coo[tid++] = v1;
      coo[tid++] = u1; coo[tid++] = v2;
      coo[tid++] = u2; coo[tid++] = v2;
    }
    
    FloatBuffer vfb = makeVerticesBuffer(ver);
    FloatBuffer cfb = makeColorsBuffer(col);
    FloatBuffer tfb = makeTexCoordsBuffer(coo);

    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
		
    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vfb);
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, cfb);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    
    gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, tfb);
    gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
    gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 6*len);

    gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    gl.glDisable(GL10.GL_TEXTURE_2D);
  }
}