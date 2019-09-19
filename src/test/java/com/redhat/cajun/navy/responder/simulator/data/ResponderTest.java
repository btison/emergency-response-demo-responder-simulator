package com.redhat.cajun.navy.responder.simulator.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

public class ResponderTest {

   /**
     *  Test description:
     *
     *    When :
     *      The distance between the current location and the next step is between the reference_distance/1.3 and reference_distance*1.3
     *
     *    Then:
     *      The next step remains unchanged
     */
    @Test
    public void testCalculateNextLocation() {

        MissionStep step1 = new MissionStep();
        step1.setLat(33.0100);
        step1.setLon(-77.0100);
        step1.setWayPoint(false);
        step1.setDestination(false);

        LinkedList<MissionStep> steps = new LinkedList<>();
        steps.add(step1);

        Responder responder = new Responder();
        responder.setQueue(steps);
        responder.setCurrentLat(33.0000);
        responder.setCurrentLon(-77.0000);
        responder.setDistanceUnit(1500.0);

        responder.calculateNextLocation();
        assertThat(responder.getQueue().size(), equalTo(1));
        assertThat(responder.getQueue().peek().getLat(), equalTo(33.0100));
        assertThat(responder.getQueue().peek().getLon(), equalTo(-77.0100));
    }

    /**
     *  Test description:
     *
     *    When :
     *      The distance between the current location and the next step is greater than refdistance*1.3
     *
     *    Then:
     *      A new step is added to the head of the steps queue
     *      The distance between the current location and the next step is approx. equal to the reference distance
     */
    @Test
    public void testCalculateNextLocation2() {

        MissionStep step1 = new MissionStep();
        step1.setLat(33.0300);
        step1.setLon(-77.0100);
        step1.setWayPoint(false);
        step1.setDestination(false);

        LinkedList<MissionStep> steps = new LinkedList<>();
        steps.add(step1);

        Responder responder = new Responder();
        responder.setQueue(steps);
        responder.setCurrentLat(33.0000);
        responder.setCurrentLon(-77.0000);
        responder.setDistanceUnit(1500.0);

        responder.calculateNextLocation();
        assertThat(responder.getQueue().size(), equalTo(2));
        MissionStep step = responder.getQueue().poll();
        assertThat(step.isDestination(), equalTo(false));
        assertThat(step.isWayPoint(), equalTo(false));
        double distance = DistanceHelper.calculateDistance(33.0000, -77.0000, step.getLat(),step.getLon());
        assertThat(distance, IsCloseTo.closeTo(1500.0, 1.0));
    }

    /**
     *  Test description:
     *
     *    When :
     *      The distance between the current location and the next step is smaller than the reference_distance/1.3
     *      The distance between the current destination and the second next step is between the reference_distance/1.3 and reference_distance*1.3
     *
     *    Then:
     *      The next step is the second next step
     */
    @Test
    public void testCalculateNextLocation3() {

        MissionStep step1 = new MissionStep();
        step1.setLat(33.0020);
        step1.setLon(-77.0020);
        step1.setWayPoint(false);
        step1.setDestination(false);

        MissionStep step2 = new MissionStep();
        step2.setLat(33.0110);
        step2.setLon(-77.0100);
        step2.setWayPoint(false);
        step2.setDestination(false);

        LinkedList<MissionStep> steps = new LinkedList<>();
        steps.add(step1);
        steps.add(step2);

        Responder responder = new Responder();
        responder.setQueue(steps);
        responder.setCurrentLat(33.0000);
        responder.setCurrentLon(-77.0000);
        responder.setDistanceUnit(1500.0);

        responder.calculateNextLocation();
        assertThat(responder.getQueue().size(), equalTo(1));
        assertThat(responder.getQueue().peek().getLat(), equalTo(step2.getLat()));
        assertThat(responder.getQueue().peek().getLon(), equalTo(step2.getLon()));
    }

    /**
     *  Test description:
     *
     *    When :
     *      The distance between the current location and the next step is smaller than the reference_distance/1.3
     *      The distance between the next step and the second next step is smaller than the reference_distance/1.3
     *      The distance between the current destination and the third next step is between the reference_distance/1.3 and reference_distance*1.3
     *
     *    Then:
     *      The next step is the second next step
     */
    @Test
    public void testCalculateNextLocation4() {

        MissionStep step1 = new MissionStep();
        step1.setLat(33.0020);
        step1.setLon(-77.0020);
        step1.setWayPoint(false);
        step1.setDestination(false);

        MissionStep step2 = new MissionStep();
        step2.setLat(33.0040);
        step2.setLon(-77.0040);
        step2.setWayPoint(false);
        step2.setDestination(false);

        MissionStep step3 = new MissionStep();
        step3.setLat(33.0100);
        step3.setLon(-77.0100);
        step3.setWayPoint(false);
        step3.setDestination(false);

        LinkedList<MissionStep> steps = new LinkedList<>();
        steps.add(step1);
        steps.add(step2);
        steps.add(step3);

        Responder responder = new Responder();
        responder.setQueue(steps);
        responder.setCurrentLat(33.0000);
        responder.setCurrentLon(-77.0000);
        responder.setDistanceUnit(1500.0);

        responder.calculateNextLocation();
        assertThat(responder.getQueue().size(), equalTo(1));
        assertThat(responder.getQueue().peek().getLat(), equalTo(step3.getLat()));
        assertThat(responder.getQueue().peek().getLon(), equalTo(step3.getLon()));
    }

