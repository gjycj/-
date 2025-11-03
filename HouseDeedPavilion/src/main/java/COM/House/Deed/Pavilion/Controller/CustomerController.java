package COM.House.Deed.Pavilion.Controller;

import COM.House.Deed.Pavilion.DTO.CustomerAddDTO;
import COM.House.Deed.Pavilion.DTO.CustomerQueryDTO;
import COM.House.Deed.Pavilion.DTO.CustomerUpdateDTO;
import COM.House.Deed.Pavilion.Entity.Customer;
import COM.House.Deed.Pavilion.Service.CustomerService;
import COM.House.Deed.Pavilion.Utils.PageResult;
import COM.House.Deed.Pavilion.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 客户信息控制器
 */
@RestController
@RequestMapping("/customer")
@Tag(name = "客户管理", description = "客户信息的增删改查接口")
public class CustomerController {

    @Resource
    private CustomerService customerService;

    /**
     * 新增客户
     */
    @PostMapping("/add")
    @Operation(
            summary = "新增客户信息",
            description = "添加新客户，手机号需唯一",
            responses = {
                    @ApiResponse(responseCode = "200", description = "新增成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误（如手机号重复）",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<?> addCustomer(
            @Parameter(description = "客户新增参数", required = true)
            @Valid @RequestBody CustomerAddDTO addDTO
    ) {
        try {
            Long customerId = customerService.addCustomer(addDTO);
            return Result.success("新增" + customerId + "客户成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 根据ID查询客户
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "根据ID查询客户详情",
            description = "通过客户ID获取客户完整信息",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "客户ID无效或不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<Customer> getCustomerById(
            @Parameter(description = "客户ID", required = true, example = "30001")
            @PathVariable Long id
    ) {
        try {
            Customer customer = customerService.getCustomerById(id);
            return Result.success(customer);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 多条件分页查询客户
     */
    @GetMapping("/list")
    @Operation(
            summary = "多条件查询客户列表",
            description = "支持按姓名、手机号、状态等条件分页查询客户",
            responses = {
                    @ApiResponse(responseCode = "200", description = "查询成功",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<PageResult<Customer>> getCustomerList(
            @Parameter(description = "查询条件（可选）")
            CustomerQueryDTO queryDTO
    ) {
        try {
            PageResult<Customer> pageResult = customerService.getCustomerByCondition(queryDTO);
            return Result.success(pageResult);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 更新客户信息
     */
    @PostMapping("/update")
    @Operation(
            summary = "更新客户信息",
            description = "支持部分字段更新，更新手机号时需确保唯一性",
            responses = {
                    @ApiResponse(responseCode = "200", description = "更新成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误或客户不存在",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<?> updateCustomer(
            @Parameter(description = "客户更新参数", required = true)
            @Valid @RequestBody CustomerUpdateDTO updateDTO
    ) {
        try {
            customerService.updateCustomer(updateDTO);
            return Result.success("更新客户成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }

    /**
     * 删除客户信息（自动备份到备份表）
     */
    @PostMapping("/delete/{id}")
    @Operation(
            summary = "根据ID删除客户（自动备份）",
            description = "删除客户前会自动将数据备份到customer_backup表，需传递操作人ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "删除成功",
                            content = @Content(schema = @Schema(implementation = Result.class))),
                    @ApiResponse(responseCode = "400", description = "参数错误或备份失败",
                            content = @Content(schema = @Schema(implementation = Result.class)))
            }
    )
    public Result<?> deleteCustomer(
            @Parameter(description = "客户ID", required = true, example = "30001")
            @PathVariable Long id,
            @Parameter(description = "执行删除操作的用户ID", required = true, example = "1001")
            @RequestParam Long operatorId
    ) {
        try {
            customerService.deleteCustomer(id, operatorId);
            return Result.success("删除客户成功（已自动备份）");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("系统异常，请联系管理员");
        }
    }
}
