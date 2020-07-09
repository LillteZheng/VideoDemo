package com.zhengsr.videodemo.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author by  zhengshaorui on
 * Describe:
 */
public class CloseUtils {
    public static void close(Closeable... closeables){
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
