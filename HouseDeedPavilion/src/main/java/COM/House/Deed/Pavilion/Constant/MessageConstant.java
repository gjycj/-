package COM.House.Deed.Pavilion.Constant;

public class MessageConstant {
    // 房东相关
    public static final String LANDLORD_NAME_EMPTY = "房东姓名不能为空";
    public static final String LANDLORD_PHONE_EMPTY = "联系电话不能为空";
    public static final String CREATED_BY_INVALID = "创建人ID无效（必须为正整数）";
    public static final String UPDATED_BY_INVALID = "更新人ID无效（必须为正整数）";
    public static final String PHONE_ALREADY_USED = "联系电话【%s】已被占用，无法新增";
    public static final String ADD_LANDLORD_FAIL = "新增房东失败，请重试";
    public static final String LANDLORD_ID_INVALID = "房东ID无效（必须为正整数）";
    public static final String LANDLORD_NOT_FOUND = "未找到ID为%s的房东";
    public static final String LANDLORD_NOT_EXIST_FOR_UPDATE = "房东不存在（ID：%s），无法更新";
    public static final String PHONE_ALREADY_USED_FOR_UPDATE = "联系电话【%s】已被占用，无法更新";
    public static final String UPDATE_LANDLORD_FAIL = "更新房东信息失败，请重试";
    // 新增删除相关常量
    public static final String LANDLORD_HAS_RELATED_HOUSE = "该房东关联了%d个房源，请先解除关联再删除";
    public static final String BACKUP_LANDLORD_FAIL = "备份房东数据失败，删除终止";
    public static final String DELETE_LANDLORD_FAIL = "删除房东记录失败";
    public static final String DELETE_LANDLORD_SUCCESS = "房东已删除，数据已备份";

}
