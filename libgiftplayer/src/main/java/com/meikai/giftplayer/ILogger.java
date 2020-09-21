package com.meikai.giftplayer;

/**
 * Created by meikai on 2020/09/21.
 */
public interface ILogger {

    void i(String tag, String log);

    void e(String tag, String log);

    void postError(String error);

}
