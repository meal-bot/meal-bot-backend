package pack.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pack.dto.InbodyDto;
import pack.entity.User;
import pack.service.InbodyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inbody")
@RequiredArgsConstructor
public class InbodyController {

    private final InbodyService inbodyService;

    /** POST /api/inbody — 인바디 데이터 저장 */
    @PostMapping
    public ResponseEntity<InbodyDto.InbodyResponse> save(
            @AuthenticationPrincipal User user,
            @RequestBody InbodyDto.SaveRequest request) {
        return ResponseEntity.ok(inbodyService.save(user, request));
    }

    /** GET /api/inbody — 내 인바디 기록 목록 조회 (최신순) */
    @GetMapping
    public ResponseEntity<List<InbodyDto.InbodyResponse>> getList(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(inbodyService.getList(user));
    }

    /** DELETE /api/inbody/{inbodyId} — 인바디 기록 삭제 */
    @DeleteMapping("/{inbodyId}")
    public ResponseEntity<Map<String, Boolean>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long inbodyId) {
        inbodyService.delete(user, inbodyId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}