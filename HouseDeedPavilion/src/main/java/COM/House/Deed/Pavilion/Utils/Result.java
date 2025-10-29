package COM.House.Deed.Pavilion.Utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一接口响应体
 */
@Data
public class Result<T> {
    // 状态码：200成功，500失败，400参数错误等
    @Schema(description = "统一接口响应体")
    private Integer code;
    // 响应信息
    @Schema(description = "响应信息", example = "操作成功")
    private String msg;
    // 响应数据（成功时返回）
    @Schema(description = "响应数据（成功时返回，失败时为null）")
    private T data;

    // 成功响应（无数据）
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        return result;
    }

    // 成功响应（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败响应
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

    // 参数错误响应
    public static <T> Result<T> paramError(String msg) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMsg(msg);
        return result;
    }
}