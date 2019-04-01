package de.fiduciagad.de.sft.adjustment.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fiduciagad.de.sft.adjustment.AdjustmentController;
import fiduciagad.de.sft.main.ConfiguratorValues;
import fiduciagad.de.sft.main.OpenCVHandler;

public class AdjustmentTest {

	@Test
	public void canConfigureColor() {

		OpenCVHandler cv = new OpenCVHandler();

		int hsvminh = -1;

		cv.setPythonModule("colorGrabber.py");
		cv.startPythonModule();
		cv.startTheAdjustment();

		assertThat(ConfiguratorValues.getColorHSVMinH(), not(hsvminh));
	}

	@Test
	public void canConvertOutputIntoRealValues() {

		// gamefieldSize 1000x500
		// cameraframeSize 600x600
		List<String> theOutput = new ArrayList<String>();

		theOutput.add("100,100,100,100,100,100");

		// upperLeftCornerBallPosXCam1,upperLeftCornerBallPosYCam1,upperLeftCornerBallPosXCam2,upperLeftCornerBallPosYCam2
		theOutput.add("10,40,-1,-1");

		// upperMiddleBallPosXCam1,upperMiddleBallPosYCam1,upperMiddleBallPosXCam2,upperMiddleBallPosYCam2
		theOutput.add("510,40,50,50");

		// upperRightCornerBallPosXCam1,upperRightCornerBallPosYCam1,upperRightCornerBallPosXCam2,upperRightCornerBallPosYCam2
		theOutput.add("-1,-1,550,50");

		// bottomRightCornerBallPosXCam1,bottomRightCornerBallPosYCam1,bottomRightCornerBallPosXCam2,bottomRightCornerBallPosYCam2
		theOutput.add("-1,-1,550,550");

		// ballsizeInPixel
		theOutput.add("30");

		AdjustmentController.convertOutputIntoValues(theOutput);

		assertThat(ConfiguratorValues.getColorHSVMinH(), is(100));

		assertThat(ConfiguratorValues.getXMaxOfGameField(), is(1000));
		assertThat(ConfiguratorValues.getYMaxOfGameField(), is(500));

		assertThat(ConfiguratorValues.getxOffsetCameraOne(), is(-10));
		assertThat(ConfiguratorValues.getyOffsetCameraOne(), is(-40));

		assertThat(ConfiguratorValues.getxOffsetCameraTwo(), is(450));
		assertThat(ConfiguratorValues.getyOffsetCameraTwo(), is(-50));

		assertThat(ConfiguratorValues.getMillimeterPerPixel(), is(1));

	}

}