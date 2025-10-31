package COM.House.Deed.Pavilion.Utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "分页查询结果封装")
public class PageResult<T> {

    @Schema(description = "当前页数据列表", example = "[{\"id\":1,\"name\":\"阳光花园\"},...]")
    private List<T> list;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "总页数", example = "10")
    private Integer pages;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;

}