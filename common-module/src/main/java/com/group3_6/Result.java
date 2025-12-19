package com.group3_6;

import lombok.Data;

@Data
public class Result {
    public Integer errno;
    public String errmsg;
    public Object data;

    public static Result success(Object data) {
        Result result = new Result();
        result.errno = 0;
        result.data = data;
        return result;
    }

    public static Result error(String errmsg) {
        Result result = new Result();
        result.errno = 1;
        result.errmsg = errmsg;
        return result;
    }

    public static Result success(String errmsg) {
        Result result = new Result();
        result.errno = 0;
        result.errmsg = errmsg;
        return result;
    }
}
