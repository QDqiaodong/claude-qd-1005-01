package com.utility.tunnel.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utility.tunnel.common.ApiResponse;
import com.utility.tunnel.entity.RiskEvent;
import com.utility.tunnel.entity.RiskEventDetail;
import com.utility.tunnel.entity.RiskEventView;
import com.utility.tunnel.service.RiskEventService;

/** 巡检异常 / 风险事件接口 */
@RestController
@RequestMapping("/api/risk-events")
public class RiskEventController {

    private final RiskEventService riskService;

    public RiskEventController(RiskEventService riskService) {
        this.riskService = riskService;
    }

    /** 列表：按舱段 / 状态筛选，未解除事件带当前封控原因 */
    @GetMapping
    public ApiResponse<List<RiskEventView>> list(@RequestParam(required = false) Long segmentId,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.success(riskService.list(segmentId, status));
    }

    /** 详情：含关联巡检点 / 设备的基本信息 */
    @GetMapping("/{id}")
    public ApiResponse<RiskEventDetail> detail(@PathVariable Long id) {
        return ApiResponse.success(riskService.detail(id));
    }

    /** 登记风险事件（从巡检点或附属设备发起） */
    @PostMapping
    public ApiResponse<RiskEvent> create(@RequestBody RiskEvent input) {
        return ApiResponse.success("风险事件已登记", riskService.create(input));
    }

    /** 推进：待处置 → 处理中 */
    @PostMapping("/{id}/advance")
    public ApiResponse<RiskEvent> advance(@PathVariable Long id, @RequestBody(required = false) RiskEvent input) {
        String note = input == null ? null : input.getHandleNote();
        return ApiResponse.success("已推进为处理中", riskService.advance(id, note));
    }

    /** 解除：待处置 / 处理中 → 已解除（留处置说明、解除人、解除时间） */
    @PostMapping("/{id}/resolve")
    public ApiResponse<RiskEvent> resolve(@PathVariable Long id, @RequestBody RiskEvent input) {
        return ApiResponse.success("风险已解除",
                riskService.resolve(id, input.getHandleNote(), input.getResolvedBy()));
    }
}
