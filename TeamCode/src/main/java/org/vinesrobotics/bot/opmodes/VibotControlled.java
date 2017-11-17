/*
 * Copyright (c) 2017 Vines High School Robotics Team
 *
 *                            Permission is hereby granted, free of charge, to any person obtaining a copy
 *                            of this software and associated documentation files (the "Software"), to deal
 *                            in the Software without restriction, including without limitation the rights
 *                            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *                            copies of the Software, and to permit persons to whom the Software is
 *                            furnished to do so, subject to the following conditions:
 *
 *                            The above copyright notice and this permission notice shall be included in all
 *                            copies or substantial portions of the Software.
 *
 *                            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *                            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *                            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *                            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *                            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *                            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *                            SOFTWARE.
 */

package org.vinesrobotics.bot.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.configuration.MotorConfigurationType;

import org.vinesrobotics.bot.R;
import org.vinesrobotics.bot.hardware.Hardware;
import org.vinesrobotics.bot.hardware.HardwareElement;
import org.vinesrobotics.bot.hardware.controllers.Controller;
import org.vinesrobotics.bot.hardware.controllers.ControllerState;
import org.vinesrobotics.bot.hardware.controllers.Controllers;
import org.vinesrobotics.bot.hardware.controllers.enums.Button;
import org.vinesrobotics.bot.hardware.controllers.enums.Joystick;
import org.vinesrobotics.bot.hardware.groups.MotorDeviceGroup;
import org.vinesrobotics.bot.hardware.groups.ServoDeviceGroup;
import org.vinesrobotics.bot.utils.Logging;
import org.vinesrobotics.bot.utils.Utils;
import org.vinesrobotics.bot.utils.Vec2D;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.util.List;


@TeleOp(name="Controlled (17)", group="Vines")
//@Disabled
public class VibotControlled extends OpMode {

    public MotorDeviceGroup leftMotors;
    public MotorDeviceGroup rightMotors;

    public DcMotor linSlide;
    MotorConfigurationType linSlideCfg;
    double linSlideMax = 4.5;
    double linSlideMin = 0;
    double linSlideSpeed = 2.25;
    double linSlideUnitMultiplier;

    public ServoDeviceGroup clawServos;

    public Hardware robot = new Hardware();

    @Override
    public void init() {
        Logging.setTelemetry(telemetry);
        try {
            robot.registerHardwareKeyName("intake");
            robot.registerHardwareKeyName("bumper");
            robot.registerHardwareKeyName("slide");
            robot.registerHardwareKeyName("claw");
        } catch (InvalidKeyException e) {}
        robot.initHardware(hardwareMap);

        List<HardwareElement> lefts = robot.getDevicesWithAllKeys("left","drive");
        leftMotors = new MotorDeviceGroup();
        try {
            for (HardwareElement he : lefts) {
                leftMotors.addDevice((DcMotor) he.get());
            }
            leftMotors.setDirection(DcMotor.Direction.FORWARD);
            leftMotors.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }catch (Exception e){}

        List<HardwareElement> right = robot.getDevicesWithAllKeys("right","drive");
        rightMotors = new MotorDeviceGroup();
        try {
            for (HardwareElement he : right) {
                rightMotors.addDevice((DcMotor)he.get());
            }
            rightMotors.setDirection(DcMotor.Direction.REVERSE);
            rightMotors.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }catch (Exception e){}

        List<HardwareElement> slides = robot.getDevicesWithAllKeys("motor", "slide");
        linSlide = (DcMotor) slides.get(0).get();
        linSlide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        linSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        linSlideCfg = linSlide.getMotorType();
        linSlideUnitMultiplier = linSlideCfg.getTicksPerRev();

        List<HardwareElement> claw = robot.getDevicesWithAllKeys("claw","servo");
        clawServos = new ServoDeviceGroup();
        try {
            for (HardwareElement he : right) {
                Servo serv = (Servo)he.get();
                clawServos.addDevice(serv);
                if (robot.hasKey(he, "right")) {
                    serv.setDirection(Servo.Direction.REVERSE);
                }
            }
        }catch (Exception e){}

        // old init_m
        controllers = Controllers.getControllerObjects(this);
        main_ct = controllers.a();


    }

    public void init_loop() {
        /*init_loop_m();*/
    }

    @Override
    public void start() {
        /*start_m();*/
        if (died) return;

        Utils.getDeltaTime(this.getRuntime());
    }

    private Exception error = null;
    private double ctime = 0;

    /**
     * User defined loop method
     * <p>
     * This method will be called repeatedly in x loop while this op mode is running
     */
    public void loop() {
        if (error != null) {
            ByteArrayOutputStream sw = new ByteArrayOutputStream();
            error.printStackTrace(new PrintStream(sw));
            telemetry.addLine(String.format("Error after %g seconds:%n%s", this.getRuntime(), sw.toString()));
            updateTelemetry(telemetry);
        } else {
            try {
                loop_m(Utils.getDeltaTime(this.getRuntime()));
            } catch (Exception e) {
                error = e;
            }
        }
    }

    @Override
    public void stop() {

    }

    private boolean died = false;

    Controllers controllers;
    Controller main_ct;

    /*
     * Code to run REPEATEDLY after the driver hits PLAY but before they hit STOP
     */
    // loop_m separation preserved to remove error checking dirtiness
    double clawPosition = 0.5;
    double slidePosition = linSlideMin;
    public void loop_m(double deltaTime) {
        if (died) return;

        ControllerState main = this.main_ct.getControllerState();

        Vec2D<Double> left = main.joy(Joystick.LEFT);
        Vec2D<Double> right = main.joy(Joystick.RIGHT);

        double lPower = left.y(),rPower = lPower;

        lPower *= left.x();
        rPower *= -left.x();

        leftMotors.setPower(lPower);
        rightMotors.setPower(rPower);


        double slidePower = 1; // power
        if (linSlide.getPower()!=slidePower) linSlide.setPower(slidePower);
        if (main.isPressed(Button.UP)) slidePosition += linSlideSpeed;
        if (main.isPressed(Button.DOWN)) slidePosition -= linSlideSpeed;
        if (slidePosition > linSlideMax) slidePosition = linSlideMax;
        if (slidePosition < linSlideMin) slidePosition = linSlideMin;
        int calcPos = (int)Math.round(slidePosition * linSlideUnitMultiplier);
        if (linSlide.getTargetPosition() != calcPos) linSlide.setTargetPosition(calcPos);


        if (main.isPressed(Button.RS) && main.isPressed(Button.LS)) main = null;

        // literal copy of Shields' code; there's a better way to do this
        double servo_speed = 50;
        if (main.isPressed(Button.A)) clawPosition += servo_speed*deltaTime;
        if (main.isPressed(Button.X)) clawPosition -= servo_speed*deltaTime;

        clawServos.setPosition(clawPosition);

        telemetry.addLine( "Values in range of -1 to +1" );
        telemetry.addData( "Speed", (-left.y()-right.y())/2 );
        telemetry.addData( "Turning Speed", (-left.y()+right.y())/2 );
        updateTelemetry(telemetry);

        try { // VuForiaKey
            Utils.getContext().getResources().getText(R.string.VuForiaKey);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}