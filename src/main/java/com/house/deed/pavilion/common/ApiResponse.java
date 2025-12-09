package com.house.deed.pavilion.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 统一API响应体
 * 标准化接口返回格式，包含状态码、消息和数据体
 *
 * @param <T> 响应数据类型
 */
@Data
@Schema(description = "统一API响应体，包含状态码、消息和数据")
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码：200-成功，其他为错误码
     */
    @Schema(description = "状态码（200=成功，其他为错误码）", example = "200")
    private int code;

    /**
     * 响应消息：成功时为"success"，错误时为具体原因
     */
    @Schema(description = "响应消息（成功时为success，错误时为具体原因）", example = "success")
    private String msg;

    /**
     * 响应数据：成功时返回业务数据，错误时可为null
     */
    @Schema(description = "响应数据（成功时返回业务数据，错误时为null）")
    private T data;

    /**
     * 分页信息：仅分页查询时返回，非分页接口为null
     */
    @Schema(description = "分页信息（仅分页查询时返回）")
    private PageInfo pageInfo;

    /**
     * 私有构造器：通过静态方法创建实例
     */
    private ApiResponse(int code, String msg, T data, PageInfo pageInfo) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.pageInfo = pageInfo;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, null);
    }

    /**
     * 成功响应（带分页数据）
     * 自动从MyBatis-Plus的Page对象提取分页信息
     */
    public static <T> ApiResponse<List<T>> success(Page<T> page) {
        PageInfo pageInfo = new PageInfo(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages()
        );
        return new ApiResponse<>(200, "success", page.getRecords(), pageInfo);
    }

    /**
     * 错误响应（带错误消息）
     */
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(500, msg, null, null);
    }

    /**
     * 错误响应（带自定义错误码和消息）
     */
    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null, null);
    }

    /**
     * 分页信息内部类
     * 封装分页查询的元数据
     */
    @Data
    @Schema(description = "分页查询元数据")
    public static class PageInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "当前页码", example = "1")
        private long current;

        @Schema(description = "每页条数", example = "10")
        private long size;

        @Schema(description = "总记录数", example = "100")
        private long total;

        @Schema(description = "总页数", example = "10")
        private long pages;

        public PageInfo(long current, long size, long total, long pages) {
            this.current = current;
            this.size = size;
            this.total = total;
            this.pages = pages;
        }
    }
}