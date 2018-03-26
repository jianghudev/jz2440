package org.flysnow.app.example131;

public class hello {
    static{
        System.loadLibrary("jh_hello");
        System.out.println("load jh_hello library ok!");
    }
    public native String get_hello();
}
