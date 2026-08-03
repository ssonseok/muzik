package son.suck.muzik.session;

import lombok.Getter;
import lombok.Setter;
import son.suck.muzik.domain.Music;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class GameSession {

    private List<Music> playlist;                   // 이번 판의 출제 음악 목록
    private int currentRound = 0;                  // 현재 라운드 (0부터 시작)
    private final Set<String> skipVotes = new HashSet<>(); // 스킵 누른 유저(닉네임 or ID)

    public GameSession(List<Music> playlist) {
        this.playlist = playlist;
    }

    // 현재 라운드의 음악 가져오기
    public Music getCurrentMusic() {
        return playlist.get(currentRound);
    }

    // 다음 라운드가 남아있는지 확인
    public boolean hasNextRound() {
        return currentRound + 1 < playlist.size();
    }

    // 다음 라운드로 이동 & 스킵 투표함 초기화
    public void nextRound() {
        this.currentRound++;
        this.skipVotes.clear();
    }

    // 스킵 투표 추가 (Set이므로 한 사람이 여러 번 눌러도 1번만 추가됨)
    public boolean addSkipVote(String sender) {
        return skipVotes.add(sender);
    }

    // 현재 스킵 투표한 인원수
    public int getSkipVoteCount() {
        return skipVotes.size();
    }
}
