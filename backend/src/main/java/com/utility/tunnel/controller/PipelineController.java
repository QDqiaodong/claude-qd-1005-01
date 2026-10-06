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
import com.utility.tunnel.entity.CapacityCheck;
import com.utility.tunnel.entity.Pipeline;
import com.utility.tunnel.entity.SegmentCapacity;
import com.utility.tunnel.service.PipelineService;

/** 入廊管线接口 */
@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping
    public ApiResponse<List<Pipeline>> list(@RequestParam(required = false) Long segmentId) {
        return ApiResponse.success(pipelineService.list(segmentId));
    }

    /** 全部舱段用量（列表汇总：舱容 / 已用 / 剩余 / 逐条占用） */
    @GetMapping("/capacities")
    public ApiResponse<List<SegmentCapacity>> capacities() {
        return ApiResponse.success(pipelineService.capacities());
    }

    /** 指定舱段用量（新建表单的舱段容量面板数据源） */
    @GetMapping("/capacity")
    public ApiResponse<SegmentCapacity> capacity(@RequestParam Long segmentId) {
        return ApiResponse.success(pipelineService.capacity(segmentId));
    }

    /** 提交前容量与规格兼容预检：不落库，返回触发的规则编号与原因 */
    @PostMapping("/check")
    public ApiResponse<CapacityCheck> check(@RequestBody Pipeline input) {
        return ApiResponse.success(pipelineService.check(input));
    }

    @GetMapping("/{id}")
    public ApiResponse<Pipeline> get(@PathVariable Long id) {
        return ApiResponse.success(pipelineService.get(id));
    }

    @PostMapping
    public ApiResponse<Pipeline> create(@RequestBody Pipeline input) {
        return ApiResponse.success("管线已入廊", pipelineService.create(input));
    }
}
