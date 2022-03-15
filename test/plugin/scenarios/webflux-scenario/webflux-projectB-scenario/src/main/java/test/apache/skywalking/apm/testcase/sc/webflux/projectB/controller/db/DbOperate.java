package test.apache.skywalking.apm.testcase.sc.webflux.projectB.controller.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
public interface DbOperate extends JpaRepository<User,Long> {

    @Query(value = "SELECT t.name from User t where t.id=1 ")
    public String selectOne();

}
