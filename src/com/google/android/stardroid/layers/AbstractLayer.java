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

package com.google.android.stardroid.layers;

import com.google.android.stardroid.base.Maps;
import com.google.android.stardroid.renderer.RendererController;
import com.google.android.stardroid.renderer.RendererControllerBase;
import com.google.android.stardroid.renderer.RendererController.AtomicSection;
import com.google.android.stardroid.renderer.RendererControllerBase.RenderManager;
import com.google.android.stardroid.renderer.RendererObjectManager.UpdateType;
import com.google.android.stardroid.renderer.util.UpdateClosure;
import com.google.android.stardroid.search.SearchResult;
import com.google.android.stardroid.source.ImageSource;
import com.google.android.stardroid.source.LineSource;
import com.google.android.stardroid.source.PointSource;
import com.google.android.stardroid.source.TextSource;
import com.google.android.stardroid.util.Blog;
import com.google.android.stardroid.util.MiscUtil;

import android.content.res.Resources;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base implementation of the {@link Layer} interface.
 *
 * @author John Taylor
 * @author Brent Bryan
 */
public abstract class AbstractLayer implements Layer {
  private static final String TAG = MiscUtil.getTag(AbstractLayer.class);

  private final ReentrantLock renderMapLock = new ReentrantLock();
  private final HashMap<Class<?>, RenderManager<?>> renderMap = Maps.newHashMap();
  private final Resources resources;

  private RendererController renderer;

  public AbstractLayer(Resources resources) {
    this.resources = resources;
  }

  protected Resources getResources() {
    return resources;
  }

  @Override
  public void registerWithRenderer(RendererController rendererController) {
    this.renderMap.clear();
    this.renderer = rendererController;
    updateLayerForControllerChange();
  }

  protected abstract void updateLayerForControllerChange();

  @Override
  public void setVisible(boolean visible) {
    renderMapLock.lock();
    try {
      if (renderer == null) {
        Log.w(TAG, "Renderer not set - aborting " + this.getClass().getSimpleName());
        return;
      }

      AtomicSection atomic = renderer.createAtomic();
      for (Entry<Class<?>, RenderManager<?>> entry: renderMap.entrySet()) {
        entry.getValue().queueEnabled(visible, atomic);
      }
      renderer.queueAtomic(atomic);
    } finally {
      renderMapLock.unlock();
    }
  }

  protected void addUpdateClosure(UpdateClosure closure) {
    if (renderer != null) {
      renderer.addUpdateClosure(closure);
    }
  }

  protected void removeUpdateClosure(UpdateClosure closure) {
    if (renderer != null) {
      renderer.removeUpdateCallback(closure);
    }
  }

  /**
   * Forces a redraw of this layer, clearing all of the information about this
   * layer in the renderer and repopulating it.
   */
  protected abstract void redraw();


  protected void redraw(
      final ArrayList<TextSource> textSources,
      final ArrayList<PointSource> pointSources,
      final ArrayList<LineSource> lineSources,
      final ArrayList<ImageSource> imageSources) {
    redraw(textSources, pointSources, lineSources, imageSources, EnumSet.of(UpdateType.Reset));
  }

  /**
   * Updates the renderer (using the given {@link UpdateType}), with then given set of
   * UI elements.  Depending on the value of {@link UpdateType}, current sources will
   * either have their state updated, or will be overwritten by the given set
   * of UI elements.
   */
  protected void redraw(
      final ArrayList<TextSource> textSources,
      final ArrayList<PointSource> pointSources,
      final ArrayList<LineSource> lineSources,
      final ArrayList<ImageSource> imageSources,
      EnumSet<UpdateType> updateTypes) {

    // Log.d(TAG, getLayerName() + " Updating renderer: " + updateTypes);
    if (renderer == null) {
      Log.w(TAG, "Renderer not set - aborting: " + this.getClass().getSimpleName());
      return;
    }

    renderMapLock.lock();
    try {
      // Blog.d(this, "Redraw: " + updateTypes);
      AtomicSection atomic = renderer.createAtomic();
      setSources(textSources, updateTypes, TextSource.class, atomic);
      setSources(pointSources, updateTypes, PointSource.class, atomic);
      setSources(lineSources, updateTypes, LineSource.class, atomic);
      setSources(imageSources, updateTypes, ImageSource.class, atomic);
      renderer.queueAtomic(atomic);
    } finally {
      renderMapLock.unlock();
    }
  }

  /**
   * Sets the objects on the {@link RenderManager} to the given values,
   * creating (or disabling) the {@link RenderManager} if necessary.
   */
  private <E> void setSources(ArrayList<E> sources, EnumSet<UpdateType> updateType,
      Class<E> clazz, AtomicSection atomic) {

    @SuppressWarnings("unchecked")
    RenderManager<E> manager = (RenderManager<E>) renderMap.get(clazz);
    if (sources == null || sources.isEmpty()) {
      if (manager != null) {
        // TODO(brent): we should really just disable this layer, but in a
        // manner that it will automatically be reenabled when appropriate.
        Blog.d(this, "       " + clazz.getSimpleName());
        manager.queueObjects(Collections.<E>emptyList(), updateType, atomic);
      }
      return;
    }

    if (manager == null) {
      manager = createRenderManager(clazz, atomic);
      renderMap.put(clazz, manager);
    }
    // Blog.d(this, "       " + clazz.getSimpleName() + " " + sources.size());
    manager.queueObjects(sources, updateType, atomic);
  }

  @SuppressWarnings("unchecked")
  <E> RenderManager<E> createRenderManager(Class<E> clazz, RendererControllerBase controller) {
    if (clazz.equals(ImageSource.class)) {
      return (RenderManager<E>) controller.createImageManager(getLayerId());

    } else if (clazz.equals(TextSource.class)) {
      return (RenderManager<E>) controller.createLabelManager(getLayerId());

    } else if (clazz.equals(LineSource.class)) {
      return (RenderManager<E>) controller.createLineManager(getLayerId());

    } else if (clazz.equals(PointSource.class)) {
      return (RenderManager<E>) controller.createPointManager(getLayerId());
    }
    throw new IllegalStateException("Unknown source type: " + clazz);
  }

  @Override
  public List<SearchResult> searchByObjectName(String name) {
    // By default, layers will return no search results.
    // Override this if the layer should be searchable.
    return Collections.emptyList();
  }

  @Override
  public Set<String> getObjectNamesMatchingPrefix(String prefix) {
    // By default, layers will return no search results.
    // Override this if the layer should be searchable.
    return Collections.emptySet();
  }

  /**
   * Provides a string ID to the internationalized name of this layer.
   */
  // TODO(brent): could this be combined with getLayerId?  Not sure - they
  // serve slightly different purposes.
  protected abstract int getLayerNameId();

  @Override
  public String getPreferenceId() {
    return getPreferenceId(getLayerNameId());
  }

  protected String getPreferenceId(int layerNameId) {
    return "source_provider." + layerNameId;
  }

  @Override
  public String getLayerName() {
    return getStringFromId(getLayerNameId());
  }

  /**
   * Return an internationalized string from a string resource id.
   */
  protected String getStringFromId(int resourceId) {
    return resources.getString(resourceId);
  }
}
