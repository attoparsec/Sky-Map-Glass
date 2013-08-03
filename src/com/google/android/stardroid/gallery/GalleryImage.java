// Copyright 2010 Google Inc.
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

package com.google.android.stardroid.gallery;

/**
 * Holds data about an image.
 *
 * @author John Taylor
 */
public class GalleryImage {
  public int imageId;
  public String name;
  public String searchTerm;

  public GalleryImage(int imageId, String name, String searchTerm) {
    this.imageId = imageId;
    this.name = name;
    this.searchTerm = searchTerm;
  }
}
