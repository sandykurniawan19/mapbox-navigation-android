package com.mapbox.services.android.navigation.ui.v5.camera;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

/**
 * Updates the map camera while navigating.
 * <p>
 * This class listens to the progress of {@link MapboxNavigation} and moves
 * the {@link MapboxMap} camera based on the location updates.
 *
 * @since 0.6.0
 */
public class NavigationCamera implements ProgressChangeListener {

  private static final double CAMERA_TILT = 45;
  private static double CAMERA_ZOOM = 17;

  private MapboxMap mapboxMap;
  private MapboxNavigation navigation;
  private CameraPosition currentCameraPosition;
  private double targetDistance;
  private boolean trackingEnabled = true;

  /**
   * Creates an instance of {@link NavigationCamera}.
   *
   * @param view       for determining percentage of total screen
   * @param mapboxMap  for moving the camera
   * @param navigation for listening to location updates
   * @since 0.6.0
   */
  public NavigationCamera(@NonNull View view, @NonNull MapboxMap mapboxMap,
                          @NonNull MapboxNavigation navigation) {
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    initialize(view);
  }

  /**
   * Called when beginning navigation with a route.
   * <p>
   * Creates a {@link CameraPosition} based on the {@link DirectionsRoute}.
   * If the route is null, the {@link ProgressChangeListener} is still added so future updates aren't ignored.
   *
   * @param route used to create the camera position
   * @since 0.6.0
   */
  public void start(DirectionsRoute route) {
    if (route != null) {
      LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);

      double initialBearing = TurfMeasurement.bearing(
        Point.fromLngLat(
          lineString.getCoordinates().get(0).getLongitude(), lineString.getCoordinates().get(0).getLatitude()
        ),
        Point.fromLngLat(
          lineString.getCoordinates().get(1).getLongitude(), lineString.getCoordinates().get(1).getLatitude()
        )
      );

      Point targetPoint = TurfMeasurement.destination(
        Point.fromLngLat(
          lineString.getCoordinates().get(0).getLongitude(), lineString.getCoordinates().get(0).getLatitude()
        ),
        targetDistance, initialBearing, TurfConstants.UNIT_METERS
      );

      LatLng targetLatLng = new LatLng(
        targetPoint.latitude(),
        targetPoint.longitude()
      );

      createApproachAnimator(mapboxMap.getCameraPosition(), targetLatLng, CAMERA_ZOOM, initialBearing, CAMERA_TILT).start();
    }
  }

  /**
   * Called during rotation.
   * The camera should resume from the last location update, not the beginning of the route.
   * <p>
   * Creates a {@link CameraPosition} based on the {@link Location}.
   * If the route is null, the {@link ProgressChangeListener} is still added so future updates aren't ignored.
   *
   * @param location used to create the camera position
   * @since 0.6.0
   */
  public void resume(Location location) {
//    if (location != null) {
//      currentCameraPosition = buildCameraPositionFromLocation(location);
//      animateCameraToPosition(currentCameraPosition);
//    } else {
//      navigation.addProgressChangeListener(NavigationCamera.this);
//    }
  }

  /**
   * Used to update the camera position.
   * <p>
   * {@link Location} is also stored in case the user scrolls the map and the camera
   * will eventually need to return to that last location update.
   *
   * @param location      used to update the camera position
   * @param routeProgress ignored in this scenario
   * @since 0.6.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (location.getLongitude() != 0 && location.getLatitude() != 0) {

      // Target bearing
      double targetBearing = location.getBearing();
      Timber.d("Target bearing: " + targetBearing);

      double distanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
      Timber.d("Distance remaining: " + distanceRemaining);
      double targetTilt;
      if (distanceRemaining > 200) {
        targetTilt = CAMERA_TILT;
      } else if (distanceRemaining > 150) {
        targetTilt = 35;
      } else if (distanceRemaining > 100) {
        targetTilt = 25;
      } else if (distanceRemaining > 50) {
        targetTilt = 15;
      } else {
        targetTilt = 0;
      }

      LatLng targetLatLng = createTargetLatLng(location, targetTilt);

      createFollowAnimator(mapboxMap.getCameraPosition(), targetLatLng, CAMERA_ZOOM, targetBearing, targetTilt).start();
    }
  }

  @NonNull
  private LatLng createTargetLatLng(Location location, double targetTilt) {

    double modifiedTargetDistance = targetDistance;

    switch ((int) targetTilt) {
      case 35:
        modifiedTargetDistance = modifiedTargetDistance - (modifiedTargetDistance * .25);
        break;
      case 25:
        modifiedTargetDistance = modifiedTargetDistance - (modifiedTargetDistance * .50);
        break;
      case 15:
        modifiedTargetDistance = modifiedTargetDistance - (modifiedTargetDistance * .75);
        break;
      case 0:
        modifiedTargetDistance = 0;
        break;
      default:
        break;
    }

    Timber.d("Target distance: %s", modifiedTargetDistance);

    Point targetPoint = TurfMeasurement.destination(
      Point.fromLngLat(location.getLongitude(), location.getLatitude()),
      modifiedTargetDistance, location.getBearing(), TurfConstants.UNIT_METERS
    );

    return new LatLng(
      targetPoint.latitude(),
      targetPoint.longitude()
    );
  }

  /**
   * Setter for whether or not the camera should follow the location.
   *
   * @param trackingEnabled true if should track, false if should not
   * @since 0.6.0
   */
  public void setCameraTrackingLocation(boolean trackingEnabled) {
    this.trackingEnabled = trackingEnabled;
  }

  /**
   * Getter for current state of tracking.
   *
   * @return true if tracking, false if not
   * @since 0.6.0
   */
  public boolean isTrackingEnabled() {
    return trackingEnabled;
  }

  /**
   * Enables tracking and moves the camera to the last known location update
   * from the {@link ProgressChangeListener}.
   *
   * @since 0.6.0
   */
  public void resetCameraPosition() {
    this.trackingEnabled = true;
    if (currentCameraPosition != null) {
      mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition), 750, true);
    }
  }

  private Animator createFollowAnimator(CameraPosition currentPosition, LatLng targetLatLng, double targetZoom,
                                        double targetBearing, double targetTilt) {
    AnimatorSet animatorSet = new AnimatorSet();
    List<Animator> animators = new ArrayList<>();
    animators.add(createZoomAnimator(currentPosition.zoom, targetZoom, 1000));
    animators.add(createBearingAnimator(currentPosition.bearing, targetBearing));
    animators.add(createTiltAnimator(currentPosition.tilt, targetTilt));
    animators.add(createLatLngAnimator(currentPosition.target, targetLatLng, 1000));
    animatorSet.playTogether(animators);
    return animatorSet;
  }

  private Animator createApproachAnimator(CameraPosition currentPosition, LatLng targetLatLng, double targetZoom,
                                        double targetBearing, double targetTilt) {
    AnimatorSet animatorSet = new AnimatorSet();
    List<Animator> animators = new ArrayList<>();
    animators.add(createZoomAnimator(currentPosition.zoom, targetZoom, 3000));
    animators.add(createLatLngAnimator(currentPosition.target, targetLatLng, 3000));
//    animatorSet.add(createBearingAnimator(currentPosition.bearing, targetBearing));
//    animatorSet.add(createTiltAnimator(currentPosition.tilt, targetTilt));
    animatorSet.playSequentially(animators);
    animatorSet.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {

      }

      @Override
      public void onAnimationEnd(Animator animation) {
        navigation.addProgressChangeListener(NavigationCamera.this);

      }

      @Override
      public void onAnimationCancel(Animator animation) {
        navigation.addProgressChangeListener(NavigationCamera.this);
      }

      @Override
      public void onAnimationRepeat(Animator animation) {

      }
    });

    return animatorSet;
  }

  private Animator createLatLngAnimator(LatLng currentPosition, LatLng targetPosition, long duration) {
    // Create animator from current position to target position
    ValueAnimator latLngAnimator = ValueAnimator.ofObject(new LatLngEvaluator(), currentPosition, targetPosition);
    latLngAnimator.setDuration(duration);
    latLngAnimator.setInterpolator(new LinearInterpolator());
    latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.setLatLng((LatLng) animation.getAnimatedValue());
      }
    });
    return latLngAnimator;
  }

  private Animator createZoomAnimator(double currentZoom, double targetZoom, long duration) {
    ValueAnimator zoomAnimator = ValueAnimator.ofFloat((float) currentZoom, (float) targetZoom);
    zoomAnimator.setDuration(duration);
    zoomAnimator.setInterpolator(new LinearInterpolator());
    zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.setZoom((Float) animation.getAnimatedValue());
      }
    });
    return zoomAnimator;
  }

  private Animator createBearingAnimator(double currentBearing, double targetBearing) {

    float modifiedTargetBearing = normalizeBearing((float) currentBearing, (float) targetBearing);
    ValueAnimator bearingAnimator = ValueAnimator.ofFloat((float) currentBearing, modifiedTargetBearing);
    bearingAnimator.setDuration(1000);
    bearingAnimator.setInterpolator(new LinearInterpolator());
    bearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.setBearing((Float) animation.getAnimatedValue());
      }
    });
    return bearingAnimator;
  }

  private Animator createTiltAnimator(double currentTilt, double targetTilt) {
    ValueAnimator tiltAnimator = ValueAnimator.ofFloat((float) currentTilt, (float) targetTilt);
    tiltAnimator.setDuration(2500);
    tiltAnimator.setInterpolator(new DecelerateInterpolator());
    tiltAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.setTilt((Float) animation.getAnimatedValue());
      }
    });
    return tiltAnimator;
  }

  private static class LatLngEvaluator implements TypeEvaluator<LatLng> {

    private final LatLng latLng = new LatLng();

    @Override
    public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
      latLng.setLatitude(startValue.getLatitude()
        + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
      latLng.setLongitude(startValue.getLongitude()
        + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
      return latLng;
    }
  }

  private float normalizeBearing(float currentBearing, float targetBearing) {
    double diff = currentBearing - targetBearing;
    if (diff > 180.0f) {
      targetBearing += 360.0f;
    } else if (diff < -180.0f) {
      targetBearing -= 360.f;
    }
    return targetBearing;
  }

  /**
   * Initializes both the target distance and zoom level for the camera.
   *
   * @param view used for setting target distance / zoom level
   */
  private void initialize(View view) {
    initializeTargetDistance(view);
    initializeScreenOrientation(view.getContext());
  }

  /**
   * Defines the camera target distance given the percentage of the
   * total phone screen the view uses.
   * <p>
   * If the view takes up a smaller portion of the screen, the target distance needs
   * to be adjusted to accommodate.
   *
   * @param view used for calculating target distance
   */
  private void initializeTargetDistance(View view) {
    double viewHeight = (double) view.getHeight();
    double screenHeight = (double) view.getContext().getResources().getDisplayMetrics().heightPixels;
    targetDistance = (viewHeight / screenHeight) * 100;
  }

  /**
   * Defines the camera zoom level given the screen orientation.
   *
   * @param context used for getting current orientation
   */
  private void initializeScreenOrientation(Context context) {
    CAMERA_ZOOM = new OrientationMap().get(context.getResources().getConfiguration().orientation);
  }

  /**
   * Holds the two different screen orientations
   * and their corresponding zoom levels.
   */
  private static class OrientationMap extends SparseArray<Integer> {

    OrientationMap() {
      put(ORIENTATION_PORTRAIT, 17);
      put(ORIENTATION_LANDSCAPE, 16);
    }
  }
}
