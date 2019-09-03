package com.redhat.cajun.navy.responder.simulator.data;

import org.junit.Test;

public class DistanceHelperTest {

    @Test
    public void testCalculateIntermediateCoordinate() {

        DistanceHelper.Coordinate c1 = new DistanceHelper.Coordinate(33, -77);
        DistanceHelper.Coordinate c2 = new DistanceHelper.Coordinate(32, -78);

        DistanceHelper.Coordinate intermediate = DistanceHelper.calculateIntermediateCoordinate(c1, c2, 10000);
    }

}
