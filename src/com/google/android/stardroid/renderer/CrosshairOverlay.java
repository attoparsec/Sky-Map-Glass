// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.stardroid.renderer;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;

import com.google.android.stardroid.R;
import com.google.android.stardroid.renderer.util.SearchHelper;
import com.google.android.stardroid.renderer.util.TextureManager;
import com.google.android.stardroid.renderer.util.TextureReference;
import com.google.android.stardroid.renderer.util.TexturedQuad;
import com.google.android.stardroid.units.Vector3;
import com.google.android.stardroid.util.MathUtil;

public class CrosshairOverlay {

  public void reloadTextures(GL10 gl, Resources res, TextureManager textureManager) {
    // Load the crosshair texture.
    mTex = textureManager.getTextureFromResource(gl, R.drawable.crosshair);
  }
  
  public void resize(GL10 gl, int screenWidth, int screenHeight) {
    mQuad = new TexturedQuad(mTex,
                             0, 0, 0,
                             40.0f / screenWidth, 0, 0,
                             0, 40.0f / screenHeight, 0);
  }
    
  public void draw(GL10 gl, SearchHelper searchHelper, boolean nightVisionMode) {
    // Return if the label has a negative z.
    Vector3 position = searchHelper.getTransformedPosition();
    if (position.z < 0) {
      return;
    }
    
    gl.glPushMatrix();
    gl.glLoadIdentity();
    
    gl.glTranslatef(position.x, position.y, 0);
    
    int period = 1000;
    long time = System.currentTimeMillis();
    float intensity = 0.7f + 0.3f * MathUtil.sin((time % period) * MathUtil.TWO_PI / period);
    if (nightVisionMode) {
      gl.glColor4f(intensity, 0, 0, 0.7f);
    } else {
      gl.glColor4f(intensity, intensity, 0, 0.7f);
    }
    
    gl.glEnable(GL10.GL_BLEND);
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);    

    mQuad.draw(gl);
    
    gl.glDisable(GL10.GL_BLEND);
    
    gl.glPopMatrix();
  }
  
  private TexturedQuad mQuad = null;
  private TextureReference mTex = null;
}
