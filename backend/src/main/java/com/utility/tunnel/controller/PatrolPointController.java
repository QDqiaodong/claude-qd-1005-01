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
import com.utility.tunnel.entity.PatrolPoint;
import com.utility.tunnel.service.PatrolPointService;

/** 巡检点接口 */
@RestController
@RequestMapping("/api/patrol-points")
public class PatrolPointController {

    private final PatrolPointService patrolService;

    public PatrolPointController(PatrolPointService patrolService) {
        this.patrolService = patrolService;
    }

    @GetMapping
    public ApiResponse<List<PatrolPoint>> list(@RequestParam(required = false) Long segmentId) {
        return ApiResponse.success(patrolService.list(segmentId));
    }

    @PostMapping
    public ApiResponse<PatrolPoint> create(@RequestBody PatrolPoint input) {
        return ApiResponse.success("巡检点已建", patrolService.create(input));
    }

    /** 巡检登记：刷新最近巡检时间为当前时间 */
    @PostMapping("/{id}/register")
    public ApiResponse<PatrolPoint> register(@PathVariable Long id) {
        return ApiResponse.success("巡检已登记", patrolService.register(id));
    }
}
