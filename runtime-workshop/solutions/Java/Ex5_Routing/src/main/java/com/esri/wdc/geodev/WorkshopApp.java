/** *****************************************************************************
 * Copyright 2016-2017 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ***************************************************************************** */
package com.esri.wdc.geodev;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.AngularUnit;
import com.esri.arcgisruntime.geometry.AngularUnitId;
import com.esri.arcgisruntime.geometry.GeodesicEllipseParameters;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.layers.ArcGISSceneLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.GlobeCameraController;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties.SurfacePlacement;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * This Application class demonstrates key features of ArcGIS Runtime 100.0.
 */
public class WorkshopApp extends Application {

    // Exercise 1: Specify elevation service URL
    private static final String ELEVATION_IMAGE_SERVICE
        = "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";

    // Exercise 3: Specify operational layer paths
    private static final String MMPK_PATH = "../../../data/DC_Crime_Data.mmpk";
    private static final String SCENE_SERVICE_URL
        = "https://www.arcgis.com/home/item.html?id=a7419641a50e412c980cf242c29aa3c0";

    // Exercise 4: Create symbols for click and buffer
    private static final SimpleMarkerSymbol CLICK_SYMBOL
        = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFffa500, 10);
    private static final SimpleFillSymbol BUFFER_SYMBOL
        = new SimpleFillSymbol(SimpleFillSymbol.Style.NULL, 0xFFFFFFFF,
            new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFFA500, 3));

    // Exercise 5: Create symbols for routing
    private static final SimpleMarkerSymbol ROUTE_ORIGIN_SYMBOL
        = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.TRIANGLE, 0xFF00FF00, 10);
    private static final SimpleMarkerSymbol ROUTE_DESTINATION_SYMBOL
        = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, 0xFFFF0000, 10);
    private static final SimpleLineSymbol ROUTE_LINE_SYMBOL
        = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF550055, 5);

    // Exercise 1: Declare and instantiate fields, including UI components
    private final MapView mapView = new MapView();
    private ArcGISMap map = new ArcGISMap();
    private final ImageView imageView_2d
        = new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/two_d.png")));
    private final ImageView imageView_3d
        = new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/three_d.png")));
    private final Button button_toggle2d3d = new Button(null, imageView_3d);
    private final AnchorPane anchorPane = new AnchorPane();
    private SceneView sceneView = null;
    private ArcGISScene scene = null;
    private boolean threeD = false;

    // Exercise 2: Declare UI components for zoom buttons
    private final ImageView imageView_zoomIn
        = new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/zoom_in.png")));
    private final ImageView imageView_zoomOut
        = new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/zoom_out.png")));
    private final ImageView imageView_lockFocus
        = new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/lock.png")));
    private final Button button_zoomIn = new Button(null, imageView_zoomIn);
    private final Button button_zoomOut = new Button(null, imageView_zoomOut);
    private final ToggleButton toggleButton_lockFocus = new ToggleButton(null, imageView_lockFocus);

    // Exercise 4: Declare UI component for location button
    private final ImageView imageView_location
        = new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/location.png")));
    private final ToggleButton toggleButton_bufferAndQuery = new ToggleButton(null, imageView_location);

    // Exercise 5: Declare UI component for routing button
    private final ImageView imageView_routing
        = new ImageView(new Image(WorkshopApp.class.getResourceAsStream("/resources/routing.png")));
    private final ToggleButton toggleButton_routing = new ToggleButton(null, imageView_routing);

    // Exercise 4: Declare buffer and query fields
    private final GraphicsOverlay bufferAndQueryMapGraphics = new GraphicsOverlay();

    // Exercise 5: Declare routing fields
    private RouteTask routeTask;
    private RouteParameters routeParameters;
    private final GraphicsOverlay mapRouteGraphics = new GraphicsOverlay();
    private final GraphicsOverlay sceneRouteGraphics = new GraphicsOverlay();
    private Point originPoint = null;

    /**
     * Default constructor for class.
     */
    public WorkshopApp() {
        super();

        // Exercise 1: Set up the 2D map, since we will display that first
        map.setBasemap(Basemap.createTopographicVector());
        mapView.setMap(map);

        // Exercise 1: Set the 2D/3D toggle button's action
        button_toggle2d3d.setOnAction(event -> button_toggle2d3d_onAction());

        // Exercise 2: Set the zoom buttons' and lock focus button's actions
        button_zoomIn.setOnAction(event -> button_zoomIn_onAction());
        button_zoomOut.setOnAction(event -> button_zoomOut_onAction());
        toggleButton_lockFocus.setOnAction(event -> toggleButton_lockFocus_onAction());

        /**
         * Exercise 2: Disable the lock focus toggle button until the scene is
         * set up.
         */
        toggleButton_lockFocus.setDisable(true);

        /**
         * Exercise 3: Open a mobile map package (.mmpk) and add its operational
         * layers to the map
         */
        final MobileMapPackage mmpk = new MobileMapPackage(MMPK_PATH);
        mmpk.addDoneLoadingListener(() -> {
            List<ArcGISMap> maps = mmpk.getMaps();
            if (0 < maps.size()) {
                map = maps.get(0);
                mapView.setMap(map);
            }
            map.setBasemap(Basemap.createTopographicVector());
        });
        mmpk.loadAsync();

        //Exercise 4: Add a GraphicsOverlay to the map for the click and buffer
        mapView.getGraphicsOverlays().add(bufferAndQueryMapGraphics);

        // Exercise 4: Set the buffer and query toggle button's action
        toggleButton_bufferAndQuery.setOnAction(event -> toggleButton_bufferAndQuery_onAction());

        // Exercise 5: Set the routing toggle button's action
        toggleButton_routing.setOnAction(event -> toggleButton_routing_onAction());

        /**
         * Exercise 5: Set up routing objects
         */
        mapView.getGraphicsOverlays().add(mapRouteGraphics);
        new Thread(() -> {
            RouteTask theRouteTask = new RouteTask("http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World");
            /**
             * Note: for ArcGIS Online routing, this tutorial uses a username
             * and password in the source code for simplicity. For security
             * reasons, you would not do it this way in a real app. Instead, you
             * would do one of the following: - Use an OAuth 2.0 user login -
             * Use an OAuth 2.0 app login - Challenge the user for credentials
             */
            // Don't share this code without removing plain text username and password!!!
            theRouteTask.setCredential(new UserCredential("myUsername", "myPassword"));
            RouteParameters theRouteParameters = null;
            try {
                theRouteParameters = (RouteParameters) theRouteTask.createDefaultParametersAsync().get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(WorkshopApp.class.getName()).log(Level.SEVERE, null, ex);
                theRouteTask = null;
            }
            routeTask = theRouteTask;
            routeParameters = theRouteParameters;
            if (null != routeParameters) {
                routeParameters.setReturnDirections(false);
                routeParameters.setReturnRoutes(true);
                routeParameters.setReturnStops(false);
            } else {
                toggleButton_routing.setDisable(true);
            }
        }).start();
    }

    @Override
    public void start(Stage primaryStage) {
        // Exercise 1: Place the MapView and 2D/3D toggle button in the UI
        AnchorPane.setLeftAnchor(mapView, 0.0);
        AnchorPane.setRightAnchor(mapView, 0.0);
        AnchorPane.setTopAnchor(mapView, 0.0);
        AnchorPane.setBottomAnchor(mapView, 0.0);
        AnchorPane.setRightAnchor(button_toggle2d3d, 15.0);
        AnchorPane.setBottomAnchor(button_toggle2d3d, 15.0);
        anchorPane.getChildren().addAll(mapView, button_toggle2d3d);

        // Exercise 2: Place the zoom and lock focus buttons in the UI
        AnchorPane.setRightAnchor(button_zoomOut, 15.0);
        AnchorPane.setBottomAnchor(button_zoomOut, 80.0);
        AnchorPane.setRightAnchor(button_zoomIn, 15.0);
        AnchorPane.setBottomAnchor(button_zoomIn, 145.0);
        AnchorPane.setRightAnchor(toggleButton_lockFocus, 90.0);
        AnchorPane.setBottomAnchor(toggleButton_lockFocus, 145.0);
        anchorPane.getChildren().addAll(button_zoomOut, button_zoomIn, toggleButton_lockFocus);

        // Exercise 4: Place the location button in the UI
        AnchorPane.setRightAnchor(toggleButton_bufferAndQuery, 90.0);
        AnchorPane.setBottomAnchor(toggleButton_bufferAndQuery, 15.0);
        anchorPane.getChildren().add(toggleButton_bufferAndQuery);

        // Exercise 5: Place the routing button in the UI
        AnchorPane.setRightAnchor(toggleButton_routing, 90.0);
        AnchorPane.setBottomAnchor(toggleButton_routing, 80.0);
        anchorPane.getChildren().add(toggleButton_routing);

        // Exercise 1: Finish displaying the UI
        // JavaFX Scene (unrelated to ArcGIS 3D scene)
        Scene javaFxScene = new Scene(anchorPane);
        primaryStage.setTitle("My first map application");
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.setScene(javaFxScene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Exercise 1: Dispose of the MapView and SceneView before exiting
        mapView.dispose();
        if (null != sceneView) {
            sceneView.dispose();
        }

        super.stop();
    }

    /**
     * Exercise 1: Toggle between 2D map and 3D scene
     */
    private void button_toggle2d3d_onAction() {
        threeD = !threeD;
        button_toggle2d3d.setGraphic(threeD ? imageView_2d : imageView_3d);

        // Exercise 5: Set originPoint to null to reset routing when switching between 2D and 3D
        originPoint = null;

        // Exercise 1: Switch between 2D map and 3D scene
        if (threeD) {
            if (null == scene) {
                // Set up the 3D scene. This only happens the first time the user switches to 3D.
                scene = new ArcGISScene();
                scene.setBasemap(Basemap.createImagery());
                Surface surface = new Surface();
                surface.getElevationSources().add(new ArcGISTiledElevationSource(ELEVATION_IMAGE_SERVICE));
                scene.setBaseSurface(surface);
                sceneView = new SceneView();

                // Exercise 3: Add a scene layer to the scene
                ArcGISSceneLayer sceneLayer = new ArcGISSceneLayer(SCENE_SERVICE_URL);
                sceneLayer.addDoneLoadingListener(() -> {
                    sceneView.setViewpoint(new Viewpoint(sceneLayer.getFullExtent()));
                    // Rotate the camera
                    Viewpoint viewpoint = sceneView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE);
                    Point targetPoint = (Point) viewpoint.getTargetGeometry();
                    Camera camera = sceneView.getCurrentViewpointCamera()
                        .rotateAround(targetPoint, 45.0, 65.0, 0.0);
                    sceneView.setViewpointCameraAsync(camera);
                });
                scene.getOperationalLayers().add(sceneLayer);

                // Exercise 5: Add a GraphicsOverlay to the scene for the routing
                sceneRouteGraphics.getSceneProperties().setSurfacePlacement(SurfacePlacement.DRAPED);
                sceneView.getGraphicsOverlays().add(sceneRouteGraphics);

                /**
                 * Exercise 5: The routing toggle button might already be
                 * selected. If so, we need to set the SceneView's event
                 * handlers.
                 */
                if (toggleButton_routing.isSelected()) {
                    sceneView.setOnMouseClicked(event -> addStopToRoute(event));
                }

                sceneView.setArcGISScene(scene);
                AnchorPane.setLeftAnchor(sceneView, 0.0);
                AnchorPane.setRightAnchor(sceneView, 0.0);
                AnchorPane.setTopAnchor(sceneView, 0.0);
                AnchorPane.setBottomAnchor(sceneView, 0.0);

                // Exercise 2: Enable the lock scene toggle button.
                toggleButton_lockFocus.setDisable(false);
            }
            anchorPane.getChildren().remove(mapView);
            anchorPane.getChildren().add(0, sceneView);
        } else {
            anchorPane.getChildren().remove(sceneView);
            anchorPane.getChildren().add(0, mapView);
        }
    }

    /**
     * Exercise 2: zoom in
     */
    private void button_zoomIn_onAction() {
        zoom(2.0);
    }

    /**
     * Exercise 2: zoom out
     */
    private void button_zoomOut_onAction() {
        zoom(0.5);
    }

    /**
     * Exercise 2: toggle lock focus
     */
    private void toggleButton_lockFocus_onAction() {
        if (toggleButton_lockFocus.isSelected()) {
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
     * Exercise 2: determine whether to call zoomMap or zoomScene
     */
    private void zoom(double factor) {
        if (threeD) {
            zoomScene(factor);
        } else {
            zoomMap(factor);
        }
    }

    /**
     * Exercise 2: Utility method for zooming the 2D map
     *
     * @param factor the zoom factor (greater than 1 to zoom in, less than 1 to
     * zoom out)
     */
    private void zoomMap(double factor) {
        mapView.setViewpointScaleAsync(mapView.getMapScale() / factor);
    }

    /**
     * Exercise 2: Get the SceneView viewpoint target.
     *
     * @return the SceneView viewpoint target.
     */
    private Geometry getSceneTarget() {
        return sceneView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).getTargetGeometry();
    }

    /**
     * Exercise 2: Utility method for zooming the 3D scene
     *
     * @param factor the zoom factor (greater than 1 to zoom in, less than 1 to
     * zoom out)
     */
    private void zoomScene(double factor) {
        Geometry target = getSceneTarget();
        if (target instanceof Point) {
            Camera camera = sceneView.getCurrentViewpointCamera()
                .zoomToward((Point) target, factor);
            sceneView.setViewpointCameraAsync(camera, 0.5f);
        } else {
            Logger.getLogger(WorkshopApp.class.getName()).log(Level.WARNING,
                "SceneView.getCurrentViewpoint returned {0} instead of {1}",
                new String[]{target.getClass().getName(), Point.class.getName()});
        }
    }

    /**
     * Exercise 4: Activate buffer and query
     */
    private void toggleButton_bufferAndQuery_onAction() {
        if (toggleButton_bufferAndQuery.isSelected()) {
            mapView.setOnMouseClicked(mouseEvent -> bufferAndQuery(mouseEvent));

            // Exercise 5: Unselect the routing button
            toggleButton_routing.setSelected(false);

        } else {
            mapView.setOnMouseClicked(null);
            if (null != sceneView) {
                sceneView.setOnMouseClicked(null);
            }
        }
    }

    /**
     * Exercise 4: Convert a MouseEvent to a geographic point in the MapView or
     * SceneView's spatial reference.
     *
     * @param event The MouseEvent.
     * @return A geographic point in the MapView or SceneView's spatial
     * reference.
     */
    private Point getGeoPoint(MouseEvent event) {
        Point2D screenPoint = new Point2D(event.getX(), event.getY());
        Point geoPoint = threeD
            ? sceneView.screenToBaseSurface(screenPoint)
            : mapView.screenToLocation(screenPoint);
        return geoPoint;
    }

    /**
     * Exercise 4: Buffer and query
     */
    private void bufferAndQuery(MouseEvent event) {
        if (MouseButton.PRIMARY.equals(event.getButton()) && event.isStillSincePress()) {
            Point geoPoint = getGeoPoint(event);
            // Buffer by 1000 meters
            GeodesicEllipseParameters params = new GeodesicEllipseParameters();
            params.setCenter(geoPoint);
            params.setGeometryType(GeometryType.POLYGON);
            params.setLinearUnit(new LinearUnit(LinearUnitId.METERS));
            params.setMaxPointCount(1000);
            params.setMaxSegmentLength(1.0);
            params.setSemiAxis1Length(1000.0);
            Polygon buffer = (Polygon) GeometryEngine.ellipseGeodesic(params);

            // Show click and buffer as graphics
            ListenableList<Graphic> graphics = bufferAndQueryMapGraphics.getGraphics();
            graphics.clear();
            graphics.add(new Graphic(buffer, BUFFER_SYMBOL));
            graphics.add(new Graphic(geoPoint, CLICK_SYMBOL));

            // Run the query
            QueryParameters query = new QueryParameters();
            query.setGeometry(buffer);
            LayerList operationalLayers = mapView.getMap().getOperationalLayers();
            operationalLayers.parallelStream().filter(
                layer -> layer instanceof FeatureLayer
            ).forEach(layer -> {
                ((FeatureLayer) layer).selectFeaturesAsync(query, FeatureLayer.SelectionMode.NEW);
            });
        }
    }

    /**
     * Exercise 5: Activate routing
     */
    private void toggleButton_routing_onAction() {
        if (toggleButton_routing.isSelected()) {
            mapView.setOnMouseClicked(mouseEvent -> addStopToRoute(mouseEvent));
            if (null != sceneView) {
                sceneView.setOnMouseClicked(mouseEvent -> addStopToRoute(mouseEvent));
            }
            toggleButton_bufferAndQuery.setSelected(false);
        } else {
            mapView.setOnMouseClicked(null);
            if (null != sceneView) {
                sceneView.setOnMouseClicked(null);
            }
        }
        originPoint = null;
    }

    /**
     * Exercise 5: Add a stop to the route, and calculate the route if we have
     * two stops.
     */
    private void addStopToRoute(MouseEvent event) {
        if (null != routeTask && MouseButton.PRIMARY.equals(event.getButton()) && event.isStillSincePress()) {
            ListenableList<Graphic> graphics = (threeD ? sceneRouteGraphics : mapRouteGraphics).getGraphics();
            Point point = getGeoPoint(event);
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
                ListenableFuture<RouteResult> solveFuture = routeTask.solveRouteAsync(routeParameters);
                solveFuture.addDoneListener(() -> {
                    try {
                        RouteResult routeResult = (RouteResult) solveFuture.get();
                        if (0 < routeResult.getRoutes().size()) {
                            graphics.add(new Graphic(routeResult.getRoutes().get(0).getRouteGeometry(), ROUTE_LINE_SYMBOL));
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        Logger.getLogger(WorkshopApp.class.getName()).log(Level.SEVERE, null, e);
                    }
                });
                // After running route...
                originPoint = null;
            }
        }
    }

    /**
     * Exercise 1: Main method that runs the app.
     *
     * @param args Command line arguments (none are expected for this app).
     */
    public static void main(String[] args) {
        launch(args);
    }

}
