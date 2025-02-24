package com.workduo.group.group.service;

import com.workduo.area.sidoarea.dto.SidoAreaDto;
import com.workduo.area.sidoarea.entity.SidoArea;
import com.workduo.area.siggarea.dto.siggarea.SiggAreaDto;
import com.workduo.area.siggarea.entity.SiggArea;
import com.workduo.area.siggarea.repository.SiggAreaRepository;
import com.workduo.common.CommonRequestContext;
import com.workduo.configuration.jpa.JpaAuditingConfiguration;
import com.workduo.error.group.exception.GroupException;
import com.workduo.error.member.exception.MemberException;
import com.workduo.group.group.dto.CreateGroup;
import com.workduo.group.group.dto.GroupDto;
import com.workduo.group.group.dto.GroupParticipantsDto;
import com.workduo.group.group.dto.ListGroup;
import com.workduo.group.group.entity.Group;
import com.workduo.group.group.entity.GroupCreateMember;
import com.workduo.group.group.entity.GroupJoinMember;
import com.workduo.group.group.repository.GroupCreateMemberRepository;
import com.workduo.group.group.repository.GroupJoinMemberRepository;
import com.workduo.group.group.repository.GroupLikeRepository;
import com.workduo.group.group.repository.GroupRepository;
import com.workduo.group.group.repository.query.GroupQueryRepository;
import com.workduo.group.group.service.impl.GroupServiceImpl;
import com.workduo.group.group.type.GroupStatus;
import com.workduo.group.groupmetting.repository.GroupMeetingParticipantRepository;
import com.workduo.member.member.entity.Member;
import com.workduo.member.member.repository.MemberRepository;
import com.workduo.member.membercalendar.repository.MemberCalendarRepository;
import com.workduo.sport.sport.dto.SportDto;
import com.workduo.sport.sport.entity.Sport;
import com.workduo.sport.sport.repository.SportRepository;
import com.workduo.sport.sportcategory.dto.SportCategoryDto;
import com.workduo.sport.sportcategory.entity.SportCategory;
import com.workduo.util.AwsS3Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.workduo.error.group.type.GroupErrorCode.*;
import static com.workduo.error.member.type.MemberErrorCode.MEMBER_EMAIL_ERROR;
import static com.workduo.group.group.type.GroupJoinMemberStatus.GROUP_JOIN_MEMBER_STATUS_ING;
import static com.workduo.group.group.type.GroupJoinMemberStatus.GROUP_JOIN_MEMBER_STATUS_WITHDRAW;
import static com.workduo.group.group.type.GroupRole.GROUP_ROLE_LEADER;
import static com.workduo.group.group.type.GroupRole.GROUP_ROLE_NORMAL;
import static com.workduo.group.group.type.GroupStatus.GROUP_STATUS_CANCEL;
import static com.workduo.member.member.type.MemberStatus.MEMBER_STATUS_ING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Import({JpaAuditingConfiguration.class})
@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupCreateMemberRepository groupCreateMemberRepository;
    @Mock
    private SportRepository sportRepository;
    @Mock
    private SiggAreaRepository siggAreaRepository;
    @Mock
    private CommonRequestContext context;
    @Mock
    private GroupJoinMemberRepository groupJoinMemberRepository;
    @Mock
    private GroupMeetingParticipantRepository groupMeetingParticipantRepository;
    @Mock
    private MemberCalendarRepository memberCalendarRepository;
    @Mock
    private GroupQueryRepository groupQueryRepository;
    @Mock
    private GroupLikeRepository groupLikeRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private AwsS3Provider awsS3Provider;

    @Spy
    @InjectMocks
    private GroupServiceImpl groupService;

    Member member;
    Sport sport;
    SportCategory sportCategory;
    SiggArea siggArea;
    SidoArea sidoArea;
    Group group;
    GroupDto groupDto;
    GroupCreateMember groupCreateMember;
    GroupJoinMember normal;
    GroupJoinMember leader;
    GroupJoinMember alreadyWithdrawMember;
    Group deletedGroup;
    List<MultipartFile> image = new ArrayList<>();

    @BeforeEach
    public void init() {
        image.add(new MockMultipartFile(
                "multipartFiles",
                "imagefile.jpeg",
                "image/jpeg",
                "<<jpeg data>>".getBytes()
        ));

        member = Member.builder()
                .id(1L)
                .username("한규빈")
                .phoneNumber("01011111111")
                .nickname("규난")
                .password("1234")
                .email("rbsks147@naver.com")
                .memberStatus(MEMBER_STATUS_ING)
                .build();

        sport = Sport.builder()
                .id(1)
                .sportCategory(SportCategory.builder()
                        .id(1)
                        .name("구기")
                        .build())
                .name("축구")
                .build();

        siggArea = SiggArea.builder()
                .sgg("11110")
                .sggnm("종로구")
                .sidonm("서울특별시")
                .sidoArea(SidoArea.builder()
                        .sido("11")
                        .sidonm("서울특별시")
                        .build())
                .build();

        group = Group.builder()
                .id(1L)
                .groupStatus(GroupStatus.GROUP_STATUS_ING)
                .thumbnailPath("test")
                .introduce("test")
                .name("test")
                .siggArea(siggArea)
                .limitPerson(10)
                .sport(sport)
                .build();

        groupDto = GroupDto.builder()
                .groupId(1L)
                .thumbnailPath("test")
                .introduce("test")
                .name("test")
                .siggArea(SiggAreaDto.builder()
                        .sgg("11110")
                        .sidonm("11")
                        .sggnm("종로구")
                        .sidonm("서울특별시")
                        .build())
                .limitPerson(10)
                .sport(SportDto.builder().build())
                .build();

        deletedGroup = Group.builder()
                .id(1L)
                .groupStatus(GROUP_STATUS_CANCEL)
                .thumbnailPath("test")
                .introduce("test")
                .name("test")
                .siggArea(siggArea)
                .limitPerson(10)
                .sport(sport)
                .build();

        groupCreateMember = GroupCreateMember.builder()
                .member(member)
                .group(group)
                .build();

        normal = GroupJoinMember.builder()
                .member(member)
                .group(group)
                .groupJoinMemberStatus(GROUP_JOIN_MEMBER_STATUS_ING)
                .groupRole(GROUP_ROLE_NORMAL)
                .id(1L)
                .build();

        leader = GroupJoinMember.builder()
                .member(member)
                .group(group)
                .groupJoinMemberStatus(GROUP_JOIN_MEMBER_STATUS_ING)
                .groupRole(GROUP_ROLE_LEADER)
                .id(2L)
                .build();

        alreadyWithdrawMember = GroupJoinMember.builder()
                .member(member)
                .group(group)
                .groupJoinMemberStatus(GROUP_JOIN_MEMBER_STATUS_WITHDRAW)
                .groupRole(GROUP_ROLE_LEADER)
                .id(3L)
                .build();

        sportCategory = SportCategory.builder()
                .id(1)
                .name("구기")
                .build();

        sidoArea = SidoArea.builder()
                .sido("11")
                .sidonm("서울특별시")
                .build();
    }

    @Nested
    class createGroup {
        @Test
        @DisplayName("그룹 생성 성공")
        @Transactional
        public void createGroup() throws Exception {
            // given
            CreateGroup.Request request = CreateGroup.Request.builder()
                    .name("수정구 풋살 모임")
                    .limitPerson(30)
                    .sportId(1)
                    .sgg("11110")
                    .introduce("경기도 성남시 수정구 아재들의 풋살, 축구 모임입니다. 많은 참여 부탁드립니다.")
                    .build();

            doReturn(Optional.of(member)).when(memberRepository)
                            .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(siggArea)).when(siggAreaRepository)
                            .findBySgg(anyString());
            doReturn(Optional.of(sport)).when(sportRepository)
                            .findById(anyInt());
            doReturn(group).when(groupRepository)
                    .save(any());
            groupService.generatePath(1L);
            doReturn(new ArrayList<>(List.of("test"))).when(awsS3Provider)
                            .uploadFile(any(), anyString());

            // when
            groupService.createGroup(request, image);

            // then
            verify(memberRepository, times(1))
                    .findByEmail(anyString());
            verify(siggAreaRepository, times(1))
                    .findBySgg(anyString());
            verify(sportRepository, times(1))
                    .findById(anyInt());
            verify(groupRepository, times(1))
                    .save(any());
            verify(groupJoinMemberRepository, times(1))
                    .save(any());
            verify(groupCreateMemberRepository, times(1))
                    .save(any());
        }

        @Test
        @DisplayName("그룹 생성 실패 - 유저 정보 없음")
        public void creatGroupFailNotFoundUser() throws Exception {
            // given
            given(memberRepository.findByEmail(anyString()))
                    .willReturn(Optional.empty());
            doReturn("").when(context).getMemberEmail();
            CreateGroup.Request request = CreateGroup.Request.builder()
                    .name("수정구 풋살 모임")
                    .limitPerson(30)
                    .sportId(1)
                    .sgg("11110")
                    .introduce("경기도 성남시 수정구 아재들의 풋살, 축구 모임입니다. 많은 참여 부탁드립니다.")
//                    .thumbnailPath("https://블라블라~~~.com")
                    .build();

            // when
            MemberException memberException =
                    assertThrows(MemberException.class,
                            () -> groupService.createGroup(request, image));

            // then
            assertEquals(memberException.getErrorCode(), MEMBER_EMAIL_ERROR);
        }

        @Test
        @DisplayName("그룹 생성 실패 - 그룹 생성 최대개수 초과")
        public void creatGroupFailGroupCreateMaximumExceeded() throws Exception {
            // given
            given(memberRepository.findByEmail(anyString()))
                    .willReturn(Optional.of(member));
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();

            given(sportRepository.findById(anyInt()))
                    .willReturn(Optional.of(sport));
            given(siggAreaRepository.findBySgg(anyString()))
                    .willReturn(Optional.of(siggArea));

            given(groupCreateMemberRepository.countByMember(any()))
                    .willReturn(3L);

            CreateGroup.Request request = CreateGroup.Request.builder()
                    .name("수정구 풋살 모임")
                    .limitPerson(30)
                    .sportId(1)
                    .sgg("11110")
                    .introduce("경기도 성남시 수정구 아재들의 풋살, 축구 모임입니다. 많은 참여 부탁드립니다.")
//                    .thumbnailPath("https://블라블라~~~.com")
                    .build();

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.createGroup(request, null));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_MAXIMUM_EXCEEDED);
        }

        @Test
        @DisplayName("그룹 생성 실패 - 지역 정보 없음")
        public void creatGroupFailNotFoundSiggArea() throws Exception {
            // given
            given(memberRepository.findByEmail(anyString()))
                    .willReturn(Optional.of(member));
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();

            given(siggAreaRepository.findBySgg(anyString()))
                    .willReturn(Optional.empty());

            CreateGroup.Request request = CreateGroup.Request.builder()
                    .name("수정구 풋살 모임")
                    .limitPerson(30)
                    .sportId(1)
                    .sgg("11110")
                    .introduce("경기도 성남시 수정구 아재들의 풋살, 축구 모임입니다. 많은 참여 부탁드립니다.")
//                    .thumbnailPath("https://블라블라~~~.com")
                    .build();

            // when
            IllegalStateException groupException =
                    assertThrows(IllegalStateException.class,
                            () -> groupService.createGroup(request, null));

            // then
            assertEquals(groupException.getMessage(), "해당 지역은 없는 지역입니다.");
        }

        @Test
        @DisplayName("그룹 생성 실패 - 운동 정보 없음")
        public void creatGroupFailNotFoundSport() throws Exception {
            // given
            given(memberRepository.findByEmail(anyString()))
                    .willReturn(Optional.of(member));
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();

            given(siggAreaRepository.findBySgg(anyString()))
                    .willReturn(Optional.of(siggArea));
            given(sportRepository.findById(anyInt()))
                    .willReturn(Optional.empty());

            CreateGroup.Request request = CreateGroup.Request.builder()
                    .name("수정구 풋살 모임")
                    .limitPerson(30)
                    .sportId(1)
                    .sgg("11110")
                    .introduce("경기도 성남시 수정구 아재들의 풋살, 축구 모임입니다. 많은 참여 부탁드립니다.")
//                    .thumbnailPath("https://블라블라~~~.com")
                    .build();

            // when
            IllegalStateException groupException =
                    assertThrows(IllegalStateException.class,
                            () -> groupService.createGroup(request, null));

            // then
            assertEquals(groupException.getMessage(), "해당 운동은 없는 운동입니다.");
        }
    }


    @Nested
    class deleteGroup {
        @Test
        @DisplayName("그룹 삭제 성공")
        public void deleteGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository).findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository).findById(anyLong());
            doReturn(true).when(groupCreateMemberRepository)
                    .existsByMemberAndGroup(member, group);
            doNothing().when(groupCreateMemberRepository)
                    .deleteByMemberAndGroup(member, group);
            doNothing().when(groupJoinMemberRepository)
                    .updateGroupJoinMemberStatusCancel(group);
            doNothing().when(groupMeetingParticipantRepository)
                    .deleteAllByGroup(group);
            doNothing().when(memberCalendarRepository).
                    updateMemberCalendarMeetingActiveStatusGroupCancel(group);

            // when
            groupService.deleteGroup(1L);

            // then
            verify(memberRepository, times(1))
                    .findByEmail(anyString());

            verify(groupRepository, times(1))
                    .findById(anyLong());

            verify(groupCreateMemberRepository, times(1))
                    .existsByMemberAndGroup(member, group);

            verify(groupCreateMemberRepository, times(1))
                    .deleteByMemberAndGroup(member, group);

            verify(groupJoinMemberRepository, times(1))
                    .updateGroupJoinMemberStatusCancel(group);

            verify(groupMeetingParticipantRepository, times(1))
                    .deleteAllByGroup(group);

            verify(memberCalendarRepository, times(1))
                    .updateMemberCalendarMeetingActiveStatusGroupCancel(group);

        }

        @Test
        @DisplayName("그룹 삭제 실패 - 유저 정보 없음")
        public void deleteGroupFailNotFoundUser() throws Exception {
            // given
            given(memberRepository.findByEmail(anyString()))
                    .willReturn(Optional.empty());
            doReturn("").when(context).getMemberEmail();

            // when
            MemberException memberException =
                    assertThrows(MemberException.class,
                            () -> groupService.deleteGroup(1L));

            // then
            assertEquals(memberException.getErrorCode(), MEMBER_EMAIL_ERROR);
        }

        @Test
        @DisplayName("그룹 삭제 실패 - 그룹 정보 없음")
        public void deleteGroupFailNotFoundGroup() throws Exception {
            // given
            given(memberRepository.findByEmail(anyString()))
                    .willReturn(Optional.of(member));
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();

            given(groupRepository.findById(anyLong()))
                    .willReturn(Optional.empty());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.deleteGroup(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_FOUND);
        }

        @Test
        @DisplayName("그룹 삭제 실패 - 그룹장이 아닌 경우 그룹 삭제 실패")
        public void deleteGroupFailNotLeader() throws Exception {
            // given
            given(memberRepository.findByEmail(anyString()))
                    .willReturn(Optional.of(member));
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();

            given(groupRepository.findById(anyLong()))
                    .willReturn(Optional.of(group));

            given(groupCreateMemberRepository.existsByMemberAndGroup(any(), any()))
                    .willReturn(false);

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.deleteGroup(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_LEADER);
        }

    }

    @Nested
    class withdrawGroup {
        @Test
        @DisplayName("그룹 탈퇴 성공")
        public void withdrawGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository).findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository).findById(anyLong());
            doReturn(Optional.of(normal)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());
            doReturn(false).when(groupCreateMemberRepository)
                    .existsByMemberAndGroup(member, group);
            doNothing().when(groupMeetingParticipantRepository)
                    .deleteByGroupAndMember(group, member);
            doNothing().when(memberCalendarRepository).
                    updateMemberCalendarMemberAndGroupWithdraw(member, group);

            // when
            groupService.withdrawGroup(1L);

            // then
            verify(memberRepository, times(1))
                    .findByEmail(anyString());

            verify(groupRepository, times(1))
                    .findById(anyLong());

            verify(groupJoinMemberRepository, times(1))
                    .findByMemberAndGroup(any(), any());

            verify(groupCreateMemberRepository, times(1))
                    .existsByMemberAndGroup(member, group);

            verify(groupMeetingParticipantRepository, times(1))
                    .deleteByGroupAndMember(group, member);

            verify(memberCalendarRepository, times(1))
                    .updateMemberCalendarMemberAndGroupWithdraw(member, group);
        }

        @Test
        @DisplayName("그룹 탈퇴 실패 - 유저 정보 없음")
        public void withdrawGroupFailNotFoundUser() throws Exception {
            // given
            doReturn(Optional.empty()).when(memberRepository).findByEmail(anyString());
            doReturn("").when(context).getMemberEmail();


            // when
            MemberException memberException =
                    assertThrows(MemberException.class,
                            () -> groupService.withdrawGroup(1L));

            // then
            assertEquals(memberException.getErrorCode(), MEMBER_EMAIL_ERROR);
        }

        @Test
        @DisplayName("그룹 탈퇴 실패 - 그룹 정보 없음")
        public void withdrawGroupFailNotFoundGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository).findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();
            doReturn(Optional.empty()).when(groupRepository).findById(anyLong());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.withdrawGroup(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_FOUND);
        }

        @Test
        @DisplayName("그룹 탈퇴 실패 - 그룹 안에 유저 정보 없음")
        public void withdrawGroupFailGroupNotFoundGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository).findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository).findById(anyLong());
            doReturn(Optional.empty()).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.withdrawGroup(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_FOUND_USER);
        }

        @Test
        @DisplayName("그룹 탈퇴 실패 - 그룹장인 경우 그룹 탈퇴 실패")
        public void withdrawGroupFailLeaderNotWithdraw() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository).findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository).findById(anyLong());
            doReturn(Optional.of(leader)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());
            doReturn(true).when(groupCreateMemberRepository)
                    .existsByMemberAndGroup(any(), any());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.withdrawGroup(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_LEADER_NOT_WITHDRAW);
        }

        @Test
        @DisplayName("그룹 탈퇴 실패 - 이미 탈퇴한 그룹")
        public void withdrawGroupFailAlreadyWithdraw() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository).findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context).getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository).findById(anyLong());
            doReturn(Optional.of(alreadyWithdrawMember)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());
            doReturn(false).when(groupCreateMemberRepository)
                    .existsByMemberAndGroup(any(), any());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.withdrawGroup(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_ALREADY_WITHDRAW);
        }
    }

    @Nested
    class detailGroup {
        @Test
        @DisplayName("그룹 상세 보기 성공")
        public void groupDetail() throws Exception {
            // given
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(Optional.of(groupDto)).when(groupQueryRepository)
                    .findById(anyLong());

            // when
            GroupDto groupDto = groupService.groupDetail(1L);

            // then
            assertEquals(groupDto.getGroupId(), group.getId());
        }

        @Test
        @DisplayName("그룹 상세 보기 실패 - 삭제된 그룹")
        public void groupDetailFailAlreadyDeletedGroup() throws Exception {
            // given
            doReturn(Optional.of(deletedGroup)).when(groupRepository)
                    .findById(anyLong());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupDetail(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_ALREADY_DELETE_GROUP);
        }

        @Test
        @DisplayName("그룹 상세 보기 실패 - 해당 그룹 없음")
        public void groupDetailFailNotFoundGroup() throws Exception {
            // given
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(Optional.empty()).when(groupQueryRepository)
                    .findById(anyLong());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupDetail(1L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_FOUND);
        }
    }

    @Nested
    class likeGroup {
        @Test
        @DisplayName("그룹 좋아요 성공")
        public void groupLike() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(true).when(groupJoinMemberRepository)
                    .existsByGroupAndMember(any(), any());
            doReturn(Optional.of(normal)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());

            // when
            groupService.groupLike(1L);

            // then
            verify(memberRepository, times(1))
                    .findByEmail(any());

            verify(groupRepository, times(1))
                    .findById(anyLong());

            verify(groupJoinMemberRepository, times(1))
                    .existsByGroupAndMember(any(), any());

            verify(groupJoinMemberRepository, times(1))
                    .findByMemberAndGroup(any(), any());
        }

        @Test
        @DisplayName("그룹 좋아요 실패 - 유저 정보 없음")
        public void groupLikeFailNotFoundUser() throws Exception {
            // given
            doReturn(Optional.empty()).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("").when(context)
                    .getMemberEmail();

            // when
            MemberException memberException =
                    assertThrows(MemberException.class,
                            () -> groupService.groupUnLike(1L));

            // then
            assertEquals(memberException.getErrorCode(), MEMBER_EMAIL_ERROR);
        }

        @Test
        @DisplayName("그룹 좋아요 실패 - 그룹 정보 없음")
        public void groupLikeFailNotFoundGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.empty()).when(groupRepository)
                    .findById(anyLong());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupUnLike(1L));

            // then
            assertEquals(groupException.getErrorCode(),  GROUP_NOT_FOUND);
        }

        @Test
        @DisplayName("그룹 좋아요 실패 - 이미 삭제된 그룹")
        public void groupLikeFailAlreadyDeleteGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(deletedGroup)).when(groupRepository)
                    .findById(anyLong());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupUnLike(1L));

            // then
            assertEquals(groupException.getErrorCode(),  GROUP_ALREADY_DELETE_GROUP);
        }

        @Test
        @DisplayName("그룹 좋아요 실패 - 그룹 안에 유저 정보 없음")
        public void groupLikeFailGroupNotFoundUser() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(false).when(groupJoinMemberRepository)
                    .existsByGroupAndMember(any(), any());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupUnLike(1L));

            // then
            assertEquals(groupException.getErrorCode(),  GROUP_NOT_FOUND_USER);
        }

        @Test
        @DisplayName("그룹 좋아요 실패 - 이미 탈퇴한 그룹")
        public void groupLikeFailAlreadyWithdrawGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(true).when(groupJoinMemberRepository)
                    .existsByGroupAndMember(any(), any());
            doReturn(Optional.of(alreadyWithdrawMember)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupUnLike(1L));

            // then
            assertEquals(groupException.getErrorCode(),  GROUP_ALREADY_WITHDRAW);
        }
    }

    @Nested
    class participantGroup {
        @Test
        @DisplayName("그룹 참여 성공")
        public void groupParticipant() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(1).when(groupJoinMemberRepository)
                    .countByGroup(any());

            // when
            groupService.groupParticipant(2L);

            // then
            verify(memberRepository, times(1))
                    .findByEmail(anyString());
            verify(groupRepository, times(1))
                    .findById(anyLong());
            verify(groupJoinMemberRepository, times(1))
                    .countByGroup(any());
        }

        @Test
        @DisplayName("그룹 참여 실패 - 유저 정보 없음")
        public void groupParticipantFailNotFoundUser() throws Exception {
            // given
            doReturn(Optional.empty()).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("").when(context)
                    .getMemberEmail();

            // when
            MemberException memberException =
                    assertThrows(MemberException.class,
                            () -> groupService.groupParticipant(2L));

            // then
            assertEquals(memberException.getErrorCode(), MEMBER_EMAIL_ERROR);
        }

        @Test
        @DisplayName("그룹 참여 실패 - 그룹 정보 없음")
        public void groupParticipantFailNotFoundGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.empty()).when(groupRepository)
                    .findById(anyLong());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupParticipant(2L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_FOUND);
        }

        @Test
        @DisplayName("그룹 참여 실패 - 이미 그룹이 삭제")
        public void groupParticipantFailAlreadyDeleteGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(deletedGroup)).when(groupRepository)
                    .findById(anyLong());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupParticipant(2L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_ALREADY_DELETE_GROUP);
        }

        @Test
        @DisplayName("그룹 참여 실패 - 정원 초과")
        public void groupParticipantFailGroupMaximumParticipant() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("rbsks147@naver.com").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(10).when(groupJoinMemberRepository)
                    .countByGroup(any());

            // when
            GroupException groupException =
                    assertThrows(GroupException.class,
                            () -> groupService.groupParticipant(2L));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_MAXIMUM_PARTICIPANT);
        }
    }

    @Nested
    public class groupList {

        @Test
        @DisplayName("그룹 리스트 성공")
        public void groupList() throws Exception {
            // given
            SidoAreaDto sidoAreaDto = SidoAreaDto.fromEntity(sidoArea);
            SiggAreaDto siggAreaDto = SiggAreaDto.fromEntity(siggArea);
            SportCategoryDto sportCategoryDto = SportCategoryDto.fromEntity(sportCategory);
            SportDto sportDto = SportDto.fromEntity(sport);

            ListGroup.Request condition = ListGroup.Request.builder()
                    .sgg(null)
                    .sportId(null)
                    .build();

            PageRequest pageRequest = PageRequest.of(0, 5);
            GroupDto groupDto = GroupDto.builder()
                    .groupId(1L)
                    .sidoArea(sidoAreaDto)
                    .siggArea(siggAreaDto)
                    .sport(sportDto)
                    .sportCategory(sportCategoryDto)
                    .introduce("test")
                    .limitPerson(10)
                    .likes(10L)
                    .participants(20L)
                    .thumbnailPath("test")
                    .name("group1")
                    .build();
            List<GroupDto> groupDtoList = new ArrayList<>(List.of(groupDto));
            Page<GroupDto> groupDtoPage = new PageImpl<>(groupDtoList);
            doReturn(null).when(context)
                            .getMemberEmail();
            Long memberId = null;
            doReturn(groupDtoPage).when(groupQueryRepository)
                    .findByGroupList(any(), eq(memberId), any());

            // when
            Page<GroupDto> groupDtoPages = groupService.groupList(pageRequest, condition);

            // then
            assertEquals(groupDtoPages.getContent().size(), groupDtoList.size());
        }
    }

    @Nested
    public class groupParticipantList {

        @Test
        @DisplayName("그룹 참여자 리스트 성공")
        public void groupParticipantList() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("test1234").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(Optional.of(normal)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());
            doReturn(true).when(groupJoinMemberRepository)
                    .existsByGroupAndMember(any(), any());

            PageRequest pageRequest = PageRequest.of(0, 5);
            GroupParticipantsDto groupParticipantsDto = GroupParticipantsDto.builder()
                    .userId(1L)
                    .username("test")
                    .nickname("test")
                    .profileImg("test")
                    .groupRole(GROUP_ROLE_LEADER)
                    .build();
            List<GroupParticipantsDto> participantsDtoList =
                    new ArrayList<>(List.of(groupParticipantsDto));
            Page<GroupParticipantsDto> participantsDtos = new PageImpl<>(participantsDtoList);
            doReturn(participantsDtos).when(groupQueryRepository)
                    .findByGroupParticipantList(any(), anyLong());

            // when
            Page<GroupParticipantsDto> participantsPages = groupService.groupParticipantList(pageRequest, 1L);

            // then
            assertEquals(participantsPages.getContent().size(), participantsDtoList.size());
        }

        @Test
        @DisplayName("그룹 참여자 리스트 실패 - 유저 정보 없음")
        public void groupParticipantListFailNotFoundUser() throws Exception {
            // given
            doReturn(Optional.empty()).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("").when(context)
                    .getMemberEmail();

            // when
            MemberException groupException = assertThrows(MemberException.class,
                    () -> groupService.groupParticipantList(any(), anyLong()));

            // then
            assertEquals(groupException.getErrorCode(), MEMBER_EMAIL_ERROR);
        }

        @Test
        @DisplayName("그룹 참여자 리스트 실패 - 그룹 없음")
        public void groupParticipantListFailNotFoundGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("test1234").when(context)
                    .getMemberEmail();
            doReturn(Optional.empty()).when(groupRepository)
                    .findById(anyLong());

            // when
            GroupException groupException = assertThrows(GroupException.class,
                    () -> groupService.groupParticipantList(any(), anyLong()));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_FOUND);
        }

        @Test
        @DisplayName("그룹 참여자 리스트 실패 - 그룹에 해당 유저 없음")
        public void groupParticipantListFailNotFoundGroupUser() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("test1234").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(Optional.empty()).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());

            // when
            GroupException groupException = assertThrows(GroupException.class,
                    () -> groupService.groupParticipantList(any(), anyLong()));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_NOT_FOUND_USER);
        }

        @Test
        @DisplayName("그룹 참여자 리스트 실패 - 이미 삭제된 그룹")
        public void groupParticipantListFailAlreadyDeleteGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("test1234").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(deletedGroup)).when(groupRepository)
                    .findById(anyLong());
            doReturn(Optional.of(normal)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());

            // when
            GroupException groupException = assertThrows(GroupException.class,
                    () -> groupService.groupParticipantList(any(), anyLong()));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_ALREADY_DELETE_GROUP);
        }

        @Test
        @DisplayName("그룹 참여자 리스트 실패 - 이미 탈퇴한 유저")
        public void groupParticipantListFailAlreadyWithdrawGroup() throws Exception {
            // given
            doReturn(Optional.of(member)).when(memberRepository)
                    .findByEmail(anyString());
            doReturn("test1234").when(context)
                    .getMemberEmail();
            doReturn(Optional.of(group)).when(groupRepository)
                    .findById(anyLong());
            doReturn(Optional.of(alreadyWithdrawMember)).when(groupJoinMemberRepository)
                    .findByMemberAndGroup(any(), any());
            doReturn(true).when(groupJoinMemberRepository)
                    .existsByGroupAndMember(any(), any());

            // when
            GroupException groupException = assertThrows(GroupException.class,
                    () -> groupService.groupParticipantList(any(), anyLong()));

            // then
            assertEquals(groupException.getErrorCode(), GROUP_ALREADY_WITHDRAW);
        }
    }
}
