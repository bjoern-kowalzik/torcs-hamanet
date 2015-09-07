package torcs.simple;

import torcs.scr.Driver;
import torcs.scr.Action;
import torcs.scr.SensorModel;

/**
 * Simple controller as a starting point to develop your own one - accelerates
 * slowly - tries to maintain a constant speed (only accelerating, no braking) -
 * stays in first gear - steering follows the track and avoids to come too close
 * to the edges
 */
public class SimpleDriver extends Driver {

	// the constant speed that we try to keep
	final double targetSpeed = 50;

	// counting each time that control is called
	private int tickcounter = 0;

	public Action control(SensorModel sensorModel) {

		// adjust tick counter
		tickcounter++;

		// check, if we just started the race
		if (tickcounter == 1) {
			System.out.println("This is Simple Driver on track "
					+ getTrackName());
			System.out.println("This is a race "
					+ (damage ? "with" : "without") + " damage.");
		}

		// create new action object to send our commands to the server
		Action action = new Action();

		/*
		 * ----------------------- control velocity --------------------
		 */

		// simply accelerate until we reach our target speed.
		if (sensorModel.getSpeed() < targetSpeed) {
			action.accelerate = 1;
		}

		// don't brake
		action.brake = 0;

		// stay in first gear
		action.gear = 1;

		/*
		 * ----------------------- control steering ---------------------
		 */

		double trackAngle = sensorModel.getAngleToTrackAxis();
		double[] trackedgeSensors = sensorModel.getTrackEdgeSensors();
		double distanceLeft = trackedgeSensors[0];
		double distanceRight = trackedgeSensors[18];
		// System.out.println("trackAngle" + trackAngle);
		// System.out.println(Arrays.toString(trackedgeSensors));

		// follow the track
		action.steering = trackAngle * 0.75;

		// avoid to come too close to the edges
		if (distanceLeft < 3.0) {
			action.steering -= (5.0 - distanceLeft) * 0.05;
		}
		if (distanceRight < 3.0) {
			action.steering += (5.0 - distanceRight) * 0.05;
		}

		// return the action
		return action;
	}

	public void shutdown() {
		System.out.println("Bye bye!");
	}
}
