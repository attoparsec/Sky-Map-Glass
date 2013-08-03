// Copyright 2008 Google Inc.
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

package com.google.android.stardroid.control;

import com.google.android.stardroid.control.AstronomerModel.Pointing;
import com.google.android.stardroid.units.GeocentricCoordinates;
import com.google.android.stardroid.units.Matrix33;
import com.google.android.stardroid.units.Vector3;
import com.google.android.stardroid.util.Geometry;
import com.google.android.stardroid.util.MiscUtil;

import android.util.Log;

/**
 * Allows user-input elements such as touch screens and trackballs to move the
 * map.
 *
 * @author John Taylor
 */
public class ManualOrientationController extends AbstractController {
  private static final String TAG = MiscUtil.getTag(ManualOrientationController.class);

  @Override
  public void start() {
    // Nothing to do
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  /**
   * Moves the astronomer's pointing right or left.
   *
   * @param radians the angular change in the pointing in radians (only
   * accurate in the limit as radians tends to 0.)
   */
  public void changeRightLeft(float radians) {
    // TODO(johntaylor): Some of the Math in here perhaps belongs in
    // AstronomerModel.
    if (!enabled) {
      return;
    }
    Pointing pointing = model.getPointing();
    GeocentricCoordinates pointingXyz = pointing.getLineOfSight();
    GeocentricCoordinates topXyz = pointing.getPerpendicular();
    Vector3 horizontalXyz = Geometry.vectorProduct(pointingXyz, topXyz);
    Vector3 deltaXyz = Geometry.scaleVector(horizontalXyz, radians);

    Vector3 newPointingXyz = Geometry.addVectors(pointingXyz, deltaXyz);
    newPointingXyz.normalize();

    model.setPointing(newPointingXyz, topXyz);
  }

  /**
   * Moves the astronomer's pointing up or down.
   *
   * @param radians the angular change in the pointing in radians (only
   * accurate in the limit as radians tends to 0.)
   */
  public void changeUpDown(float radians) {
    if (!enabled) {
      return;
    }
    // Log.d(TAG, "Scrolling up down");
    Pointing pointing = model.getPointing();
    GeocentricCoordinates pointingXyz = pointing.getLineOfSight();
    // Log.d(TAG, "Current view direction " + viewDir);
    GeocentricCoordinates topXyz = pointing.getPerpendicular();

    Vector3 deltaXyz = Geometry.scaleVector(topXyz, -radians);
    Vector3 newPointingXyz = Geometry.addVectors(pointingXyz, deltaXyz);
    newPointingXyz.normalize();

    Vector3 deltaUpXyz = Geometry.scaleVector(pointingXyz, radians);
    Vector3 newUpXyz = Geometry.addVectors(topXyz, deltaUpXyz);
    newUpXyz.normalize();

    model.setPointing(newPointingXyz, newUpXyz);
  }

  /**
   * Rotates the astronomer's view.
   */
  public void rotate(float degrees) {
    if (!enabled) {
      return;
    }
    Log.d(TAG, "Rotating by " + degrees);
    Pointing pointing = model.getPointing();
    GeocentricCoordinates pointingXyz = pointing.getLineOfSight();

    Matrix33 rotation = Geometry.calculateRotationMatrix(degrees, pointingXyz);

    GeocentricCoordinates topXyz = pointing.getPerpendicular();

    Vector3 newUpXyz = Geometry.matrixVectorMultiply(rotation, topXyz);
    newUpXyz.normalize();

    model.setPointing(pointingXyz, newUpXyz);
  }
}
