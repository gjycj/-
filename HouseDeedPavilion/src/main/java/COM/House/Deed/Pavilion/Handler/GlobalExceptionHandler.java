package COM.House.Deed.Pavilion.Handler;

import COM.House.Deed.Pavilion.Utils.Result;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {
    // 专门处理 MyBatis 相关异常
    @ExceptionHandler(MyBatisSystemException.class)
    @ResponseBody
    public Result<Void> handleMyBatisException(MyBatisSystemException e) {
        // 获取底层真实异常（如 BindingException、SQLException 等）
        Throwable originalException = e.getCause();
        // 提取真实错误信息（避免返回空 msg）
        String errorMsg = originalException != null ? originalException.getMessage() : "数据库操作失败";
        // 打印完整堆栈，便于调试
        originalException.printStackTrace();
        // 返回包含真实错误的响应
        return Result.fail("MyBatis 操作异常：" + errorMsg);
    }

    // 处理其他业务异常
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.fail(e.getMessage());
    }
}
