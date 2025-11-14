package com.house.deed.pavilion.common.util;

import com.house.deed.pavilion.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * 通用参数校验工具（所有模块复用）
 */
public class ValidateUtil {

    // 房号格式正则（支持“1-301”“1单元301”“A302”）
    private static final Pattern HOUSE_NO_PATTERN = Pattern.compile("^[A-Za-z0-9\\u4e00-\\u9fa5\\-]{2,20}$");
    // 产权证号正则（15位或18位字符，含字母数字）
    private static final Pattern CERT_NO_PATTERN = Pattern.compile("^[A-Za-z0-9]{15}|[A-Za-z0-9]{18}$");

    /**
     * 非空校验
     * @param obj 待校验对象
     * @param message 异常提示
     */
    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new BusinessException(400, message);
        }
        if (obj instanceof String && StringUtils.isBlank((String) obj)) {
            throw new BusinessException(400, message);
        }
    }

    /**
     * 数值合理性校验（大于0）
     * @param number 待校验数值
     * @param message 异常提示
     */
    public static void greaterThanZero(Number number, String message) {
        if (number == null) {
            throw new BusinessException(400, message);
        }
        if (number instanceof BigDecimal && ((BigDecimal) number).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, message);
        }
        if (number instanceof Integer && ((Integer) number) <= 0) {
            throw new BusinessException(400, message);
        }
    }

    /**
     * 数值大小校验（小于等于）
     * @param source 源数值
     * @param target 目标数值
     * @param message 异常提示
     */
    public static void lessThanOrEqual(Number source, Number target, String message) {
        notNull(source, "源数值不能为空");
        notNull(target, "目标数值不能为空");
        if (source instanceof Integer && target instanceof Integer) {
            if (((Integer) source) > ((Integer) target)) {
                throw new BusinessException(400, message);
            }
        }
        if (source instanceof BigDecimal && target instanceof BigDecimal) {
            if (((BigDecimal) source).compareTo((BigDecimal) target) > 0) {
                throw new BusinessException(400, message);
            }
        }
    }

    /**
     * 格式校验（房号）
     * @param houseNo 房号
     */
    public static void validateHouseNo(String houseNo) {
        notNull(houseNo, "房号不能为空");
        if (!HOUSE_NO_PATTERN.matcher(houseNo).matches()) {
            throw new BusinessException(400, "房号格式非法（支持2-20位字符，含字母、数字、汉字、横杠）");
        }
    }

    /**
     * 格式校验（产权证号）
     * @param certNo 产权证号
     */
    public static void validateCertNo(String certNo) {
        if (StringUtils.isNotBlank(certNo) && !CERT_NO_PATTERN.matcher(certNo).matches()) {
            throw new BusinessException(400, "产权证号格式非法（需15位或18位字母/数字）");
        }
    }
}