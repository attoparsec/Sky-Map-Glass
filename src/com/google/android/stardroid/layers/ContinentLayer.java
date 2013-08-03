package com.google.android.stardroid.layers;

import com.google.android.stardroid.R;
import com.google.android.stardroid.base.Closeables;
import com.google.android.stardroid.base.Lists;
import com.google.android.stardroid.base.TimeConstants;
import com.google.android.stardroid.control.AstronomerModel;
import com.google.android.stardroid.renderer.RendererObjectManager.UpdateType;
import com.google.android.stardroid.source.AbstractAstronomicalSource;
import com.google.android.stardroid.source.AstronomicalSource;
import com.google.android.stardroid.source.LineSource;
import com.google.android.stardroid.source.Sources;
import com.google.android.stardroid.source.TextSource;
import com.google.android.stardroid.source.impl.LineSourceImpl;
import com.google.android.stardroid.source.impl.TextSourceImpl;
import com.google.android.stardroid.source.proto.ProtobufAstronomicalSource;
import com.google.android.stardroid.source.proto.SourceProto.AstronomicalSourceProto;
import com.google.android.stardroid.source.proto.SourceProto.AstronomicalSourcesProto;
import com.google.android.stardroid.units.GeocentricCoordinates;
import com.google.android.stardroid.units.LatLong;
import com.google.android.stardroid.util.Blog;
import com.google.android.stardroid.util.MiscUtil;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Creates outlines of the continents
 *
 * @author Matthew Dockrey
 */
public class ContinentLayer extends AbstractSourceLayer {
  private static final String TAG = MiscUtil.getTag(ContinentLayer.class);
  private final AstronomerModel model;
  private final AssetManager assetManager;

  public ContinentLayer(AstronomerModel model, AssetManager assetManager, Resources resources) {
    super(resources, true);
    this.model = model;
    this.assetManager = assetManager;
  }

  @Override
  protected void initializeAstroSources(ArrayList<AstronomicalSource> sources) {
    sources.add(new ContinentSource(model, assetManager, getResources()));
  }

  @Override
  public int getLayerId() {
    return -110;
  }
  
  @Override
  public String getPreferenceId() {
    return "source_provider.10";
  }

  @Override
  public String getLayerName() {
    return "Continent";
  }

  @Override
  protected int getLayerNameId() {
    return R.string.show_continent_pref; 
  }

  /** Implementation of {@link AstronomicalSource} for the horizon source. */
  static class ContinentSource extends AbstractAstronomicalSource {
    // Due to a bug in the G1 rendering code text and lines render in different
    // colors.
    private static final int LINE_COLOR = Color.argb(120, 86, 176, 245);
    private static final long UPDATE_FREQ_MS = 1 * TimeConstants.MILLISECONDS_PER_SECOND;

    private ArrayList<List> latlongs = new ArrayList<List>();
    private final ArrayList<LineSource> lineSources = new ArrayList<LineSource>();
    private final AstronomerModel model;

    private long lastUpdateTimeMs = 0L;
    private int updated = 0;

    public ContinentSource(AstronomerModel model, AssetManager assetManager, Resources res) {
      this.model = model;

      InputStream coastlines = null;	
      try {
    	coastlines = assetManager.open("coastlines.dat", AssetManager.ACCESS_BUFFER);

        // Loop through input file
        // Add LatLongs for each row
        // Create new lineSource at element breaks
        BufferedReader in = new BufferedReader(new InputStreamReader(coastlines));
        String line = null;
        List<GeocentricCoordinates> verticesCelestial = null;
        List<LatLong> verticesTerrestial = null;
        int count = 0;

        while ((line = in.readLine()) != null) {
        	if (line.contains(">")) {
        		if (verticesCelestial != null) {
        			lineSources.add(new LineSourceImpl(LINE_COLOR, verticesCelestial, 1.5f));
        			latlongs.add(verticesTerrestial);
        		}

        		verticesCelestial = new ArrayList<GeocentricCoordinates>();        		
        		verticesTerrestial = new ArrayList<LatLong>();        		
        		count = 0;
        	} else {
        		if (count % 5 == 0) { // Too many vertices otherwise, they start getting dropped
        			String[] bits = line.split("\\s+");
        			verticesCelestial.add(new GeocentricCoordinates(0, 0, 0));        		
        			verticesTerrestial.add(new LatLong(Float.parseFloat(bits[1]), Float.parseFloat(bits[0])));
        		}
        		count++;
        	}
        }
        
		if (verticesCelestial != null) {
			lineSources.add(new LineSourceImpl(LINE_COLOR, verticesCelestial, 1.5f));
			latlongs.add(verticesTerrestial);
		}
      } catch (IOException e) {
        Log.e(TAG, "Unable to open coastlines.dat");
      } finally {
        Closeables.closeSilently(coastlines);
      }
      
    }

    private void updateCoords() {
      // Blog.d(this, "Updating Coords: " + (model.getTime().getTime() - lastUpdateTimeMs));

      this.lastUpdateTimeMs = model.getTime().getTime();

      for (int i = 0; i < lineSources.size(); i++)
      {
          List<GeocentricCoordinates> verticesCelestial = lineSources.get(i).getVertices();
          List<LatLong> verticesTerrestial = latlongs.get(i);
          
    	  for (int j = 0; j < verticesCelestial.size(); j++)
    	  {
    		  verticesCelestial.get(j).updateFromLatLong(model.getTime(), model.getLocation(), verticesTerrestial.get(j));
    	  }
      }
    }

    @Override
    public Sources initialize() {
      updateCoords();
      return this;
    }
    
    @Override
    public EnumSet<UpdateType> update() {
      EnumSet<UpdateType> updateTypes = EnumSet.noneOf(UpdateType.class);

      // TODO(brent): Add distance here.
      // Need this to run at first, or else the coordinates aren't adjusted to location/time properly
      // ...but then it just needlessly slows everything down
      // HACK!
      if (Math.abs(model.getTime().getTime() - lastUpdateTimeMs) > UPDATE_FREQ_MS && updated < 5) {
        updateCoords();
        updateTypes.add(UpdateType.UpdatePositions);
        updated++;
      }
      return updateTypes;
    }

    @Override
    public List<? extends LineSource> getLines() {
      return lineSources;
    }
  }
}
