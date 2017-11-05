package com.thibaudperso.sonycamera.sdk;

/**
 * Created by thibaud on 07/11/16.
 */

public class RequestNotWellFormatedException extends RuntimeException {
    public RequestNotWellFormatedException(Exception e) {
        super(e);
    }
}