    /**
     *  Test description:
     *
     *    When :
     *      The distance between the current location and the next step is smaller than the reference_distance/1.3
     *      The distance between the next step and the second next step is smaller than the reference_distance/1.3
     *      The distance between the current destination and the third next step is greater than the reference_distance*1.3
     *
     *    Then:
     *      A new step is added before the third next step at the head of the queue
     *      The distance between the current location and the next step is approx. equal to the reference distance
     */
    @Test
    public void testCalculateNextLocation5() {

        MissionStep step1 = new MissionStep();
        step1.setLat(33.0020);
        step1.setLon(-77.0020);
        step1.setWayPoint(false);
        step1.setDestination(false);

        MissionStep step2 = new MissionStep();
        step2.setLat(33.0040);
        step2.setLon(-77.0040);
        step2.setWayPoint(false);
        step2.setDestination(false);

        MissionStep step3 = new MissionStep();
        step3.setLat(33.0200);
        step3.setLon(-77.0200);
        step3.setWayPoint(false);
        step3.setDestination(false);

        LinkedList<MissionStep> steps = new LinkedList<>();
        steps.add(step1);
        steps.add(step2);
        steps.add(step3);

        Responder responder = new Responder();
        responder.setQueue(steps);
        responder.setCurrentLat(33.0000);
        responder.setCurrentLon(-77.0000);
        responder.setDistanceUnit(1500.0);

        responder.calculateNextLocation();
        assertThat(responder.getQueue().size(), equalTo(2));

        double distance1 = DistanceHelper.calculateDistance(33.0000, -77.0000, step1.getLat(), step1.getLon());
        double distance2 = DistanceHelper.calculateDistance(step1.getLat(), step1.getLon(), step2.getLat(), step2.getLon());
        double distance3 = DistanceHelper.calculateDistance(step2.getLat(), step2.getLon(), responder.getQueue().peek().getLat(), responder.getQueue().peek().getLon());

        double distance = distance1 + distance2 + distance3;
        assertThat(distance, IsCloseTo.closeTo(1500.0, 10.0));
    }

    /**
     *  Test description:
     *
     *    When :
     *      The distance between the current location and the next step is smaller than the reference_distance/1.3
     *      The next step is a waypoint
     *
     *    Then:
     *      The next step remains unchanged
     */
    @Test
    public void testCalculateNextLocation6() {

        MissionStep step1 = new MissionStep();
        step1.setLat(33.0020);
        step1.setLon(-77.0020);
        step1.setWayPoint(true);
        step1.setDestination(false);

        MissionStep step2 = new MissionStep();
        step2.setLat(33.0110);
        step2.setLon(-77.0100);
        step2.setWayPoint(true);
        step2.setDestination(false);

        LinkedList<MissionStep> steps = new LinkedList<>();
        steps.add(step1);
        steps.add(step2);

        Responder responder = new Responder();
        responder.setQueue(steps);
        responder.setCurrentLat(33.0000);
        responder.setCurrentLon(-77.0000);
        responder.setDistanceUnit(1500.0);

        responder.calculateNextLocation();
        assertThat(responder.getQueue().size(), equalTo(2));
        assertThat(responder.getQueue().peek().getLat(), equalTo(step1.getLat()));
        assertThat(responder.getQueue().peek().getLon(), equalTo(step1.getLon()));
    }

    /**
     *  Test description:
     *
     *    When :
     *      The distance between the current location and the next step is smaller than the reference_distance/1.3
     *      The next step is a destination
     *
     *    Then:
     *      The next step remains unchanged
     */
    @Test
    public void testCalculateNextLocation7() {

        MissionStep step1 = new MissionStep();
        step1.setLat(33.0020);
        step1.setLon(-77.0020);
        step1.setWayPoint(false);
        step1.setDestination(true);

        MissionStep step2 = new MissionStep();
        step2.setLat(33.0110);
        step2.setLon(-77.0100);
        step2.setWayPoint(true);
        step2.setDestination(false);

        LinkedList<MissionStep> steps = new LinkedList<>();
        steps.add(step1);
        steps.add(step2);

        Responder responder = new Responder();
        responder.setQueue(steps);
        responder.setCurrentLat(33.0000);
        responder.setCurrentLon(-77.0000);
        responder.setDistanceUnit(1500.0);

        responder.calculateNextLocation();
        assertThat(responder.getQueue().size(), equalTo(2));
        assertThat(responder.getQueue().peek().getLat(), equalTo(step1.getLat()));
        assertThat(responder.getQueue().peek().getLon(), equalTo(step1.getLon()));
    }


    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
