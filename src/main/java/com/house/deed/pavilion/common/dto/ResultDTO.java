package com.house.deed.pavilion.common.dto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "统一响应体")
public class ResultDTO<T> {
    @ApiModelProperty(value = "状态码：200=成功，其他=失败", example = "200")
    private int code;
    @ApiModelProperty(value = "提示信息", example = "操作成功")
    private String message;
    @ApiModelProperty(value = "业务数据")
    private T data;

    // 成功响应（带数据）
    public static <T> ResultDTO<T> success(T data) {
        ResultDTO<T> result = new ResultDTO<>();
        result.code = 200;
        result.message = "操作成功";
        result.data = data;
        return result;
    }

    // 失败响应（带错误码和信息）
    public static <T> ResultDTO<T> fail(int code, String message) {
        ResultDTO<T> result = new ResultDTO<>();
        result.code = code;
        result.message = message;
        return result;
    }
}