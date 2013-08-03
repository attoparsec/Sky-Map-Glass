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

package com.google.android.stardroid.units;

import android.util.FloatMath;
import java.util.Date;

import com.google.android.stardroid.util.Geometry;
import static com.google.android.stardroid.util.Geometry.calculateRADecOfZenith;
import static com.google.android.stardroid.util.TimeUtil.normalizeAngle;

/**
 * This class corresponds to an object's location in Euclidean space
 * when it is projected onto a unit sphere (with the Earth at the
 * center).
 *
 * @author Brent Bryan
 */
public class GeocentricCoordinates extends Vector3 {

  public GeocentricCoordinates(float x, float y, float z) {
    super(x, y, z);
  }

  public void updateFromLatLong(Date time, LatLong observer, LatLong target) {
	  updateFromLatLong(time, observer.latitude, observer.longitude, target.latitude, target.longitude);
  }

  private void updateFromLatLong(Date time, float latitude_o, float longitude_o, float latitude_t, float longitude_t) {
	  // Get current zenith, use the RA to adjust latlons for rotation of the Earth 
	  RaDec up = calculateRADecOfZenith(time, new LatLong(latitude_o, 0));
	  longitude_o = (float)normalizeAngle(longitude_o + up.ra);
	  longitude_t = (float)normalizeAngle(longitude_t + up.ra);

	  // Convert latlon (observer and target) to ECEF
	  // See links from: http://en.wikipedia.org/wiki/ECEF
	  latitude_o = (float)Math.toRadians(latitude_o);
	  longitude_o = (float)Math.toRadians(longitude_o);
	  latitude_t = (float)Math.toRadians(latitude_t);
	  longitude_t = (float)Math.toRadians(longitude_t);
	  
	  float a = 6378137;
	  float b = 6356752.31424518f;
	  float e = FloatMath.sqrt((a * a - b * b) / (a * a));
	  float h = 0;

	  float N_o = a / FloatMath.sqrt(1 - e * e * FloatMath.sin(latitude_o)* FloatMath.sin(latitude_o));
	  float X_o = (N_o + h) * FloatMath.cos(latitude_o) * FloatMath.cos(longitude_o);
	  float Y_o = (N_o + h) * FloatMath.cos(latitude_o) * FloatMath.sin(longitude_o);
	  float Z_o = ((b * b) / (a * a) * N_o + h) * FloatMath.sin(latitude_o);

	  float N_t = a / FloatMath.sqrt(1 - e * e * FloatMath.sin(latitude_t)* FloatMath.sin(latitude_t));
	  float X_t = (N_t + h) * FloatMath.cos(latitude_t) * FloatMath.cos(longitude_t);
	  float Y_t = (N_t + h) * FloatMath.cos(latitude_t) * FloatMath.sin(longitude_t);
	  float Z_t = ((b * b) / (a * a) * N_t + h) * FloatMath.sin(latitude_t);
	  
	  // Get vector from observer to target
	  this.x = X_t - X_o;
	  this.y = Y_t - Y_o;
	  this.z = Z_t - Z_o;
	  	  
	  // Initialize the object finally
	  this.normalize();
  }

  public static GeocentricCoordinates getInstanceLatLong(Date time, LatLong observer, LatLong target) {
	    return getInstanceLatLong(time, observer.latitude, observer.longitude, target.latitude, target.longitude);
  }

  public static GeocentricCoordinates getInstanceLatLong(Date time, float latitude_o, float longitude_o, float latitude_t, float longitude_t) {
	    GeocentricCoordinates coords = new GeocentricCoordinates(0.0f, 0.0f, 0.0f);
	    coords.updateFromLatLong(time, latitude_o, longitude_o, latitude_t, longitude_t);
	    return coords;
  }

  /** Recomputes the x, y, and z variables in this class based on the specified
   * {@link RaDec}.
   */
  public void updateFromRaDec(RaDec raDec) {
    updateFromRaDec(raDec.ra, raDec.dec);
  }

  private void updateFromRaDec(float ra, float dec) {
    float raRadians = ra * Geometry.DEGREES_TO_RADIANS;
    float decRadians = dec * Geometry.DEGREES_TO_RADIANS;

    this.x = FloatMath.cos(raRadians) * FloatMath.cos(decRadians);
    this.y = FloatMath.sin(raRadians) * FloatMath.cos(decRadians);
    this.z = FloatMath.sin(decRadians);
  }

  /**
   * Convert ra and dec to x,y,z where the point is place on the unit sphere.
   */
  public static GeocentricCoordinates getInstance(RaDec raDec) {
    return getInstance(raDec.ra, raDec.dec);
  }

  public static GeocentricCoordinates getInstance(float ra, float dec) {
    GeocentricCoordinates coords = new GeocentricCoordinates(0.0f, 0.0f, 0.0f);
    coords.updateFromRaDec(ra, dec);
    return coords;
  }

  /**
   * Convert ra and dec to x,y,z where the point is place on the unit sphere.
   */
  public static GeocentricCoordinates getInstanceFromFloatArray(float[] xyz) {
    return new GeocentricCoordinates(xyz[0], xyz[1], xyz[2]);
  }

  @Override
  public float[] toFloatArray() {
    return new float[] {x, y, z};
  }

  /**
   * Assumes it's an array of length 3.
   * @param xyz
   */
  public void updateFromFloatArray(float[] xyz) {
    this.x = xyz[0];
    this.y = xyz[1];
    this.z = xyz[2];
  }

  public void updateFromVector3(Vector3 v) {
    this.x = v.x;
    this.y = v.y;
    this.z = v.z;
  }

  @Override
  public GeocentricCoordinates copy() {
    return new GeocentricCoordinates(x, y, z);
  }

  public static GeocentricCoordinates getInstanceFromVector3(Vector3 v) {
    return new GeocentricCoordinates(v.x, v.y, v.z);
  }
}