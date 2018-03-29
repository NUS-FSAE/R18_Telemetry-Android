package nusfsae.r18telemetry;

import android.provider.ContactsContract;

/**
 * Created by FSAE on 12-Mar-18.
 */

public class DataStorage {
    private static final int CANID1 = 0x40;
    private static final int CANID2 = 0x41;
    private static final int CANID3 = 0x42;
    private static final int CANID4 = 0x43;

    public static Object lock1 = new Object();
    public static Object lock2 = new Object();
    public static Object lock3 = new Object();
    public static Object lock4 = new Object();

    private static int throttle;
    private static int brake;
    private static int gear;
    private static double rpm;
    private static int speed;
    private static int oilTemp;
    private static int airTemp;
    private static int engTemp;
    private static int fuelPressure;
    private static int oilPressure;
    private static int brakeTemp;
    private static int brakeBias;
    private static int tyreTempRRI;
    private static int tyreTempRRO;
    private static double batteryVolts;
    private static Boolean launchEnabled;
    private static Boolean autoShiftEnabled;
    private static Boolean clutchEngaged;
    private static Boolean radioActivated;

    public DataStorage() {
        rpm = 8.5;
        oilPressure = 509;
        fuelPressure = 341;
        throttle = 27;
        gear = 3;
        speed = 60;
        airTemp = 31;
        oilTemp = 75;
        engTemp = 79;
        batteryVolts = 13.2;
        brakeTemp = 81;
        brakeBias = 55;
    }

    //data with 2 byte length would need to "& 0xFF" to avoid becoming a negative value due to Java treating them as signed int

    public void insertData(byte[] data) {
        switch (data[0]) {
            case 0x40:
                synchronized (lock1) {
                    int buff = (data[1] << 8 | (data[2] & 0xFF))/100;
                    rpm = buff/10.0;
                    oilPressure = (data[3] << 8 | (data[4] & 0xFF))/10;
                    fuelPressure = (data[5] << 8 | (data[6] & 0xFF)) / 10;
                    throttle = data[7];
                    speed = data[8];
                }
                break;
            case 0x41:
                synchronized (lock2) {
                    brake = data[1];
                    brakeTemp = (data[3] << 8 | (data[4]&0xFF));
                    gear = data[7];
                    brakeBias = data[8];
                }
                break;
            case 0x42:
                synchronized (lock3) {
                    engTemp = data[1];
                    oilTemp = data[2];
                    batteryVolts = (data[3] & 0xFF) / 10.0;
                    airTemp = data[4];
                    tyreTempRRI = data[5];
                    tyreTempRRO = data[6];
                }
                break;
            case 0x43:
                synchronized (lock4) {

                }
                break;
            default:
        }
    }

    public int getThrottleData() {
        synchronized (lock1) {
            return throttle;
        }
    }

    public int getSpeedData() {
        synchronized (lock1) {
            return speed;
        }
    }

    public double getRpmData() {
        synchronized (lock1) {
            return rpm;
        }
    }

    public int getOilPressureData() {
        synchronized (lock1) {
            return oilPressure;
        }
    }

    public int getAirTemp() {
        synchronized (lock3) {
            return airTemp;
        }
    }

    public int getFuelPressure() {
        synchronized (lock1) {
            return fuelPressure;
        }
    }

    public int getBrakeData() {
        synchronized (lock2) {
            return brake;
        }
    }

    public int getBrakeTempData() {
        synchronized (lock2) {
            return brakeTemp;
        }
    }

    public int getGearData() {
        synchronized (lock2) {
            return gear;
        }
    }

    public int getEngTempData() {
        synchronized (lock3) {
            return engTemp;
        }
    }

    public int getOilTempData() {
        synchronized (lock3) {
            return oilTemp;
        }
    }

    public double getbatteryVoltsData() {
        synchronized (lock3) {
            return batteryVolts;
        }
    }

    public int getBrakeBiasData() {
        return brakeBias;
    }

    public int getTyreTempRRI() {
        return tyreTempRRI;
    }

    public int getTyreTempRRO() {
        return tyreTempRRO;
    }

}
