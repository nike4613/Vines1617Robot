/*
 * Copyright (c) 2016 Vines High School Robotics Team
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

package org.vinesrobotics.bot.hardware.controllers;

import org.vinesrobotics.bot.hardware.controllers.enums.Button;
import org.vinesrobotics.bot.hardware.controllers.enums.Joystick;
import org.vinesrobotics.bot.utils.Axis;
import org.vinesrobotics.bot.utils.Vec2D;

import java.util.HashMap;
import java.util.Map;

/**
 * An immutable object to hold the state of a controller at any given point
 */
public class ControllerState {
    // the joystick map
    private HashMap<String, Float> joys = new HashMap<>();
    // the button map
    private HashMap<String,ButtonState> buttons = new HashMap<>();
    // a reference to the {@link Controller} to get the information from
    private Controller control = null;

    // the previous state (last cycle). Used for edge detection on inputs.
    protected ControllerState prev;

    /**
     * Gets the previous ControllerState
     *
     * @return the last ControllerState
     */
    public ControllerState last() { return prev; }

    /**
     * Initializes the controller state to controller cntr
     *
     * @param cntr Controller to init to
     */
    protected ControllerState(Controller cntr) {
        control = cntr;
        update();
    }

    /**
     * Clones the specified ControllerState
     *
     * @param dup the state to clone
     */
    private ControllerState(ControllerState dup) {
        for (Map.Entry<String,Float> e : dup.joys.entrySet()) {
            joys.put(e.getKey(),e.getValue());
        }
        for (Map.Entry<String,ButtonState> e : dup.buttons.entrySet()) {
            buttons.put(e.getKey(),e.getValue());
        }
    }

    /**
     * Checks if given button is pressed.
     *
     * @param btn {@link Button} to check
     * @return true if button is pressed
     */
    public boolean isPressed(Button btn) {
        return btnVal(btn) > 0;
    }

    /**
     * Returns value of button btn
     *
     * @param btn {@link Button} to get the value ov
     * @return value of btn
     */
    public double btnVal(Button btn) { return buttons.get(btn.name()).value; }

    /**
     * Gets the value for particular joystick/axis pair
     *
     * @param joystick {@link Joystick} to check
     * @param ax {@link Axis} to check
     * @return value of axis
     */
    public double joyVal(Joystick joystick, Axis ax) {
        return joys.get(joystick.name()+ax.name());
    }

    /**
     * Gets the axis values of {@link Joystick}.
     *
     * @param j {@link Joystick} to check
     * @return Value of {@link Joystick}
     */
    public Vec2D<Double> joy(Joystick j) {
        return new Vec2D<>( joyVal(j,Axis.X), joyVal(j,Axis.Y) );
    }

    /**
     * Updates state values with those from the {@link Controller}.
     */
    protected void update() {
        if (control == null) return;

        for (Button btn : Button.values()) {
            buttons.put( btn.name(), control.getButton(btn) );
        }
        for (Joystick joy : Joystick.values()) {
            for (Axis ax : Axis.values()) {
                joys.put(joy.name() + ax.name(), control.getJoystick(joy).getAxis(ax) );
            }
        }
    }

    /**
     * Clones this ControllerState, without preserving references.
     *
     * @return The new ControllerState.
     */
    public ControllerState clone() {
        return new ControllerState(this);
    }

}
