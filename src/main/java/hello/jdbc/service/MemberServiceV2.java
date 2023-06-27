package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@Slf4j
@RequiredArgsConstructor // 요거 final 객체 생성자 만들어주는거!! 잊지말기
public class MemberServiceV2 {

    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Connection con = dataSource.getConnection();
        try {
            con.setAutoCommit(false); // 트랜잭션 시작. 수동 커밋이라 이말임.
            // 비즈니스 로직 수행.
            // 트랜잭션 시작
            bizLogic(con, fromId, toId, money); // 트랜잭션을 관리하는 로직과 비즈니스 로직을 구분하기 위해 분리.
            // 커밋 or 롤백
            con.commit(); // 성공시 커밋
        } catch(Exception e) {
            con.rollback(); // 실패시 롤백
            throw new IllegalStateException(e);
        } finally {
            release(con);
        }

    }

    private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(con, fromId);
        Member toMember = memberRepository.findById(con, toId);

        memberRepository.update(con, fromId, fromMember.getMoney() - money);
        validation(toMember); // 멤버 id가 ex인 경우 예외발생. 예외 발생시 아래 구문은 실행 안됨.
        memberRepository.update(con, toId, toMember.getMoney() + money);
    }

    private static void release(Connection con) {
        if(con != null) {
            try {
                con.setAutoCommit(true); // pool을 쓰고 반환될테니 오토커밋으로 바꿔줘야함.
                con.close(); // 종료가 아닌 connection pool 반납.
            } catch(Exception e) {
                log.info("error", e); // exception을 log에 넣을때는 {} 요거 안넣어도 됨.
            }
        }
    }

    private static void validation(Member toMember) {
        if(toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
