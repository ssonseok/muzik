package son.suck.muzik.service;

import son.suck.muzik.dto.MafiaExecutionVoteRequestDto;
import son.suck.muzik.dto.MafiaNightActionRequestDto;
import son.suck.muzik.dto.MafiaVoteRequestDto;

public interface MafiaPlayService {
    //직업스킬(마피아 킬 대상, 의사 힐 대상, 경찰 조사 대상)
    void processNightAction(Long userId, MafiaNightActionRequestDto request);
    //밤 결과 정산 및 낮 페이즈 전환
    //(마피아 킬, 의사 힐, 군인 방어, 경찰 조사 결과를 종합하여 사망자 판정 및 낮으로 전환)
    boolean calculateNightResult(Long roomId);

    //낮  투표 접수
    void processNominationVote(Long userId, MafiaVoteRequestDto request);
    //낮 최종 투표
    void processDefenseVote(Long userId, MafiaExecutionVoteRequestDto request);

    //낮 투표 결과 집계 및 처형 정산 후 밤 페이즈로 전환
    boolean calculateDayResult(Long roomId);
    // 승리 조건 체크 (시민 또는 마피아 승리 판별 및 게임 END 처리)
    boolean checkGameEndCondition(Long roomId);
    // 처형 후보자가 선정되었는지 확인하는 메서드 추가
    boolean hasExecutionTarget(Long roomId);
    //페이즈때 권한있는 유저들만 채팅칠수있게 하려고
    boolean isExecutionTarget(Long roomId, Long participantId);
}
