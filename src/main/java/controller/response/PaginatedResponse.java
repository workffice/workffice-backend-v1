package controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
public class PaginatedResponse<T> extends DataResponse {
    private final List<T> data;
    private final Pagination pagination;

    public PaginatedResponse(
            List<T> data,
            Integer pageSize,
            boolean lastPage,
            Integer totalPages,
            Integer currentPage
    ) {
        this.data       = data;
        this.pagination = Pagination.of(pageSize, lastPage, totalPages, currentPage);
    }

    @Getter
    @AllArgsConstructor(staticName = "of")
    public static class Pagination {
        Integer pageSize;
        boolean lastPage;
        Integer totalPages;
        Integer currentPage;
    }
}
