package com.house.deed.pavilion.common.dto;
import lombok.Data;

@Data
public class ResultDTO<T> {

    private int code;

    private String message;

    private T data;

    // 成功响应（无数据）
    public static <T> ResultDTO<T> success() {
        ResultDTO<T> result = new ResultDTO<>();
        result.setCode(0);
        result.setMessage("操作成功");
        return result;
    }

    // 成功响应（带数据）
    public static <T> ResultDTO<T> success(T data) {
        ResultDTO<T> result = success();
        result.setData(data);
        return result;
    }


    // 失败响应（自定义消息）
    public static <T> ResultDTO<T> fail(String msg) {
        ResultDTO<T> result = new ResultDTO<>();
        result.setCode(1);
        result.setMessage(msg);
        return result;
    }

    // 失败响应（自定义状态码+消息）
    public static <T> ResultDTO<T> fail(int code, String msg) {
        ResultDTO<T> result = new ResultDTO<>();
        result.setCode(code);
        result.setMessage(msg);
        return result;
    }

}