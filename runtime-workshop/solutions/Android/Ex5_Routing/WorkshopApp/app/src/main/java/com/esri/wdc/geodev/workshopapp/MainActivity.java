package com.esri.wdc.geodev.workshopapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.AngularUnit;
import com.esri.arcgisruntime.geometry.AngularUnitId;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.ArcGISSceneLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.ElevationSource;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.DefaultSceneViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.GlobeCameraController;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.OrbitLocationCameraController;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.esri.arcgisruntime.util.ListenableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends Activity {

    // Exercise 5: Tag string for logging
    private static final String TAG = MainActivity.class.getSimpleName();

    // Exercise 1: Specify elevation service URL
    private static final String ELEVATION_IMAGE_SERVICE =
            "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";

    // Exercise 3: Specify operational layer paths
    private static final String MMPK_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/data/DC_Crime_Data.mmpk";
    private static final String SCENE_SERVICE_URL =
            "https://www.arcgis.com/home/item.html?id=a7419641a50e412c980cf242c29aa3c0";

    // Exercise 3: Permission request code for opening a mobile map package
    private static final int PERM_REQ_OPEN_MMPK = 1;

    // Exercise 4: Symbols for buffer and query
    private static final SimpleMarkerSymbol CLICK_SYMBOL =
            new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFffa500, 10);
    private static final SimpleFillSymbol BUFFER_SYMBOL =
            new SimpleFillSymbol(SimpleFillSymbol.Style.NULL, 0xFFFFFFFF,
                    new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFFA500, 3));

    // Exercise 5: Symbols for routing
    private static final SimpleMarkerSymbol ROUTE_ORIGIN_SYMBOL =
            new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.TRIANGLE, 0xFF00FF00, 10);
    private static final SimpleMarkerSymbol ROUTE_DESTINATION_SYMBOL =
            new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, 0xFFFF0000, 10);
    private static final SimpleLineSymbol ROUTE_LINE_SYMBOL =
            new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF550055, 5);

    // Exercise 1: Declare and instantiate fields
    private MapView mapView = null;
    private ArcGISMap map = new ArcGISMap();
    private SceneView sceneView = null;
    private ImageButton imageButton_toggle2d3d = null;
    private boolean threeD = false;

    // Exercise 2: Declare fields
    private ImageButton imageButton_lockFocus = null;

    // Exercise 4: Declare fields
    private ImageButton imageButton_bufferAndQuery = null;
    private final GraphicsOverlay bufferAndQueryMapGraphics = new GraphicsOverlay();

    // Exercise 5: Declare fields
    private final GraphicsOverlay mapRouteGraphics = new GraphicsOverlay();
    private final GraphicsOverlay sceneRouteGraphics = new GraphicsOverlay();
    private Point originPoint = null;
    private ImageButton imageButton_routing = null;
    private RouteTask routeTask = null;
    private RouteParameters routeParameters = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Exercise 1: Set up the 2D map.
        mapView = findViewById(R.id.mapView);
        map.setBasemap(Basemap.createTopographicVector());
        mapView.setMap(map);

        // Exercise 1: Set up the 3D scene.
        sceneView = findViewById(R.id.sceneView);
        ArrayList<ElevationSource> sources = new ArrayList<>();
        sources.add(new ArcGISTiledElevationSource(ELEVATION_IMAGE_SERVICE));
        ArcGISScene scene = new ArcGISScene(Basemap.createImagery(), new Surface(sources));
        sceneView.setScene(scene);

        // Exercise 3: Load scene layer
        final ArcGISSceneLayer sceneLayer = new ArcGISSceneLayer(SCENE_SERVICE_URL);
        sceneLayer.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                sceneView.setViewpoint(new Viewpoint(sceneLayer.getFullExtent()));
                Viewpoint viewpoint = sceneView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE);
                Point targetPoint = (Point) viewpoint.getTargetGeometry();
                Camera camera = sceneView.getCurrentViewpointCamera()
                        .rotateAround(targetPoint, 45.0, 65.0, 0.0);
                sceneView.setViewpointCameraAsync(camera);
            }
        });
        scene.getOperationalLayers().add(sceneLayer);

        // Exercise 2: Set fields.
        imageButton_lockFocus = findViewById(R.id.imageButton_lockFocus);

        // Exercise 3: Check READ_EXTERNAL_STORAGE permission for mobile map package.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            loadMobileMapPackage();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERM_REQ_OPEN_MMPK);
        }

        // Exercise 4: Set up buffer and query
        imageButton_bufferAndQuery = findViewById(R.id.imageButton_bufferAndQuery);
        mapView.getGraphicsOverlays().add(bufferAndQueryMapGraphics);

        // Exercise 5: Set up routing
        mapView.getGraphicsOverlays().add(mapRouteGraphics);
        sceneRouteGraphics.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.DRAPED);
        sceneView.getGraphicsOverlays().add(sceneRouteGraphics);
        imageButton_routing = findViewById(R.id.imageButton_routing);
        routeTask = new RouteTask(this, "http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World");
        // Don't share this code without removing plain text username and password!!!
        routeTask.setCredential(new UserCredential("theUsername", "thePassword"));
        try {
            routeParameters = routeTask.createDefaultParametersAsync().get();
            if (null != routeParameters) {
                routeParameters.setReturnDirections(false);
                routeParameters.setReturnRoutes(true);
                routeParameters.setReturnStops(false);
            } else {
                imageButton_routing.setEnabled(false);
            }
        } catch (InterruptedException | ExecutionException e) {
            routeTask = null;
            Log.e(TAG, "Could not get route parameters", e);
        }
    }

    /**
     * Exercise 1: Resume the MapView and SceneView when the Activity resumes.
     */
    @Override
    protected void onResume() {
        if (null != mapView) {
            mapView.resume();
        }
        if (null != sceneView) {
            sceneView.resume();
        }
        super.onResume();
    }

    /**
     * Exercise 1: Pause the MapView and SceneView when the Activity pauses.
     */
    @Override
    protected void onPause() {
        if (null != mapView) {
            mapView.pause();
        }
        if (null != sceneView) {
            sceneView.pause();
        }
        super.onPause();
    }

    /**
     * Exercise 1: Dispose the MapView and SceneView when the Activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        if (null != mapView) {
            mapView.dispose();
        }
        if (null != sceneView) {
            sceneView.dispose();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PERM_REQ_OPEN_MMPK == requestCode) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission) && PackageManager.PERMISSION_GRANTED == grantResults[i]) {
                    loadMobileMapPackage();
                    break;
                }
            }
        }
    }

    /**
     * Exercise 1: Toggle between 2D map and 3D scene.
     */
    public void imageButton_toggle2d3d_onClick(View view) {
        threeD = !threeD;
        setWeight(mapView, threeD ? 1f : 0f);
        setWeight(sceneView, threeD ? 0f : 1f);
        if (null == imageButton_toggle2d3d) {
            imageButton_toggle2d3d = findViewById(R.id.imageButton_toggle2d3d);
        }
        imageButton_toggle2d3d.setImageResource(threeD ? R.drawable.two_d : R.drawable.three_d);
    }

    /**
     * Exercise 2: Listener for zoom in button.
     */
    public void imageButton_zoomIn_onClick(View view) {
        zoom(2.0);
    }

    /**
     * Exercise 2: Listener for zoom out button.
     */
    public void imageButton_zoomOut_onClick(View view) {
        zoom(0.5);
    }

    /**
     * Exercise 2: Listener for lock focus button.
     */
    public void imageButton_lockFocus_onClick(View view) {
        imageButton_lockFocus.setSelected(!imageButton_lockFocus.isSelected());
        if (imageButton_lockFocus.isSelected()) {
            Geometry target = getSceneTarget();
            if (target instanceof Point) {
                final Point targetPoint = (Point) target;
                final Camera currentCamera = sceneView.getCurrentViewpointCamera();
                Point currentCameraPoint = currentCamera.getLocation();
                if (null != currentCameraPoint) {
                    final double xyDistance = GeometryEngine.distanceGeodetic(targetPoint, currentCameraPoint,
                            new LinearUnit(LinearUnitId.METERS),
                            new AngularUnit(AngularUnitId.DEGREES),
                            GeodeticCurveType.GEODESIC
                    ).getDistance();
                    final double zDistance = currentCameraPoint.getZ();
                    final double distanceToTarget = Math.sqrt(Math.pow(xyDistance, 2.0) + Math.pow(zDistance, 2.0));
                    final OrbitLocationCameraController cameraController = new OrbitLocationCameraController(
                            (Point) target, distanceToTarget
                    );
                    cameraController.setCameraHeadingOffset(currentCamera.getHeading());
                    cameraController.setCameraPitchOffset(currentCamera.getPitch());
                    sceneView.setCameraController(cameraController);
                }
            }
        } else {
            sceneView.setCameraController(new GlobeCameraController());
        }
    }

    /**
     * Exercise 4: Buffer and query toggle button action
     */
    public void imageButton_bufferAndQuery_onClick(View view) {
        imageButton_bufferAndQuery.setSelected(!imageButton_bufferAndQuery.isSelected());
        if (imageButton_bufferAndQuery.isSelected()) {
            mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView) {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event) {
                    bufferAndQuery(event);
                    return true;
                }
            });

            // Exercise 5: Unselect routing button
            imageButton_routing.setSelected(false);
        } else {
            mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView));
            sceneView.setOnTouchListener(new DefaultSceneViewOnTouchListener(sceneView));
        }
    }

    /**
     * Exercise 5: Routing toggle button action
     */
    public void imageButton_routing_onClick(View view) {
        imageButton_routing.setSelected(!imageButton_routing.isSelected());
        final DefaultMapViewOnTouchListener mapListener;
        final DefaultSceneViewOnTouchListener sceneListener;
        if (imageButton_routing.isSelected()) {
            mapListener = new DefaultMapViewOnTouchListener(this, mapView) {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event) {
                    addStopToRoute(event);
                    return true;
                }
            };
            sceneListener = new DefaultSceneViewOnTouchListener(sceneView) {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event) {
                    addStopToRoute(event);
                    return true;
                }
            };
            imageButton_bufferAndQuery.setSelected(false);
        } else {
            mapListener = new DefaultMapViewOnTouchListener(this, mapView);
            sceneListener = new DefaultSceneViewOnTouchListener(sceneView);
        }
        mapView.setOnTouchListener(mapListener);
        sceneView.setOnTouchListener(sceneListener);
    }

    /**
     * Exercise 1: Set the weight of a View, e.g. to show or hide it.
     */
    private void setWeight(View view, float weight) {
        final ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).weight = weight;
        }
        view.setLayoutParams(params);
    }

    /**
     * Exercise 2: Zoom the 2D map.
     */
    private void zoomMap(double factor) {
        mapView.setViewpointScaleAsync(mapView.getMapScale() / factor);
    }

    /**
     * Exercise 2: Zoom the 3D scene.
     */
    private void zoomScene(double factor) {
        Geometry target = getSceneTarget();
        if (target instanceof Point) {
            Camera camera = sceneView.getCurrentViewpointCamera()
                    .zoomToward((Point) target, factor);
            sceneView.setViewpointCameraAsync(camera, 0.5f);
        } else {
            // This shouldn't happen, but in case it does...
            Logger.getLogger(MainActivity.class.getName()).log(Level.WARNING,
                    "SceneView.getCurrentViewpoint returned {0} instead of {1}",
                    new String[]{target.getClass().getName(), Point.class.getName()});
        }
    }

    /**
     * Exercise 2: Zoom by a factor.
     *
     * @param factor The zoom factor (0 to 1 to zoom out, > 1 to zoom in).
     */
    private void zoom(double factor) {
        if (threeD) {
            zoomScene(factor);
        } else {
            zoomMap(factor);
        }
    }

    /**
     * Exercise 2: Get the SceneView viewpoint target.
     */
    private Geometry getSceneTarget() {
        return sceneView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).getTargetGeometry();
    }

    /**
     * Exercise 3: Load the mobile map package.
     */
    private void loadMobileMapPackage() {
        final MobileMapPackage mmpk = new MobileMapPackage(MMPK_PATH);
        mmpk.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                List<ArcGISMap> maps = mmpk.getMaps();
                if (0 < maps.size()) {
                    map = maps.get(0);
                    mapView.setMap(map);
                    map.addDoneLoadingListener(new Runnable() {
                        @Override
                        public void run() {
                            Viewpoint viewpoint = map.getInitialViewpoint();
                            if (null != viewpoint) {
                                mapView.setViewpointAsync(viewpoint);
                            }
                        }
                    });
                }
                map.setBasemap(Basemap.createTopographicVector());
            }
        });
        mmpk.loadAsync();
    }

    /**
     * Exercise 4: Do the buffer and query.
     */
    private void bufferAndQuery(MotionEvent singleTapEvent) {
        Point geoPoint = getGeoPoint(singleTapEvent);
        geoPoint = (Point) GeometryEngine.project(geoPoint, SpatialReference.create(3857));
        Polygon buffer = GeometryEngine.buffer(geoPoint, 1000.0);
        ListenableList<Graphic> graphics = bufferAndQueryMapGraphics.getGraphics();
        graphics.clear();
        graphics.add(new Graphic(buffer, BUFFER_SYMBOL));
        graphics.add(new Graphic(geoPoint, CLICK_SYMBOL));

        QueryParameters query = new QueryParameters();
        query.setGeometry(buffer);
        LayerList operationalLayers = mapView.getMap().getOperationalLayers();
        for (Layer layer : operationalLayers) {
            if (layer instanceof FeatureLayer) {
                ((FeatureLayer) layer).selectFeaturesAsync(query, FeatureLayer.SelectionMode.NEW);
            }
        }
        ;
    }

    /**
     * Exercise 4: Convert screen point to map or scene point.
     */
    private Point getGeoPoint(MotionEvent singleTapEvent) {
        android.graphics.Point screenPoint = new android.graphics.Point(
                Math.round(singleTapEvent.getX()),
                Math.round(singleTapEvent.getY()));
        return threeD ?
                sceneView.screenToBaseSurface(screenPoint) :
                mapView.screenToLocation(screenPoint);
    }

    /**
     * Exercise 5: Add a stop to the route that may be calculated.
     */
    private void addStopToRoute(MotionEvent singleTapEvent) {
        final ListenableList<Graphic> graphics = (threeD ? sceneRouteGraphics : mapRouteGraphics).getGraphics();
        Point point = getGeoPoint(singleTapEvent);
        if (point.hasZ()) {
            point = new Point(point.getX(), point.getY(), point.getSpatialReference());
        }
        if (null == originPoint) {
            originPoint = point;
            graphics.clear();
            graphics.add(new Graphic(originPoint, ROUTE_ORIGIN_SYMBOL));
        } else {
            graphics.add(new Graphic(point, ROUTE_DESTINATION_SYMBOL));
            routeParameters.clearStops();
            routeParameters.setStops(Arrays.asList(new Stop(originPoint), new Stop(point)));
            final ListenableFuture<RouteResult> solveFuture = routeTask.solveRouteAsync(routeParameters);
            solveFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    RouteResult routeResult = null;
                    try {
                        routeResult = solveFuture.get();
                        if (0 < routeResult.getRoutes().size()) {
                            graphics.add(new Graphic(routeResult.getRoutes().get(0).getRouteGeometry(), ROUTE_LINE_SYMBOL));
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        Log.e(TAG, "Could not get solved route", e);
                    }
                }
            });
            originPoint = null;
        }
    }
}