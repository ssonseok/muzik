package son.suck.muzik.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import son.suck.muzik.domain.Music;

import java.util.List;

public interface MusicRepository {
    // 방 조건(장르, 시작년도, 끝년도)에 맞는 음악들을 랜덤으로 지정된 개수만큼 추출하는 쿼리
    @Query(value = "SELECT * FROM music m " +
            "WHERE m.genre = :genre " +
            "AND m.release_year BETWEEN :startYear AND :endYear " +
            "ORDER BY RAND() LIMIT :musicCount", nativeQuery = true)
    List<Music> findRandomQuizSet(@Param("genre") String genre,
                                  @Param("startYear") int startYear,
                                  @Param("endYear") int endYear,
                                  @Param("musicCount") int musicCount);
}
