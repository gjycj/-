package COM.House.Deed.Pavilion.Service;

import COM.House.Deed.Pavilion.DTO.HouseStatusLogQueryDTO;
import COM.House.Deed.Pavilion.Entity.House;
import COM.House.Deed.Pavilion.Entity.HouseStatusLog;
import COM.House.Deed.Pavilion.Mapper.HouseMapper;
import COM.House.Deed.Pavilion.Mapper.HouseStatusLogMapper;
import COM.House.Deed.Pavilion.Utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房源状态变更记录服务类
 * 处理房源状态变更记录的创建和查询
 */
@Service
public class HouseStatusLogService {

    @Resource
    private HouseStatusLogMapper statusLogMapper;

    @Resource
    private HouseMapper houseMapper;

    /**
     * 创建房源状态变更记录
     *
     * @param houseId    房源ID
     * @param oldStatus  变更前状态
     * @param newStatus  变更后状态
     * @param reason     变更原因
     * @param operatorId 操作人ID
     */
    @Transactional
    public void createStatusLog(Long houseId, Integer oldStatus, Integer newStatus, String reason, Long operatorId) {
        // 1. 校验核心参数
        if (houseId == null || houseId <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }
        if (oldStatus == null) {
            throw new RuntimeException("变更前状态不能为空");
        }
        if (newStatus == null) {
            throw new RuntimeException("变更后状态不能为空");
        }
        if (oldStatus.equals(newStatus)) {
            throw new RuntimeException("变更前状态与变更后状态不能相同");
        }
        if (operatorId == null || operatorId <= 0) {
            throw new RuntimeException("操作人ID无效（必须为正整数）");
        }

        // 2. 校验房源是否存在
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new RuntimeException("房源不存在（ID：" + houseId + "），无法记录状态变更");
        }

        // 3. 构建状态变更记录
        HouseStatusLog log = new HouseStatusLog();
        log.setHouseId(houseId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setReason(reason);
        log.setOperatedBy(operatorId);
        log.setOperatedAt(LocalDateTime.now()); // 操作时间为当前时间

        // 4. 执行插入
        int rows = statusLogMapper.insert(log);
        if (rows != 1) {
            throw new RuntimeException("创建房源状态变更记录失败，请重试");
        }

    }

    /**
     * 根据ID查询状态变更记录
     *
     * @param id 记录ID
     * @return 状态变更记录详情
     */
    public HouseStatusLog getStatusLogById(Long id) {
        // 1. 校验ID
        if (id == null || id <= 0) {
            throw new RuntimeException("状态变更记录ID无效（必须为正整数）");
        }

        // 2. 查询并校验存在性
        HouseStatusLog log = statusLogMapper.selectById(id);
        if (log == null) {
            throw new RuntimeException("未找到ID为" + id + "的状态变更记录");
        }

        return log;
    }

    /**
     * 根据房源ID查询所有状态变更记录
     *
     * @param houseId 房源ID
     * @return 状态变更记录列表（按时间倒序）
     */
    public List<HouseStatusLog> getStatusLogsByHouseId(Long houseId) {
        // 1. 校验房源ID
        if (houseId == null || houseId <= 0) {
            throw new RuntimeException("房源ID无效（必须为正整数）");
        }

        // 2. 校验房源是否存在
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new RuntimeException("房源不存在（ID：" + houseId + "）");
        }

        // 3. 查询并返回状态变更记录
        return statusLogMapper.selectByHouseId(houseId);
    }

    /**
     * 多条件分页查询状态变更记录
     *
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页的状态变更记录列表
     */
    public PageResult<HouseStatusLog> getStatusLogsByCondition(HouseStatusLogQueryDTO queryDTO) {
        // 1. 处理空参数
        if (queryDTO == null) {
            queryDTO = new HouseStatusLogQueryDTO();
        }

        // 2. 校验分页参数
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10);
        }

        // 3. 执行分页查询
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<HouseStatusLog> logPage = statusLogMapper.selectByCondition(queryDTO);

        // 4. 封装分页结果
        return getHouseStatusLogPageResult(logPage);
    }

    /**
     * 封装房源分页结果
     */
    private PageResult<HouseStatusLog> getHouseStatusLogPageResult(Page<HouseStatusLog> logPage) {
        PageResult<HouseStatusLog> pageResult = new PageResult<>();
        pageResult.setList(logPage.getResult());
        pageResult.setTotal(logPage.getTotal());
        pageResult.setPages(logPage.getPages());
        pageResult.setPageNum(logPage.getPageNum());
        pageResult.setPageSize(logPage.getPageSize());
        return pageResult;
    }

}
