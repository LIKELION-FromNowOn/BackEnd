package com.youin.now.note;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code NOW-NOTE-003} 안내문 원문 조회.
 *
 * <p><b>요약하지 않고 원문 그대로 돌려줍니다.</b> 사용자가 「왜 이걸 하지 말라는 거지」라고
 * 물었을 때 앱이 지어낸 말이 아니라 <b>클리닉이 쓴 문장</b>을 보여 줄 수 있어야 합니다.
 *
 * <p>{@code care/}(관리 맥락·예정)는 김민정 님 폴더입니다. <b>여기는 원문만</b> 다룹니다.
 */
@RestController
@RequestMapping("/me/care")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /** 안내문이 없으면 {@code 404 NOT_FOUND} */
    @GetMapping("/note")
    public ApiResponse<NoteRes> note(@CurrentUser String userId) {
        return ApiResponse.ok(noteService.latest(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "안내문이 없습니다")));
    }
}
