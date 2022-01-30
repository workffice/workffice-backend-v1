package news.domain;

import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface NewsRepository {
    Try<Void> store(News news);

    Try<Void> update(News news);

    Option<News> findById(String id);

    List<News> findByOfficeBranch(String officeBranchId);
}
