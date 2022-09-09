package com.workduo.member.member.dto;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

public class MemberCreateDto {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull(message = "이메일 은 필수 입력 사항 입니다.")
        private String email;
        @NotNull(message = "비밀번호 는 필수 입력 사항 입니다.")
        private String password;
        @NotNull(message = "유저이름 은 필수 입력 사항 입니다.")
        private String username; // 유저 이름
        @NotNull(message = "핸드폰 번호 는 필수 입력 사항 입니다.")
        private String phoneNumber; // 핸드폰
        @NotNull(message = "닉네임 은 필수 입력 사항 입니다.")
        private String nickname; // 별명
        //지역3개
        @NotNull(message = "지역 은 최소 1개 이상 선택해야 합니다.")
        private List<Integer> siggAreaIdList;
        //운동3게
        @NotNull(message = "스포츠 는 최소 1개 이상 선택해야 합니다.")
        private List<Integer> sportList;
    }
}
