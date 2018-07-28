package com.example.divided.arduinoRGBcontrol;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;

import java.util.Random;

public class RGBdiod {
    private int redColor;
    private int greenColor;
    private int blueColor;

    public RGBdiod(int redColor, int greenColor, int blueColor) {

        this.redColor = redColor;
        this.greenColor = greenColor;
        this.blueColor = blueColor;
    }

    public RGBdiod(int color) {

        this.redColor = (color & 0xFF0000) >> 16;
        this.greenColor = (color & 0xFF00) >> 8;
        this.blueColor = (color & 0xFF);
    }

    private int[] convertHexToRgb(int hexValue) {

        int[] RGB = {(hexValue & 0xFF0000) >> 16, (hexValue & 0xFF00) >> 8, (hexValue & 0xFF)};
        return RGB;
    }

    public int getRedColor() {

        return this.redColor;
    }

    public int getGreenColor() {

        return this.greenColor;
    }

    public int getBlueColor() {

        return this.blueColor;
    }

    public void setColor(int redColor, int blueColor, int greenColor) {

        this.redColor = redColor;
        this.greenColor = greenColor;
        this.blueColor = blueColor;
    }

    public void setColor(float[] HSV) {

        this.redColor = Color.red(Color.HSVToColor(HSV));
        this.greenColor = Color.green(Color.HSVToColor(HSV));
        this.blueColor = Color.blue(Color.HSVToColor(HSV));
    }

    public void setColor(int color) {

        this.redColor = (color & 0xFF0000) >> 16;
        this.greenColor = (color & 0xFF00) >> 8;
        this.blueColor = (color & 0xFF);
    }

    public int[] getColorRGB() {

        final int[] RGB = {this.redColor, this.greenColor, this.blueColor};
        return RGB;
    }

    public float[] getColorHSV() {

        float[] HSV = new float[3];
        Color.colorToHSV(Color.rgb(this.redColor, this.greenColor, this.blueColor), HSV);
        return HSV;
    }

    public int getColorInt() {
        return Color.rgb(this.redColor, this.greenColor, this.blueColor);
    }

    public RGBdiod getRandomColor() {

        final Random rand = new Random();
        return new RGBdiod(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
    }


    @TargetApi(Build.VERSION_CODES.O)
    public RGBdiod getDarkerColor(float darknessFactor) {

        final float[] HSV = new float[3];
        Color.RGBToHSV(this.redColor, this.greenColor, this.blueColor, HSV);
        HSV[2] *= darknessFactor;

        int[] RGB = convertHexToRgb(Color.HSVToColor(HSV));
        return new RGBdiod(RGB[0], RGB[1], RGB[2]);
    }


    public RGBdiod getColorWithInvertedLightness() {

        float[] hsvOfInputColor = new float[3];
        float[] hsvOfBlackColor = new float[3];
        Color.colorToHSV(Color.BLACK, hsvOfBlackColor);
        Color.RGBToHSV(this.redColor, this.greenColor, this.blueColor, hsvOfInputColor);
        hsvOfBlackColor[2] = Math.abs(hsvOfInputColor[2] - 1);

        int[] RGB = convertHexToRgb(Color.HSVToColor(hsvOfBlackColor));
        return new RGBdiod(RGB[0], RGB[1], RGB[2]);
    }

    public RGBdiod getColorWithModifiedSaturation(float saturationValue) {
        float[] HSV = new float[3];
        Color.RGBToHSV(this.redColor, this.greenColor, this.blueColor, HSV);
        HSV[1] = saturationValue * HSV[1];
        HSV[2] = 1f;

        int[] RGB = convertHexToRgb(Color.HSVToColor(HSV));
        return new RGBdiod(RGB[0], RGB[1], RGB[2]);
    }
}
