package COM.House.Deed.Pavilion.Mapper;

import COM.House.Deed.Pavilion.DTO.VisitRecordQueryDTO;
import COM.House.Deed.Pavilion.Entity.VisitRecord;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;

/**
 * 带看记录Mapper接口（操作visit_record表）
 */
@Mapper
public interface VisitRecordMapper {

    /**
     * 新增带看记录
     *
     * @param visitRecord 带看记录信息
     * @return 影响行数（1-成功，0-失败）
     */
    int insert(VisitRecord visitRecord);

    /**
     * 根据ID查询带看记录
     *
     * @param id 带看记录ID
     * @return 带看记录详情（null-未查询到）
     */
    VisitRecord selectById(Long id);

    /**
     * 多条件分页查询带看记录
     *
     * @param queryDTO 查询条件
     * @return 分页的带看记录列表
     */
    Page<VisitRecord> selectByCondition(VisitRecordQueryDTO queryDTO);

    /**
     * 根据ID更新带看记录
     *
     * @param visitRecord 包含ID和待更新字段的带看记录
     * @return 影响行数（1-成功，0-失败）
     */
    int updateById(VisitRecord visitRecord);

    /**
     * 根据ID删除带看记录
     *
     * @param id 带看记录ID
     * @return 影响行数（1-成功，0-失败）
     */
    int deleteById(Long id);
}
