package news.application;

import news.application.dto.NewsResponse;
import news.domain.News;
import news.domain.NewsRepository;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NewsFinder {
    private final NewsRepository newsRepo;

    public NewsFinder(NewsRepository newsRepo) {
        this.newsRepo = newsRepo;
    }

    public List<NewsResponse> findByOfficeBranch(String officeBranchId) {
        return newsRepo.findByOfficeBranch(officeBranchId)
                .stream()
                .map(News::toResponse)
                .collect(Collectors.toList());
    }
}
