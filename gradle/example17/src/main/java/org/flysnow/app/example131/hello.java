package org.flysnow.app.example131;

public class hello {
    static{
        System.loadLibrary("jh_hello");
    }
    public native String get_hello();
}
