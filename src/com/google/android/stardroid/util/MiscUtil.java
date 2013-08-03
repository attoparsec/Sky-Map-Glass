// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.stardroid.util;

import com.google.android.stardroid.ApplicationConstants;

/**
 * A collection of miscellaneous utility functions.
 * 
 * @author Brent Bryan
 */
public class MiscUtil {
  private MiscUtil() {}
  
  /** Returns the Tag for a class to be used in Android logging statements */
  public static String getTag(Object o) {
    if (o instanceof Class<?>) {
      return ApplicationConstants.APP_NAME + "." + ((Class<?>)o).getSimpleName();
    }
    return ApplicationConstants.APP_NAME + "." + o.getClass().getSimpleName();
  }
}