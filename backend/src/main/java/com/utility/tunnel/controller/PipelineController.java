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
import com.utility.tunnel.entity.Pipeline;
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

    @GetMapping("/{id}")
    public ApiResponse<Pipeline> get(@PathVariable Long id) {
        return ApiResponse.success(pipelineService.get(id));
    }

    @PostMapping
    public ApiResponse<Pipeline> create(@RequestBody Pipeline input) {
        return ApiResponse.success("管线已入廊", pipelineService.create(input));
    }
}
